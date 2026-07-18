package fr.heneria.bedwars.core.game.shop;

/** Stable outcome of one attempted purchase. */
public enum ShopPurchaseCode {
  SUCCESS,
  NOT_IN_GAME,
  NOT_PLAYING,
  SPECTATOR,
  INSUFFICIENT_FUNDS,
  INVENTORY_FULL,
  TRANSACTION_FAILED
}
