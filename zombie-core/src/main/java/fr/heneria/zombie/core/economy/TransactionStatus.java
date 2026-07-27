package fr.heneria.zombie.core.economy;

/** Explicit outcome of an attempted transaction. */
public enum TransactionStatus {
  SUCCESS,
  INVALID_AMOUNT,
  PLAYER_NOT_FOUND,
  GAME_NOT_FOUND,
  INSUFFICIENT_FUNDS,
  BALANCE_LIMIT_REACHED,
  DUPLICATE_TRANSACTION,
  CANCELLED,
  GAME_NOT_ACTIVE,
  INTERNAL_ERROR
}
