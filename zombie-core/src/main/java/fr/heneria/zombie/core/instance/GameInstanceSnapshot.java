package fr.heneria.zombie.core.instance;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Immutable diagnostic view of an instance. */
public record GameInstanceSnapshot(
    UUID id,
    String mapId,
    Optional<String> worldName,
    GameInstanceState state,
    Set<UUID> players,
    int maximumPlayers,
    Instant createdAt,
    Optional<UUID> owner,
    InstanceAccess access,
    Optional<String> lastError) {

  /** Defensively copies collections and validates fields. */
  public GameInstanceSnapshot {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(mapId, "mapId");
    worldName = Objects.requireNonNull(worldName, "worldName");
    Objects.requireNonNull(state, "state");
    players = Set.copyOf(players);
    Objects.requireNonNull(createdAt, "createdAt");
    owner = Objects.requireNonNull(owner, "owner");
    Objects.requireNonNull(access, "access");
    lastError = Objects.requireNonNull(lastError, "lastError");
  }
}
