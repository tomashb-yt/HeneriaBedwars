package fr.heneria.zombie.core.economy;

import java.util.Optional;

/** Explicit transaction result; callers never infer failure from a boolean. */
public record TransactionResult(
    TransactionStatus status, Optional<Transaction> transaction, String failureReason) {
  public TransactionResult {
    java.util.Objects.requireNonNull(status, "status");
    transaction = transaction == null ? Optional.empty() : transaction;
    failureReason = failureReason == null ? "" : failureReason;
  }

  public boolean successful() {
    return status == TransactionStatus.SUCCESS;
  }

  static TransactionResult failure(TransactionStatus status, String reason) {
    return new TransactionResult(status, Optional.empty(), reason);
  }
}
