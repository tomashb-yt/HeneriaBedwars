package fr.heneria.bedwars.core.game.shop;

import fr.heneria.bedwars.api.game.GameState;
import fr.heneria.bedwars.core.game.GameInstanceManager;
import fr.heneria.bedwars.core.game.event.GameEventBus;
import fr.heneria.bedwars.core.game.event.ShopPurchaseEvent;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

/** Pure purchase policy; Bukkit performs only the final atomic inventory exchange. */
public final class ShopPurchaseService {
  private final GameInstanceManager games;
  private final GameEventBus events;
  private final Clock clock;

  public ShopPurchaseService(GameInstanceManager games, GameEventBus events, Clock clock) {
    this.games = Objects.requireNonNull(games, "games");
    this.events = Objects.requireNonNull(events, "events");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public ShopPurchaseResult purchase(UUID playerId, ShopOffer offer, ShopInventory inventory) {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(offer, "offer");
    Objects.requireNonNull(inventory, "inventory");
    var game = games.byPlayer(playerId).orElse(null);
    if (game == null)
      return result(ShopPurchaseCode.NOT_IN_GAME, offer, inventory.balance(offer.currency()));
    if (game.state() != GameState.PLAYING)
      return result(ShopPurchaseCode.NOT_PLAYING, offer, inventory.balance(offer.currency()));
    var player = game.player(playerId).orElse(null);
    if (player == null)
      return result(ShopPurchaseCode.NOT_IN_GAME, offer, inventory.balance(offer.currency()));
    if (player.spectator())
      return result(ShopPurchaseCode.SPECTATOR, offer, inventory.balance(offer.currency()));
    int balance = inventory.balance(offer.currency());
    if (balance < offer.price()) return result(ShopPurchaseCode.INSUFFICIENT_FUNDS, offer, balance);
    if (!inventory.canExchange(offer))
      return result(ShopPurchaseCode.INVENTORY_FULL, offer, balance);
    if (!inventory.exchange(offer))
      return result(
          ShopPurchaseCode.TRANSACTION_FAILED, offer, inventory.balance(offer.currency()));
    events.publish(
        new ShopPurchaseEvent(
            game.id(),
            playerId,
            offer.id().value(),
            offer.currency(),
            offer.price(),
            clock.instant()));
    return result(ShopPurchaseCode.SUCCESS, offer, inventory.balance(offer.currency()));
  }

  private static ShopPurchaseResult result(ShopPurchaseCode code, ShopOffer offer, int balance) {
    return new ShopPurchaseResult(code, offer, balance);
  }
}
