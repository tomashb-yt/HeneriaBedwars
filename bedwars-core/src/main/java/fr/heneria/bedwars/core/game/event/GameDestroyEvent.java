package fr.heneria.bedwars.core.game.event;

import fr.heneria.bedwars.core.game.GameId;
import java.time.Instant;

public record GameDestroyEvent(GameId gameId, Instant occurredAt) implements GameEvent {}
