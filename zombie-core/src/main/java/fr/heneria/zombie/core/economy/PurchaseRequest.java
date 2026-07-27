package fr.heneria.zombie.core.economy;

import java.util.Map;
import java.util.UUID;

/** Complete context for a single atomic individual purchase. */
public record PurchaseRequest(
    UUID gameId,
    UUID playerId,
    PurchaseType type,
    String itemId,
    PriceResolver.PriceContext price,
    String operationId,
    TransactionReason transactionReason,
    PurchaseFundingMode fundingMode,
    Condition condition,
    Grant grant,
    Map<String, String> metadata) {
  public PurchaseRequest {
    java.util.Objects.requireNonNull(gameId, "gameId");
    java.util.Objects.requireNonNull(playerId, "playerId");
    java.util.Objects.requireNonNull(type, "type");
    java.util.Objects.requireNonNull(price, "price");
    java.util.Objects.requireNonNull(transactionReason, "transactionReason");
    java.util.Objects.requireNonNull(fundingMode, "fundingMode");
    condition = condition == null ? () -> true : condition;
    java.util.Objects.requireNonNull(grant, "grant");
    metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
  }

  @FunctionalInterface
  public interface Condition {
    boolean valid();
  }

  @FunctionalInterface
  public interface Grant {
    boolean apply() throws Exception;
  }
}
