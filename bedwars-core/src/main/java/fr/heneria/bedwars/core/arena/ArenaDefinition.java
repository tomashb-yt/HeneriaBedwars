package fr.heneria.bedwars.core.arena;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable persistent administrative definition of an arena. */
public record ArenaDefinition(
    int configVersion,
    long revision,
    ArenaId id,
    String displayName,
    ArenaStatus status,
    Optional<String> worldName,
    Optional<String> template,
    String environment,
    int minimumPlayers,
    int maximumPlayers,
    int teamCount,
    int playersPerTeam,
    Optional<ArenaLocation> waitingLocation,
    Optional<ArenaLocation> spectatorLocation,
    Optional<ArenaBoundary> boundary,
    ArenaMetadata metadata,
    List<ArenaTeamDefinition> teams) {
  public static final int CURRENT_CONFIG_VERSION = 1;

  public ArenaDefinition {
    if (revision < 1) throw new IllegalArgumentException("Arena revision must be positive");
    id = Objects.requireNonNull(id, "id");
    displayName = Objects.requireNonNull(displayName, "displayName").trim();
    status = Objects.requireNonNull(status, "status");
    worldName = clean(worldName);
    template = clean(template);
    environment = Objects.requireNonNull(environment, "environment").trim();
    waitingLocation = Objects.requireNonNull(waitingLocation, "waitingLocation");
    spectatorLocation = Objects.requireNonNull(spectatorLocation, "spectatorLocation");
    boundary = Objects.requireNonNull(boundary, "boundary");
    metadata = Objects.requireNonNull(metadata, "metadata");
    teams =
        Objects.requireNonNull(teams, "teams").isEmpty()
            ? defaultTeams(teamCount, playersPerTeam)
            : List.copyOf(teams);
  }

  /** Compatibility constructor for version-1 definitions written before revisions existed. */
  public ArenaDefinition(
      int configVersion,
      long revision,
      ArenaId id,
      String displayName,
      ArenaStatus status,
      Optional<String> worldName,
      Optional<String> template,
      String environment,
      int minimumPlayers,
      int maximumPlayers,
      int teamCount,
      int playersPerTeam,
      Optional<ArenaLocation> waitingLocation,
      Optional<ArenaLocation> spectatorLocation,
      Optional<ArenaBoundary> boundary,
      ArenaMetadata metadata) {
    this(
        configVersion,
        revision,
        id,
        displayName,
        status,
        worldName,
        template,
        environment,
        minimumPlayers,
        maximumPlayers,
        teamCount,
        playersPerTeam,
        waitingLocation,
        spectatorLocation,
        boundary,
        metadata,
        defaultTeams(teamCount, playersPerTeam));
  }

  /** Compatibility constructor for version-1 definitions written before revisions existed. */
  public ArenaDefinition(
      int configVersion,
      ArenaId id,
      String displayName,
      ArenaStatus status,
      Optional<String> worldName,
      Optional<String> template,
      String environment,
      int minimumPlayers,
      int maximumPlayers,
      int teamCount,
      int playersPerTeam,
      Optional<ArenaLocation> waitingLocation,
      Optional<ArenaLocation> spectatorLocation,
      Optional<ArenaBoundary> boundary,
      ArenaMetadata metadata) {
    this(
        configVersion,
        1,
        id,
        displayName,
        status,
        worldName,
        template,
        environment,
        minimumPlayers,
        maximumPlayers,
        teamCount,
        playersPerTeam,
        waitingLocation,
        spectatorLocation,
        boundary,
        metadata,
        defaultTeams(teamCount, playersPerTeam));
  }

  public static ArenaDefinition draft(ArenaId id, Instant now) {
    return new ArenaDefinition(
        CURRENT_CONFIG_VERSION,
        1,
        id,
        id.value(),
        ArenaStatus.DRAFT,
        Optional.empty(),
        Optional.empty(),
        "NORMAL",
        2,
        16,
        4,
        4,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        ArenaMetadata.created(now),
        defaultTeams(4, 4));
  }

  public boolean enabled() {
    return status == ArenaStatus.ENABLED;
  }

  public ArenaDefinition edited(
      ArenaStatus newStatus,
      Optional<String> world,
      int minimum,
      int maximum,
      int newTeamCount,
      int perTeam,
      Optional<ArenaLocation> waiting,
      Optional<ArenaLocation> spectator,
      Instant now) {
    return new ArenaDefinition(
        configVersion,
        revision + 1,
        id,
        displayName,
        newStatus,
        world,
        template,
        environment,
        minimum,
        maximum,
        newTeamCount,
        perTeam,
        waiting,
        spectator,
        boundary,
        metadata.touched(now),
        this.teams);
  }

  public ArenaDefinition withDisplayName(String value, ArenaStatus newStatus, Instant now) {
    return new ArenaDefinition(
        configVersion,
        revision + 1,
        id,
        value,
        newStatus,
        worldName,
        template,
        environment,
        minimumPlayers,
        maximumPlayers,
        teamCount,
        playersPerTeam,
        waitingLocation,
        spectatorLocation,
        boundary,
        metadata.touched(now),
        teams);
  }

  public ArenaDefinition withBoundary(
      Optional<ArenaBoundary> value, ArenaStatus newStatus, Instant now) {
    return new ArenaDefinition(
        configVersion,
        revision + 1,
        id,
        displayName,
        newStatus,
        worldName,
        template,
        environment,
        minimumPlayers,
        maximumPlayers,
        teamCount,
        playersPerTeam,
        waitingLocation,
        spectatorLocation,
        value,
        metadata.touched(now),
        teams);
  }

  /** Associates a stable map-template id and its managed Bukkit working-world name. */
  public ArenaDefinition withTemplate(
      String mapTemplateId, String managedWorldName, ArenaStatus newStatus, Instant now) {
    return new ArenaDefinition(
        configVersion,
        revision + 1,
        id,
        displayName,
        newStatus,
        Optional.ofNullable(managedWorldName),
        Optional.ofNullable(mapTemplateId),
        environment,
        minimumPlayers,
        maximumPlayers,
        teamCount,
        playersPerTeam,
        waitingLocation,
        spectatorLocation,
        boundary,
        metadata.touched(now),
        teams);
  }

  /** Removes the map relation and its derived managed world. */
  public ArenaDefinition withoutTemplate(ArenaStatus newStatus, Instant now) {
    return new ArenaDefinition(
        configVersion,
        revision + 1,
        id,
        displayName,
        newStatus,
        Optional.empty(),
        Optional.empty(),
        environment,
        minimumPlayers,
        maximumPlayers,
        teamCount,
        playersPerTeam,
        waitingLocation,
        spectatorLocation,
        boundary,
        metadata.touched(now),
        teams);
  }

  /** Changes only a derived status while loading; it does not create an administrative revision. */
  public ArenaDefinition normalizedStatus(ArenaStatus value) {
    return new ArenaDefinition(
        configVersion,
        revision,
        id,
        displayName,
        value,
        worldName,
        template,
        environment,
        minimumPlayers,
        maximumPlayers,
        teamCount,
        playersPerTeam,
        waitingLocation,
        spectatorLocation,
        boundary,
        metadata,
        teams);
  }

  public ArenaDefinition withStatus(ArenaStatus newStatus, Instant now) {
    return edited(
        newStatus,
        worldName,
        minimumPlayers,
        maximumPlayers,
        teamCount,
        playersPerTeam,
        waitingLocation,
        spectatorLocation,
        now);
  }

  public ArenaDefinition withTeams(
      List<ArenaTeamDefinition> value, ArenaStatus newStatus, Instant now) {
    List<ArenaTeamDefinition> next = List.copyOf(Objects.requireNonNull(value, "value"));
    int capacity = next.stream().mapToInt(ArenaTeamDefinition::capacity).sum();
    return new ArenaDefinition(
        configVersion,
        revision + 1,
        id,
        displayName,
        newStatus,
        worldName,
        template,
        environment,
        Math.min(minimumPlayers, capacity),
        capacity,
        next.size(),
        next.isEmpty() ? 0 : next.getFirst().capacity(),
        waitingLocation,
        spectatorLocation,
        boundary,
        metadata.touched(now),
        next);
  }

  /** Deterministic migration defaults for legacy count/capacity-only arena definitions. */
  public static List<ArenaTeamDefinition> defaultTeams(int count, int capacity) {
    TeamColor[] colors = TeamColor.values();
    if (count < 1 || capacity < 1) return List.of();
    java.util.ArrayList<ArenaTeamDefinition> generated = new java.util.ArrayList<>();
    for (int index = 0; index < count; index++) {
      TeamColor color = colors[index % colors.length];
      String name = color.name().toLowerCase(java.util.Locale.ROOT);
      generated.add(
          new ArenaTeamDefinition(
              new TeamId(index < colors.length ? name : name + "-" + (index / colors.length + 1)),
              color.name(),
              color,
              index + 1,
              capacity,
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              java.util.Map.of()));
    }
    return List.copyOf(generated);
  }

  private static Optional<String> clean(Optional<String> value) {
    Objects.requireNonNull(value, "optional");
    return value.map(String::trim).filter(text -> !text.isEmpty());
  }
}
