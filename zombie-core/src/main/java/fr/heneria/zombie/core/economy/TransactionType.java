package fr.heneria.zombie.core.economy;

/** Direction and accounting meaning of a wallet mutation. */
public enum TransactionType {
  CREDIT,
  DEBIT,
  REFUND,
  TRANSFER,
  ADJUSTMENT
}
