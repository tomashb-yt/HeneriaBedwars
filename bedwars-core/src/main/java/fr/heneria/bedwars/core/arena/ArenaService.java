package fr.heneria.bedwars.core.arena;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Transactional administrative use cases. Memory changes only after persistence succeeds. */
public final class ArenaService {
  private final ArenaRepository repository;
  private final ArenaRegistry registry;
  private final ArenaValidator validator;
  private final Clock clock;

  public ArenaService(
      ArenaRepository repository, ArenaRegistry registry, ArenaValidator validator, Clock clock) {
    this.repository = Objects.requireNonNull(repository, "repository");
    this.registry = Objects.requireNonNull(registry, "registry");
    this.validator = Objects.requireNonNull(validator, "validator");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public synchronized ArenaReloadResult reload() throws IOException {
    ArenaLoadResult loaded = repository.loadAll();
    Map<ArenaId, ArenaDefinition> next = new LinkedHashMap<>();
    loaded.definitions().forEach(arena -> next.put(arena.id(), normalizedLoaded(arena)));
    int preserved = 0;
    for (ArenaId failed : loaded.failedKnownIds().keySet()) {
      ArenaDefinition old = registry.snapshot().get(failed);
      if (old != null) {
        next.put(failed, old);
        preserved++;
      }
    }
    registry.replace(next.values());
    List<String> failures = new ArrayList<>(loaded.unreadableFiles());
    loaded.failedKnownIds().forEach((id, detail) -> failures.add(id + ": " + detail));
    return new ArenaReloadResult(loaded.definitions().size(), preserved, failures);
  }

  public synchronized ArenaOperationResult create(String rawId) {
    final ArenaId id;
    try {
      id = ArenaId.parse(rawId);
    } catch (RuntimeException exception) {
      return ArenaOperationResult.failure(ArenaOperationCode.INVALID_ID, exception.getMessage());
    }
    if (registry.find(id).isPresent())
      return ArenaOperationResult.failure(ArenaOperationCode.ALREADY_EXISTS, id.value());
    return persist(ArenaDefinition.draft(id, now()));
  }

  public List<ArenaDefinition> list() {
    return registry.all().stream().sorted((a, b) -> a.id().compareTo(b.id())).toList();
  }

  public Optional<ArenaDefinition> find(String rawId) {
    try {
      return registry.find(ArenaId.parse(rawId));
    } catch (RuntimeException ignored) {
      return Optional.empty();
    }
  }

  public ArenaValidationResult validate(ArenaDefinition arena) {
    return validator.validate(arena);
  }

  public synchronized ArenaOperationResult setWorld(String rawId, String world) {
    return setWorld(rawId, world, null);
  }

  /** Associates an already validated map-template id and its managed working world. */
  public synchronized ArenaOperationResult setMapTemplate(
      String rawId, String mapTemplateId, String managedWorldName) {
    return setMapTemplate(rawId, mapTemplateId, managedWorldName, null);
  }

  public synchronized ArenaOperationResult setMapTemplate(
      String rawId, String mapTemplateId, String managedWorldName, long expectedRevision) {
    return setMapTemplate(rawId, mapTemplateId, managedWorldName, Long.valueOf(expectedRevision));
  }

  private ArenaOperationResult setMapTemplate(
      String rawId, String mapTemplateId, String managedWorldName, Long expectedRevision) {
    if (mapTemplateId == null
        || mapTemplateId.isBlank()
        || managedWorldName == null
        || managedWorldName.isBlank())
      return ArenaOperationResult.failure(ArenaOperationCode.INVALID_ARGUMENT, "map template");
    return edit(
        rawId,
        expectedRevision,
        arena ->
            arena.withTemplate(
                mapTemplateId.trim(), managedWorldName.trim(), editStatus(arena), now()));
  }

  public synchronized ArenaOperationResult clearMapTemplate(String rawId, long expectedRevision) {
    return edit(
        rawId,
        Long.valueOf(expectedRevision),
        arena -> arena.withoutTemplate(editStatus(arena), now()));
  }

  public synchronized ArenaOperationResult setWorld(
      String rawId, String world, long expectedRevision) {
    return setWorld(rawId, world, Long.valueOf(expectedRevision));
  }

  private ArenaOperationResult setWorld(String rawId, String world, Long expectedRevision) {
    return edit(
        rawId,
        expectedRevision,
        arena ->
            arena.edited(
                editStatus(arena),
                Optional.ofNullable(world),
                arena.minimumPlayers(),
                arena.maximumPlayers(),
                arena.teamCount(),
                arena.playersPerTeam(),
                arena.waitingLocation(),
                arena.spectatorLocation(),
                now()));
  }

  public synchronized ArenaOperationResult setWaiting(String rawId, ArenaLocation location) {
    return setWaiting(rawId, location, null);
  }

  public synchronized ArenaOperationResult setWaiting(
      String rawId, ArenaLocation location, long expectedRevision) {
    return setWaiting(rawId, location, Long.valueOf(expectedRevision));
  }

  private ArenaOperationResult setWaiting(
      String rawId, ArenaLocation location, Long expectedRevision) {
    return edit(
        rawId,
        expectedRevision,
        arena ->
            arena.edited(
                editStatus(arena),
                arena.worldName(),
                arena.minimumPlayers(),
                arena.maximumPlayers(),
                arena.teamCount(),
                arena.playersPerTeam(),
                Optional.of(location),
                arena.spectatorLocation(),
                now()));
  }

  public synchronized ArenaOperationResult setSpectator(String rawId, ArenaLocation location) {
    return setSpectator(rawId, location, null);
  }

  public synchronized ArenaOperationResult setSpectator(
      String rawId, ArenaLocation location, long expectedRevision) {
    return setSpectator(rawId, location, Long.valueOf(expectedRevision));
  }

  private ArenaOperationResult setSpectator(
      String rawId, ArenaLocation location, Long expectedRevision) {
    return edit(
        rawId,
        expectedRevision,
        arena ->
            arena.edited(
                editStatus(arena),
                arena.worldName(),
                arena.minimumPlayers(),
                arena.maximumPlayers(),
                arena.teamCount(),
                arena.playersPerTeam(),
                arena.waitingLocation(),
                Optional.of(location),
                now()));
  }

  public synchronized ArenaOperationResult setPlayers(String rawId, int minimum, int maximum) {
    return setPlayers(rawId, minimum, maximum, null);
  }

  public synchronized ArenaOperationResult setPlayers(
      String rawId, int minimum, int maximum, long expectedRevision) {
    return setPlayers(rawId, minimum, maximum, Long.valueOf(expectedRevision));
  }

  private ArenaOperationResult setPlayers(
      String rawId, int minimum, int maximum, Long expectedRevision) {
    if (minimum < 1 || maximum < minimum)
      return ArenaOperationResult.failure(
          ArenaOperationCode.INVALID_ARGUMENT, "Invalid player bounds");
    return edit(
        rawId,
        expectedRevision,
        arena ->
            arena.edited(
                editStatus(arena),
                arena.worldName(),
                minimum,
                maximum,
                arena.teamCount(),
                arena.playersPerTeam(),
                arena.waitingLocation(),
                arena.spectatorLocation(),
                now()));
  }

  public synchronized ArenaOperationResult setTeams(String rawId, int teams, int playersPerTeam) {
    return setTeams(rawId, teams, playersPerTeam, null);
  }

  public synchronized ArenaOperationResult setTeams(
      String rawId, int teams, int playersPerTeam, long expectedRevision) {
    return setTeams(rawId, teams, playersPerTeam, Long.valueOf(expectedRevision));
  }

  private ArenaOperationResult setTeams(
      String rawId, int teams, int playersPerTeam, Long expectedRevision) {
    if (teams < 2 || playersPerTeam < 1 || (long) teams * playersPerTeam > Integer.MAX_VALUE)
      return ArenaOperationResult.failure(
          ArenaOperationCode.INVALID_ARGUMENT, "Invalid team capacity");
    int maximum = teams * playersPerTeam;
    return edit(
        rawId,
        expectedRevision,
        arena ->
            arena.withTeams(
                ArenaDefinition.defaultTeams(teams, playersPerTeam), editStatus(arena), now()));
  }

  /**
   * Replaces detailed team definitions after the caller has built and validated a complete list.
   */
  public synchronized ArenaOperationResult setTeamDefinitions(
      String rawId, List<ArenaTeamDefinition> teams, long expectedRevision) {
    if (teams == null || teams.size() < 2)
      return ArenaOperationResult.failure(
          ArenaOperationCode.INVALID_ARGUMENT, "At least two teams");
    return edit(rawId, expectedRevision, arena -> arena.withTeams(teams, editStatus(arena), now()));
  }

  public synchronized ArenaOperationResult setDisplayName(
      String rawId, String displayName, long expectedRevision) {
    String value = displayName == null ? "" : displayName.trim();
    if (value.isEmpty() || value.length() > 64)
      return ArenaOperationResult.failure(
          ArenaOperationCode.INVALID_ARGUMENT, "Display name must contain 1 to 64 characters");
    return edit(
        rawId, expectedRevision, arena -> arena.withDisplayName(value, editStatus(arena), now()));
  }

  public synchronized ArenaOperationResult clearWaiting(String rawId, long expectedRevision) {
    return edit(
        rawId,
        expectedRevision,
        arena ->
            arena.edited(
                editStatus(arena),
                arena.worldName(),
                arena.minimumPlayers(),
                arena.maximumPlayers(),
                arena.teamCount(),
                arena.playersPerTeam(),
                Optional.empty(),
                arena.spectatorLocation(),
                now()));
  }

  public synchronized ArenaOperationResult clearSpectator(String rawId, long expectedRevision) {
    return edit(
        rawId,
        expectedRevision,
        arena ->
            arena.edited(
                editStatus(arena),
                arena.worldName(),
                arena.minimumPlayers(),
                arena.maximumPlayers(),
                arena.teamCount(),
                arena.playersPerTeam(),
                arena.waitingLocation(),
                Optional.empty(),
                now()));
  }

  public synchronized ArenaOperationResult setBoundaryMinimum(
      String rawId, ArenaVector minimum, long expectedRevision) {
    return edit(
        rawId,
        expectedRevision,
        arena -> {
          ArenaBoundary old = arena.boundary().orElseGet(ArenaBoundary::empty);
          return arena.withBoundary(
              Optional.of(new ArenaBoundary(old.enabled(), Optional.of(minimum), old.maximum())),
              editStatus(arena),
              now());
        });
  }

  public synchronized ArenaOperationResult setBoundaryMaximum(
      String rawId, ArenaVector maximum, long expectedRevision) {
    return edit(
        rawId,
        expectedRevision,
        arena -> {
          ArenaBoundary old = arena.boundary().orElseGet(ArenaBoundary::empty);
          return arena.withBoundary(
              Optional.of(new ArenaBoundary(old.enabled(), old.minimum(), Optional.of(maximum))),
              editStatus(arena),
              now());
        });
  }

  public synchronized ArenaOperationResult setBoundaryEnabled(
      String rawId, boolean enabled, long expectedRevision) {
    return edit(
        rawId,
        expectedRevision,
        arena -> {
          ArenaBoundary old = arena.boundary().orElseGet(ArenaBoundary::empty);
          return arena.withBoundary(
              Optional.of(new ArenaBoundary(enabled, old.minimum(), old.maximum())),
              editStatus(arena),
              now());
        });
  }

  public synchronized ArenaOperationResult clearBoundary(String rawId, long expectedRevision) {
    return edit(
        rawId,
        expectedRevision,
        arena -> arena.withBoundary(Optional.empty(), editStatus(arena), now()));
  }

  public synchronized ArenaOperationResult enable(String rawId) {
    return enable(rawId, null);
  }

  public synchronized ArenaOperationResult enable(String rawId, long expectedRevision) {
    return enable(rawId, Long.valueOf(expectedRevision));
  }

  private ArenaOperationResult enable(String rawId, Long expectedRevision) {
    ArenaDefinition arena = find(rawId).orElse(null);
    if (arena == null) return ArenaOperationResult.failure(ArenaOperationCode.NOT_FOUND, rawId);
    if (conflict(arena, expectedRevision)) return conflict(arena);
    ArenaValidationResult validation = validator.validate(arena);
    if (!validation.valid())
      return new ArenaOperationResult(
          ArenaOperationCode.VALIDATION_FAILED,
          Optional.of(arena),
          validation.problems(),
          "Arena is invalid");
    return persist(arena.withStatus(ArenaStatus.ENABLED, now()));
  }

  public synchronized ArenaOperationResult disable(String rawId) {
    return disable(rawId, null);
  }

  public synchronized ArenaOperationResult disable(String rawId, long expectedRevision) {
    return disable(rawId, Long.valueOf(expectedRevision));
  }

  private ArenaOperationResult disable(String rawId, Long expectedRevision) {
    ArenaDefinition arena = find(rawId).orElse(null);
    if (arena == null) return ArenaOperationResult.failure(ArenaOperationCode.NOT_FOUND, rawId);
    if (conflict(arena, expectedRevision)) return conflict(arena);
    return persist(arena.withStatus(ArenaStatus.DISABLED, now()));
  }

  public synchronized ArenaOperationResult delete(String rawId) {
    return delete(rawId, null);
  }

  public synchronized ArenaOperationResult delete(String rawId, long expectedRevision) {
    return delete(rawId, Long.valueOf(expectedRevision));
  }

  private ArenaOperationResult delete(String rawId, Long expectedRevision) {
    ArenaDefinition arena = find(rawId).orElse(null);
    if (arena == null) return ArenaOperationResult.failure(ArenaOperationCode.NOT_FOUND, rawId);
    if (conflict(arena, expectedRevision)) return conflict(arena);
    try {
      repository.deleteWithBackup(arena.id());
      Map<ArenaId, ArenaDefinition> next = new LinkedHashMap<>(registry.snapshot());
      next.remove(arena.id());
      registry.replace(next.values());
      return ArenaOperationResult.success(arena, null);
    } catch (IOException exception) {
      return ArenaOperationResult.failure(
          ArenaOperationCode.STORAGE_FAILED, exception.getMessage());
    }
  }

  private ArenaOperationResult edit(
      String rawId,
      Long expectedRevision,
      java.util.function.UnaryOperator<ArenaDefinition> editor) {
    ArenaDefinition arena = find(rawId).orElse(null);
    if (arena == null) return ArenaOperationResult.failure(ArenaOperationCode.NOT_FOUND, rawId);
    if (conflict(arena, expectedRevision)) return conflict(arena);
    return persist(editor.apply(arena));
  }

  private ArenaOperationResult persist(ArenaDefinition arena) {
    ArenaValidationResult validation = validator.validate(arena);
    ArenaDefinition normalized = arena;
    if (arena.status() != ArenaStatus.DRAFT
        && arena.status() != ArenaStatus.DISABLED
        && arena.status() != ArenaStatus.ENABLED)
      normalized =
          arena.normalizedStatus(validation.valid() ? ArenaStatus.READY : ArenaStatus.INVALID);
    try {
      repository.save(normalized);
      Map<ArenaId, ArenaDefinition> next = new LinkedHashMap<>(registry.snapshot());
      next.put(normalized.id(), normalized);
      registry.replace(next.values());
      return ArenaOperationResult.success(normalized, validation);
    } catch (IOException exception) {
      return ArenaOperationResult.failure(
          ArenaOperationCode.STORAGE_FAILED, exception.getMessage());
    }
  }

  private ArenaDefinition normalizedLoaded(ArenaDefinition arena) {
    ArenaValidationResult result = validator.validate(arena);
    if (arena.status() == ArenaStatus.ENABLED && !result.valid())
      return arena.normalizedStatus(ArenaStatus.INVALID);
    if (arena.status() == ArenaStatus.READY && !result.valid())
      return arena.normalizedStatus(ArenaStatus.INVALID);
    if (arena.status() == ArenaStatus.INVALID && result.valid())
      return arena.normalizedStatus(ArenaStatus.READY);
    return arena;
  }

  private static ArenaStatus editStatus(ArenaDefinition arena) {
    return ArenaStatus.INVALID;
  }

  private static boolean conflict(ArenaDefinition arena, Long expectedRevision) {
    return expectedRevision != null && arena.revision() != expectedRevision;
  }

  private static ArenaOperationResult conflict(ArenaDefinition arena) {
    return new ArenaOperationResult(
        ArenaOperationCode.CONFLICT,
        Optional.of(arena),
        List.of(),
        "Expected revision does not match active arena");
  }

  private Instant now() {
    return clock.instant();
  }
}
