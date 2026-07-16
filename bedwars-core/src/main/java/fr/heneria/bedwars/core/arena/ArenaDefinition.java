package fr.heneria.bedwars.core.arena;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Immutable persistent administrative definition of an arena. */
public record ArenaDefinition(
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
  public static final int CURRENT_CONFIG_VERSION = 1;

  public ArenaDefinition {
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
  }

  public static ArenaDefinition draft(ArenaId id, Instant now) {
    return new ArenaDefinition(
        CURRENT_CONFIG_VERSION,
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
        ArenaMetadata.created(now));
  }

  public boolean enabled() {
    return status == ArenaStatus.ENABLED;
  }

  public ArenaDefinition edited(
      ArenaStatus newStatus,
      Optional<String> world,
      int minimum,
      int maximum,
      int teams,
      int perTeam,
      Optional<ArenaLocation> waiting,
      Optional<ArenaLocation> spectator,
      Instant now) {
    return new ArenaDefinition(
        configVersion,
        id,
        displayName,
        newStatus,
        world,
        template,
        environment,
        minimum,
        maximum,
        teams,
        perTeam,
        waiting,
        spectator,
        boundary,
        metadata.touched(now));
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

  private static Optional<String> clean(Optional<String> value) {
    Objects.requireNonNull(value, "optional");
    return value.map(String::trim).filter(text -> !text.isEmpty());
  }
}
