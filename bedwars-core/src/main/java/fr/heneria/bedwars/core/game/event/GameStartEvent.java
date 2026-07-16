package fr.heneria.bedwars.core.game.event;

import fr.heneria.bedwars.core.game.GameId;
import java.time.Instant;

public record GameStartEvent(GameId gameId, Instant occurredAt) implements GameEvent {}
