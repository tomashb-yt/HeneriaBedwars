package fr.heneria.bedwars.core.game.shop;

/** Stable outcome of one attempted purchase. */
public enum ShopPurchaseCode {
  SUCCESS,
  NOT_IN_GAME,
  NOT_PLAYING,
  SPECTATOR,
  ALREADY_OWNED,
  WRONG_TIER,
  INSUFFICIENT_FUNDS,
  INVENTORY_FULL,
  TRANSACTION_FAILED
}
