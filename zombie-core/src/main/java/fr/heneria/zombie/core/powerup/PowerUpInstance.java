package fr.heneria.zombie.core.powerup;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Immutable snapshot of one active timed bonus. */
public record PowerUpInstance(
    UUID id,
    UUID gameId,
    PowerUpDefinition definition,
    Optional<UUID> collectorId,
    Instant activatedAt,
    Instant expiresAt,
    String source) {
  public PowerUpInstance {
    collectorId = collectorId == null ? Optional.empty() : collectorId;
    source = source == null ? "" : source;
  }
}
