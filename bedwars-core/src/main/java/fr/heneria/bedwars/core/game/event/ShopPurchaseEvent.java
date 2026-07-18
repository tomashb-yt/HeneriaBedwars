package fr.heneria.bedwars.core.game.event;

import fr.heneria.bedwars.core.game.GameId;
import fr.heneria.bedwars.core.game.shop.ShopCurrency;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Internal event published only after inventory exchange succeeds. */
public record ShopPurchaseEvent(
    GameId gameId,
    UUID playerId,
    String offerId,
    ShopCurrency currency,
    int price,
    Instant occurredAt)
    implements GameEvent {
  public ShopPurchaseEvent {
    Objects.requireNonNull(gameId, "gameId");
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(offerId, "offerId");
    Objects.requireNonNull(currency, "currency");
    Objects.requireNonNull(occurredAt, "occurredAt");
  }
}
