package fr.heneria.zombie.core.session;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Immutable view of one player session. */
public record PlayerSessionSnapshot(
    UUID playerId,
    PlayerContext context,
    Optional<UUID> instanceId,
    boolean online,
    Optional<Instant> reconnectDeadline) {

  /** Validates session invariants. */
  public PlayerSessionSnapshot {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(context, "context");
    instanceId = Objects.requireNonNull(instanceId, "instanceId");
    reconnectDeadline = Objects.requireNonNull(reconnectDeadline, "reconnectDeadline");
    if (context == PlayerContext.INSTANCE && instanceId.isEmpty()) {
      throw new IllegalArgumentException("Instance context requires an instance id");
    }
    if (context == PlayerContext.LOBBY && instanceId.isPresent()) {
      throw new IllegalArgumentException("Lobby context cannot reference an instance");
    }
  }
}
