package fr.heneria.bedwars.core.game.event;

import fr.heneria.bedwars.core.game.GameId;
import java.time.Instant;

public record GameWaitingEvent(GameId gameId, String worldName, Instant occurredAt)
    implements GameEvent {}
