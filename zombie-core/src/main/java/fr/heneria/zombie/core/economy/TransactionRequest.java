package fr.heneria.zombie.core.economy;

import java.util.Map;
import java.util.UUID;

/** Validated command passed to the transaction boundary. */
public record TransactionRequest(
    UUID gameId,
    UUID playerId,
    long amount,
    TransactionReason reason,
    String operationId,
    Map<String, String> metadata) {
  public TransactionRequest {
    java.util.Objects.requireNonNull(gameId, "gameId");
    java.util.Objects.requireNonNull(playerId, "playerId");
    java.util.Objects.requireNonNull(reason, "reason");
    metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
  }

  public static TransactionRequest of(
      UUID gameId, UUID playerId, long amount, TransactionReason reason, String operationId) {
    return new TransactionRequest(gameId, playerId, amount, reason, operationId, Map.of());
  }
}
