package fr.heneria.zombie.core.session;

import java.util.Objects;
import java.util.UUID;

/**
 * Membership reservation released after grace expiration.
 *
 * @param playerId player identifier
 * @param instanceId former instance
 */
public record ExpiredReservation(UUID playerId, UUID instanceId) {

  /** Validates identifiers. */
  public ExpiredReservation {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(instanceId, "instanceId");
  }
}
