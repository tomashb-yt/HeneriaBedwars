package fr.heneria.zombie.core.economy;

import java.util.Map;
import java.util.UUID;

/** Request for a traceable full or partial refund of a previous debit. */
public record RefundRequest(
    UUID gameId,
    UUID originalTransactionId,
    long amount,
    String operationId,
    Map<String, String> metadata) {
  public RefundRequest {
    metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
  }
}
