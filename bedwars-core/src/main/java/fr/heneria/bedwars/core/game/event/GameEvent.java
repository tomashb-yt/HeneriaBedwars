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
        PlayerLeaveGameEvent {
  GameId gameId();

  Instant occurredAt();
}
