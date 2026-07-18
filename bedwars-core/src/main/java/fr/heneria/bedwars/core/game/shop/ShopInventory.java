package fr.heneria.bedwars.core.game.shop;

/** Platform inventory transaction port. Calls are expected on the platform game thread. */
public interface ShopInventory {
  int balance(ShopCurrency currency);

  boolean canExchange(ShopOffer offer);

  boolean exchange(ShopOffer offer);
}
