package fr.heneria.zombie.core.session;

import java.time.Duration;
import java.util.Objects;

/**
 * Reconnection settings.
 *
 * @param enabled whether instance reconnection is retained
 * @param gracePeriod maximum offline duration
 * @param reservePlayerSlot whether membership remains reserved
 * @param returnToLobbyAfterExpiration whether expired sessions move to lobby
 */
public record ReconnectPolicy(
    boolean enabled,
    Duration gracePeriod,
    boolean reservePlayerSlot,
    boolean returnToLobbyAfterExpiration) {

  /** Validates the policy. */
  public ReconnectPolicy {
    Objects.requireNonNull(gracePeriod, "gracePeriod");
    if (gracePeriod.isNegative() || gracePeriod.isZero()) {
      throw new IllegalArgumentException("gracePeriod must be positive");
    }
  }
}
