package fr.heneria.bedwars.core.game.event;

import fr.heneria.bedwars.core.game.GameId;
import java.time.Instant;

public record GameEndEvent(GameId gameId, String reason, Instant occurredAt) implements GameEvent {}
