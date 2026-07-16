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
    return edit(
        rawId,
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
    return edit(
        rawId,
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
    return edit(
        rawId,
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
    if (minimum < 1 || maximum < minimum)
      return ArenaOperationResult.failure(
          ArenaOperationCode.INVALID_ARGUMENT, "Invalid player bounds");
    return edit(
        rawId,
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
    if (teams < 2 || playersPerTeam < 1 || (long) teams * playersPerTeam > Integer.MAX_VALUE)
      return ArenaOperationResult.failure(
          ArenaOperationCode.INVALID_ARGUMENT, "Invalid team capacity");
    int maximum = teams * playersPerTeam;
    return edit(
        rawId,
        arena ->
            arena.edited(
                editStatus(arena),
                arena.worldName(),
                Math.min(arena.minimumPlayers(), maximum),
                maximum,
                teams,
                playersPerTeam,
                arena.waitingLocation(),
                arena.spectatorLocation(),
                now()));
  }

  public synchronized ArenaOperationResult enable(String rawId) {
    ArenaDefinition arena = find(rawId).orElse(null);
    if (arena == null) return ArenaOperationResult.failure(ArenaOperationCode.NOT_FOUND, rawId);
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
    ArenaDefinition arena = find(rawId).orElse(null);
    if (arena == null) return ArenaOperationResult.failure(ArenaOperationCode.NOT_FOUND, rawId);
    return persist(arena.withStatus(ArenaStatus.DISABLED, now()));
  }

  public synchronized ArenaOperationResult delete(String rawId) {
    ArenaDefinition arena = find(rawId).orElse(null);
    if (arena == null) return ArenaOperationResult.failure(ArenaOperationCode.NOT_FOUND, rawId);
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
      String rawId, java.util.function.UnaryOperator<ArenaDefinition> editor) {
    ArenaDefinition arena = find(rawId).orElse(null);
    return arena == null
        ? ArenaOperationResult.failure(ArenaOperationCode.NOT_FOUND, rawId)
        : persist(editor.apply(arena));
  }

  private ArenaOperationResult persist(ArenaDefinition arena) {
    ArenaValidationResult validation = validator.validate(arena);
    ArenaDefinition normalized = arena;
    if (arena.status() != ArenaStatus.DRAFT
        && arena.status() != ArenaStatus.DISABLED
        && arena.status() != ArenaStatus.ENABLED)
      normalized =
          arena.withStatus(validation.valid() ? ArenaStatus.READY : ArenaStatus.INVALID, now());
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
      return arena.withStatus(ArenaStatus.INVALID, arena.metadata().updatedAt());
    if (arena.status() == ArenaStatus.READY && !result.valid())
      return arena.withStatus(ArenaStatus.INVALID, arena.metadata().updatedAt());
    if (arena.status() == ArenaStatus.INVALID && result.valid())
      return arena.withStatus(ArenaStatus.READY, arena.metadata().updatedAt());
    return arena;
  }

  private static ArenaStatus editStatus(ArenaDefinition arena) {
    return ArenaStatus.INVALID;
  }

  private Instant now() {
    return clock.instant();
  }
}
