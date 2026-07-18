package fr.heneria.bedwars.core.game.event;

import fr.heneria.bedwars.core.game.GameId;
import java.time.Instant;

/** Internal event contract; deliberately independent from Bukkit's event bus. */
public sealed interface GameEvent
    permits GameCreateEvent,
        GameWaitingEvent,
        GameStartEvent,
        GameEndEvent,
        GameDestroyEvent,
        PlayerJoinGameEvent,
        PlayerLeaveGameEvent,
        PlayerGameJoinEvent,
        PlayerGameLeaveEvent,
        GameCountdownStartEvent,
        GameCountdownTickEvent,
        GameCountdownCancelEvent,
        GameCountdownAccelerateEvent,
        BedDestroyedEvent,
        PlayerGameDeathEvent,
        PlayerRespawnScheduledEvent,
        PlayerGameRespawnEvent,
        PlayerFinalDeathEvent,
        TeamEliminatedEvent,
        GameVictoryEvent,
        ShopPurchaseEvent,
        TeamUpgradePurchaseEvent {
  GameId gameId();

  Instant occurredAt();
}
