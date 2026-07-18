package fr.heneria.bedwars.core.game.upgrade;

import fr.heneria.bedwars.core.game.shop.ShopCurrency;

/** Atomic currency-only transaction port for team upgrades. */
public interface TeamUpgradeWallet {
  int balance(ShopCurrency currency);

  boolean pay(ShopCurrency currency, int amount);
}
