package fr.heneria.zombie.core.economy;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Immutable audit entry for one applied wallet mutation. */
public record Transaction(
    UUID transactionId,
    UUID gameId,
    UUID playerId,
    TransactionType type,
    TransactionReason reason,
    long amount,
    long balanceBefore,
    long balanceAfter,
    Instant createdAt,
    String operationId,
    Map<String, String> metadata) {
  public Transaction {
    java.util.Objects.requireNonNull(transactionId, "transactionId");
    java.util.Objects.requireNonNull(gameId, "gameId");
    java.util.Objects.requireNonNull(playerId, "playerId");
    java.util.Objects.requireNonNull(type, "type");
    java.util.Objects.requireNonNull(reason, "reason");
    java.util.Objects.requireNonNull(createdAt, "createdAt");
    if (operationId == null || operationId.isBlank() || amount < 0) {
      throw new IllegalArgumentException("Invalid transaction");
    }
    metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
  }
}
