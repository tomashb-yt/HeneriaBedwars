package fr.heneria.zombie.core.economy;

import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/** Sole mutation boundary for every in-game point balance. */
public final class TransactionService {
  private final EconomyService economies;
  private final Clock clock;
  private final EconomyEventDispatcher events;
  private final Consumer<String> diagnostics;

  public TransactionService(
      EconomyService economies,
      Clock clock,
      EconomyEventDispatcher events,
      Consumer<String> diagnostics) {
    this.economies = java.util.Objects.requireNonNull(economies, "economies");
    this.clock = java.util.Objects.requireNonNull(clock, "clock");
    this.events = java.util.Objects.requireNonNull(events, "events");
    this.diagnostics = java.util.Objects.requireNonNull(diagnostics, "diagnostics");
  }

  public TransactionResult openWallet(
      UUID gameId, UUID playerId, long startingPoints, String operationId) {
    GameEconomy game = economies.find(gameId).orElse(null);
    if (game == null) {
      return TransactionResult.failure(TransactionStatus.GAME_NOT_FOUND, "Unknown game");
    }
    game.createWallet(playerId);
    if (startingPoints == 0) {
      return credit(
          TransactionRequest.of(
              gameId, playerId, 0, TransactionReason.STARTING_POINTS, operationId));
    }
    return credit(
        TransactionRequest.of(
            gameId, playerId, startingPoints, TransactionReason.STARTING_POINTS, operationId));
  }

  public TransactionResult credit(TransactionRequest request) {
    return mutate(request, TransactionType.CREDIT, true);
  }

  public TransactionResult debit(TransactionRequest request) {
    return mutate(request, TransactionType.DEBIT, false);
  }

  public TransactionResult adjust(
      UUID gameId, UUID playerId, long targetBalance, String operationId) {
    Optional<PlayerWallet.Snapshot> current = economies.wallet(gameId, playerId);
    if (current.isEmpty()) {
      return TransactionResult.failure(TransactionStatus.PLAYER_NOT_FOUND, "Unknown wallet");
    }
    long delta;
    try {
      delta = Math.subtractExact(targetBalance, current.get().balance());
    } catch (ArithmeticException overflow) {
      return TransactionResult.failure(TransactionStatus.INVALID_AMOUNT, "Amount overflow");
    }
    if (delta == 0) {
      return mutate(
          TransactionRequest.of(gameId, playerId, 0, TransactionReason.ADMIN_SET, operationId),
          TransactionType.ADJUSTMENT,
          true);
    }
    TransactionRequest request =
        TransactionRequest.of(
            gameId, playerId, Math.abs(delta), TransactionReason.ADMIN_SET, operationId);
    return mutate(request, TransactionType.ADJUSTMENT, delta > 0);
  }

  public TransactionResult refund(RefundRequest request) {
    if (request == null
        || request.gameId() == null
        || request.originalTransactionId() == null
        || request.operationId() == null
        || request.operationId().isBlank()
        || request.amount() <= 0) {
      return TransactionResult.failure(TransactionStatus.INVALID_AMOUNT, "Invalid refund");
    }
    GameEconomy game = economies.find(request.gameId()).orElse(null);
    if (game == null) {
      return TransactionResult.failure(TransactionStatus.GAME_NOT_FOUND, "Unknown game");
    }
    synchronized (game) {
      Optional<TransactionResult> duplicate = game.operation(request.operationId());
      if (duplicate.isPresent()) {
        return duplicateResult(duplicate.get());
      }
      Transaction original = game.transaction(request.originalTransactionId()).orElse(null);
      if (original == null || original.type() != TransactionType.DEBIT) {
        return rememberFailure(
            game,
            request.operationId(),
            TransactionStatus.INVALID_AMOUNT,
            "Original debit not found");
      }
      long remaining = original.amount() - game.refunded(original.transactionId());
      if (request.amount() > remaining) {
        return rememberFailure(
            game,
            request.operationId(),
            TransactionStatus.INVALID_AMOUNT,
            "Refund exceeds original debit");
      }
      TransactionResult result =
          apply(
              game,
              game.wallet(original.playerId()).orElseThrow(),
              TransactionType.REFUND,
              TransactionReason.REFUND,
              request.amount(),
              true,
              request.operationId(),
              merged(
                  request.metadata(),
                  Map.of("originalTransactionId", original.transactionId().toString())));
      if (result.successful()) {
        game.addRefund(original.transactionId(), request.amount());
      }
      return result;
    }
  }

  private TransactionResult mutate(
      TransactionRequest request, TransactionType type, boolean increase) {
    if (request == null
        || request.operationId() == null
        || request.operationId().isBlank()
        || request.amount() < 0) {
      return TransactionResult.failure(TransactionStatus.INVALID_AMOUNT, "Invalid request");
    }
    GameEconomy game = economies.find(request.gameId()).orElse(null);
    if (game == null) {
      return TransactionResult.failure(TransactionStatus.GAME_NOT_FOUND, "Unknown game");
    }
    synchronized (game) {
      Optional<TransactionResult> duplicate = game.operation(request.operationId());
      if (duplicate.isPresent()) {
        return duplicateResult(duplicate.get());
      }
      if (!game.active()) {
        return rememberFailure(
            game,
            request.operationId(),
            TransactionStatus.GAME_NOT_ACTIVE,
            "Game economy is closed");
      }
      PlayerWallet wallet = game.wallet(request.playerId()).orElse(null);
      if (wallet == null) {
        return rememberFailure(
            game, request.operationId(), TransactionStatus.PLAYER_NOT_FOUND, "Unknown wallet");
      }
      return apply(
          game,
          wallet,
          type,
          request.reason(),
          request.amount(),
          increase,
          request.operationId(),
          request.metadata());
    }
  }

  private TransactionResult apply(
      GameEconomy game,
      PlayerWallet wallet,
      TransactionType type,
      TransactionReason reason,
      long amount,
      boolean increase,
      String operationId,
      Map<String, String> metadata) {
    long before = wallet.snapshot().balance();
    long after;
    if (increase) {
      try {
        after = Math.addExact(before, amount);
      } catch (ArithmeticException overflow) {
        after = Long.MAX_VALUE;
      }
      if (after > game.policy().maximumBalance()) {
        if (game.policy().overflowPolicy() == OverflowPolicy.REJECT) {
          return rememberFailure(
              game, operationId, TransactionStatus.BALANCE_LIMIT_REACHED, "Balance limit reached");
        }
        if (game.policy().overflowPolicy() == OverflowPolicy.LOG_AND_CLAMP) {
          diagnostics.accept("Economy credit clamped for " + wallet.snapshot().playerId());
        }
        after = game.policy().maximumBalance();
        amount = after - before;
      }
    } else {
      if (!game.policy().allowNegativeBalance() && amount > before) {
        return rememberFailure(
            game, operationId, TransactionStatus.INSUFFICIENT_FUNDS, "Insufficient funds");
      }
      try {
        after = Math.subtractExact(before, amount);
      } catch (ArithmeticException overflow) {
        return rememberFailure(
            game, operationId, TransactionStatus.INVALID_AMOUNT, "Balance overflow");
      }
    }
    Transaction transaction =
        new Transaction(
            UUID.randomUUID(),
            wallet.snapshot().gameId(),
            wallet.snapshot().playerId(),
            type,
            reason,
            amount,
            before,
            after,
            clock.instant(),
            operationId,
            metadata);
    events.publish(
        new EconomyEvent(
            EconomyEvent.Type.TRANSACTION_PRE,
            transaction.gameId(),
            transaction.playerId(),
            clock.instant(),
            transaction));
    wallet.apply(transaction);
    game.append(transaction);
    TransactionResult result =
        new TransactionResult(TransactionStatus.SUCCESS, Optional.of(transaction), "");
    game.remember(operationId, result);
    events.publish(
        new EconomyEvent(
            EconomyEvent.Type.TRANSACTION_COMPLETED,
            transaction.gameId(),
            transaction.playerId(),
            clock.instant(),
            transaction));
    return result;
  }

  private TransactionResult rememberFailure(
      GameEconomy game, String operationId, TransactionStatus status, String reason) {
    TransactionResult result = TransactionResult.failure(status, reason);
    game.remember(operationId, result);
    events.publish(
        new EconomyEvent(
            EconomyEvent.Type.TRANSACTION_REJECTED,
            game.snapshot().gameId(),
            null,
            clock.instant(),
            result));
    return result;
  }

  private static TransactionResult duplicateResult(TransactionResult original) {
    return new TransactionResult(
        TransactionStatus.DUPLICATE_TRANSACTION,
        original.transaction(),
        "Operation already processed");
  }

  private static Map<String, String> merged(Map<String, String> left, Map<String, String> right) {
    java.util.LinkedHashMap<String, String> result = new java.util.LinkedHashMap<>(left);
    result.putAll(right);
    return Map.copyOf(result);
  }
}
