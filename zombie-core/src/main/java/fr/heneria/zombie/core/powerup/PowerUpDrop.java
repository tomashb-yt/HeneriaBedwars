package fr.heneria.zombie.core.powerup;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Immutable snapshot of a collectible drop and its terminal state. */
public record PowerUpDrop(
    UUID id,
    UUID gameId,
    int round,
    PowerUpType type,
    Instant createdAt,
    Instant expiresAt,
    State state,
    Optional<UUID> collectorId) {
  public enum State {
    AVAILABLE,
    COLLECTED,
    EXPIRED
  }
}
