package fr.heneria.zombie.core.economy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Mutable economy aggregate isolated to one game and guarded by its intrinsic lock. */
public final class GameEconomy {
  private final UUID gameId;
  private final EconomyPolicy policy;
  private final Map<UUID, PlayerWallet> wallets = new LinkedHashMap<>();
  private final Map<String, TransactionResult> operations = new LinkedHashMap<>();
  private final Map<UUID, Transaction> transactions = new LinkedHashMap<>();
  private final Map<UUID, Long> refunded = new LinkedHashMap<>();
  private final Map<UUID, ArrayDeque<UUID>> playerHistory = new LinkedHashMap<>();
  private boolean active = true;
  private long rejectedTransactions;

  GameEconomy(UUID gameId, EconomyPolicy policy) {
    this.gameId = gameId;
    this.policy = policy;
  }

  synchronized PlayerWallet createWallet(UUID playerId) {
    return wallets.computeIfAbsent(playerId, ignored -> new PlayerWallet(gameId, playerId));
  }

  synchronized Optional<PlayerWallet> wallet(UUID playerId) {
    return Optional.ofNullable(wallets.get(playerId));
  }

  synchronized Optional<TransactionResult> operation(String operationId) {
    return Optional.ofNullable(operations.get(operationId));
  }

  synchronized void remember(String operationId, TransactionResult result) {
    operations.put(operationId, result);
    if (!result.successful()) {
      rejectedTransactions++;
    }
  }

  synchronized void append(Transaction transaction) {
    transactions.put(transaction.transactionId(), transaction);
    ArrayDeque<UUID> history =
        playerHistory.computeIfAbsent(transaction.playerId(), ignored -> new ArrayDeque<>());
    history.addLast(transaction.transactionId());
    while (history.size() > policy.maximumHistoryEntries()) {
      UUID removed = history.removeFirst();
      transactions.remove(removed);
    }
  }

  public synchronized List<Transaction> history(UUID playerId) {
    ArrayDeque<UUID> ids = playerHistory.get(playerId);
    if (ids == null) {
      return List.of();
    }
    ArrayList<Transaction> result = new ArrayList<>(ids.size());
    ids.forEach(id -> Optional.ofNullable(transactions.get(id)).ifPresent(result::add));
    return List.copyOf(result);
  }

  public synchronized Optional<Transaction> transaction(UUID transactionId) {
    return Optional.ofNullable(transactions.get(transactionId));
  }

  synchronized long refunded(UUID transactionId) {
    return refunded.getOrDefault(transactionId, 0L);
  }

  synchronized void addRefund(UUID transactionId, long amount) {
    refunded.merge(transactionId, amount, Math::addExact);
  }

  public synchronized Snapshot snapshot() {
    LinkedHashMap<UUID, PlayerWallet.Snapshot> values = new LinkedHashMap<>();
    wallets.forEach((id, wallet) -> values.put(id, wallet.snapshot()));
    return new Snapshot(
        gameId,
        active,
        Map.copyOf(values),
        transactions.size(),
        operations.size(),
        rejectedTransactions);
  }

  synchronized boolean active() {
    return active;
  }

  synchronized void close() {
    active = false;
  }

  EconomyPolicy policy() {
    return policy;
  }

  public record Snapshot(
      UUID gameId,
      boolean active,
      Map<UUID, PlayerWallet.Snapshot> wallets,
      int retainedTransactions,
      int knownOperations,
      long rejectedTransactions) {}
}
