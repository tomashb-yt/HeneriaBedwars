package fr.heneria.bedwars.core.game.event;

import fr.heneria.bedwars.core.game.GameId;
import java.time.Instant;

public record GameCountdownStartEvent(
    GameId gameId, int durationSeconds, boolean forced, Instant occurredAt) implements GameEvent {}
