package fr.heneria.bedwars.core.game.event;

import fr.heneria.bedwars.core.game.GameId;
import java.time.Instant;

public record GameCreateEvent(GameId gameId, String arenaId, Instant occurredAt)
    implements GameEvent {}
