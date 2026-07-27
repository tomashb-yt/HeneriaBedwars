package fr.heneria.zombie.core.economy;

import java.util.Optional;

/** Final outcome of validation, payment, grant and possible compensation. */
public record PurchaseResult(
    Status status,
    long price,
    Optional<Transaction> payment,
    Optional<Transaction> refund,
    String failureReason) {
  public PurchaseResult {
    payment = payment == null ? Optional.empty() : payment;
    refund = refund == null ? Optional.empty() : refund;
    failureReason = failureReason == null ? "" : failureReason;
  }

  public boolean successful() {
    return status == Status.SUCCESS;
  }

  public enum Status {
    SUCCESS,
    INVALID_CONTEXT,
    INVALID_PRICE,
    UNSUPPORTED_FUNDING_MODE,
    PAYMENT_FAILED,
    GRANT_FAILED_REFUNDED,
    GRANT_FAILED_REFUND_FAILED,
    DUPLICATE_OPERATION,
    INTERNAL_ERROR
  }
}
