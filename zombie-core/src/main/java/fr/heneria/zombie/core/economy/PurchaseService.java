package fr.heneria.zombie.core.economy;

import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Generic atomic purchase orchestrator with idempotence and compensating refunds. */
public final class PurchaseService {
  private final TransactionService transactions;
  private final PriceResolver prices;
  private final EconomyEventDispatcher events;
  private final Clock clock;
  private final Map<UUID, Map<String, PurchaseResult>> completed = new ConcurrentHashMap<>();
  private final Map<UUID, Object> locks = new ConcurrentHashMap<>();

  public PurchaseService(
      TransactionService transactions,
      PriceResolver prices,
      EconomyEventDispatcher events,
      Clock clock) {
    this.transactions = java.util.Objects.requireNonNull(transactions, "transactions");
    this.prices = java.util.Objects.requireNonNull(prices, "prices");
    this.events = java.util.Objects.requireNonNull(events, "events");
    this.clock = java.util.Objects.requireNonNull(clock, "clock");
  }

  public PurchaseResult purchase(PurchaseRequest request) {
    if (request == null || request.operationId() == null || request.operationId().isBlank()) {
      return failure(PurchaseResult.Status.INVALID_CONTEXT, 0, "Invalid request");
    }
    Object lock = locks.computeIfAbsent(request.gameId(), ignored -> new Object());
    synchronized (lock) {
      PurchaseResult previous =
          completed
              .computeIfAbsent(request.gameId(), ignored -> new ConcurrentHashMap<>())
              .get(request.operationId());
      if (previous != null) {
        return new PurchaseResult(
            PurchaseResult.Status.DUPLICATE_OPERATION,
            previous.price(),
            previous.payment(),
            previous.refund(),
            "Purchase operation already processed");
      }
      events.publish(event(EconomyEvent.Type.PURCHASE_PRE_CHECK, request, request));
      if (request.fundingMode() != PurchaseFundingMode.INDIVIDUAL) {
        return remember(
            request,
            failure(
                PurchaseResult.Status.UNSUPPORTED_FUNDING_MODE, 0, "Only INDIVIDUAL is enabled"));
      }
      if (!request.condition().valid()) {
        return remember(
            request, failure(PurchaseResult.Status.INVALID_CONTEXT, 0, "Condition rejected"));
      }
      PriceResolver.PriceResult price = prices.resolve(request.price());
      if (!price.valid()) {
        return remember(
            request, failure(PurchaseResult.Status.INVALID_PRICE, 0, price.failureReason()));
      }
      TransactionResult payment =
          transactions.debit(
              new TransactionRequest(
                  request.gameId(),
                  request.playerId(),
                  price.price(),
                  request.transactionReason(),
                  "purchase:debit:" + request.operationId(),
                  purchaseMetadata(request)));
      if (!payment.successful()) {
        return remember(
            request,
            new PurchaseResult(
                PurchaseResult.Status.PAYMENT_FAILED,
                price.price(),
                payment.transaction(),
                Optional.empty(),
                payment.failureReason()));
      }
      boolean granted;
      try {
        granted = request.grant().apply();
      } catch (Exception failure) {
        granted = false;
      }
      if (granted) {
        PurchaseResult result =
            new PurchaseResult(
                PurchaseResult.Status.SUCCESS,
                price.price(),
                payment.transaction(),
                Optional.empty(),
                "");
        events.publish(event(EconomyEvent.Type.PURCHASE_COMPLETED, request, result));
        return remember(request, result);
      }
      Transaction original = payment.transaction().orElseThrow();
      TransactionResult refund =
          transactions.refund(
              new RefundRequest(
                  request.gameId(),
                  original.transactionId(),
                  original.amount(),
                  "purchase:refund:" + request.operationId(),
                  Map.of("purchaseFailure", "grant_failed")));
      PurchaseResult result =
          new PurchaseResult(
              refund.successful()
                  ? PurchaseResult.Status.GRANT_FAILED_REFUNDED
                  : PurchaseResult.Status.GRANT_FAILED_REFUND_FAILED,
              price.price(),
              payment.transaction(),
              refund.transaction(),
              refund.successful() ? "Grant failed; payment refunded" : refund.failureReason());
      events.publish(
          event(
              refund.successful()
                  ? EconomyEvent.Type.PURCHASE_REFUNDED
                  : EconomyEvent.Type.PURCHASE_FAILED,
              request,
              result));
      return remember(request, result);
    }
  }

  public void removeGame(UUID gameId) {
    completed.remove(gameId);
    locks.remove(gameId);
  }

  private PurchaseResult remember(PurchaseRequest request, PurchaseResult result) {
    completed.get(request.gameId()).put(request.operationId(), result);
    if (!result.successful()) {
      events.publish(event(EconomyEvent.Type.PURCHASE_FAILED, request, result));
    }
    return result;
  }

  private EconomyEvent event(EconomyEvent.Type type, PurchaseRequest request, Object data) {
    return new EconomyEvent(type, request.gameId(), request.playerId(), clock.instant(), data);
  }

  private static Map<String, String> purchaseMetadata(PurchaseRequest request) {
    java.util.LinkedHashMap<String, String> data =
        new java.util.LinkedHashMap<>(request.metadata());
    data.put("purchaseType", request.type().name());
    data.put("itemId", request.itemId() == null ? "" : request.itemId());
    return Map.copyOf(data);
  }

  private static PurchaseResult failure(PurchaseResult.Status status, long price, String reason) {
    return new PurchaseResult(status, price, Optional.empty(), Optional.empty(), reason);
  }
}
