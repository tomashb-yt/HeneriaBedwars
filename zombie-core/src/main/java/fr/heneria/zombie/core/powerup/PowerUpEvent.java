package fr.heneria.zombie.core.powerup;

import java.time.Instant;
import java.util.UUID;

/** Internal lifecycle event for drops and active effects. */
public record PowerUpEvent(Type type, UUID gameId, Instant occurredAt, Object data) {
  public enum Type {
    DROP_CREATED,
    DROP_COLLECTED,
    DROP_EXPIRED,
    ACTIVATED,
    EXPIRED
  }
}
