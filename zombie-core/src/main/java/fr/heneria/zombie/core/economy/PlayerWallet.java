package fr.heneria.zombie.core.economy;

import java.util.Optional;
import java.util.UUID;

/** Read-only public view of a session wallet; only {@link TransactionService} mutates it. */
public final class PlayerWallet {
  private final UUID gameId;
  private final UUID playerId;
  private long balance;
  private long totalEarned;
  private long totalSpent;
  private long totalRefunded;
  private long transactionCount;
  private Transaction latest;

  PlayerWallet(UUID gameId, UUID playerId) {
    this.gameId = gameId;
    this.playerId = playerId;
  }

  void apply(Transaction transaction) {
    balance = transaction.balanceAfter();
    switch (transaction.type()) {
      case CREDIT -> totalEarned = Math.addExact(totalEarned, transaction.amount());
      case DEBIT -> totalSpent = Math.addExact(totalSpent, transaction.amount());
      case REFUND -> totalRefunded = Math.addExact(totalRefunded, transaction.amount());
      case ADJUSTMENT, TRANSFER -> {
        // Adjustments and future transfers remain visible without inflating reward totals.
      }
    }
    transactionCount = Math.addExact(transactionCount, 1);
    latest = transaction;
  }

  public synchronized Snapshot snapshot() {
    return new Snapshot(
        gameId,
        playerId,
        balance,
        totalEarned,
        totalSpent,
        totalRefunded,
        transactionCount,
        Optional.ofNullable(latest));
  }

  public record Snapshot(
      UUID gameId,
      UUID playerId,
      long balance,
      long totalEarned,
      long totalSpent,
      long totalRefunded,
      long transactionCount,
      Optional<Transaction> latestTransaction) {}
}
