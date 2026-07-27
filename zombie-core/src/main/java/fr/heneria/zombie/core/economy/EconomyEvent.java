package fr.heneria.zombie.core.economy;

import java.time.Instant;
import java.util.UUID;

/** Synchronous internal event emitted before and after an economy mutation. */
public record EconomyEvent(Type type, UUID gameId, UUID playerId, Instant occurredAt, Object data) {
  public enum Type {
    TRANSACTION_PRE,
    TRANSACTION_COMPLETED,
    TRANSACTION_REJECTED,
    PURCHASE_PRE_CHECK,
    PURCHASE_COMPLETED,
    PURCHASE_FAILED,
    PURCHASE_REFUNDED
  }
}
