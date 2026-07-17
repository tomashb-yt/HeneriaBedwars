package fr.heneria.bedwars.core.game.event;

import fr.heneria.bedwars.core.game.GameId;
import java.time.Instant;

public record GameVictoryEvent(GameId gameId, String teamId, Instant occurredAt)
    implements GameEvent {}
