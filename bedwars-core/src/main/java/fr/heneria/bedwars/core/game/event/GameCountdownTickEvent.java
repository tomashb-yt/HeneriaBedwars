package fr.heneria.bedwars.core.game.event;

import fr.heneria.bedwars.core.game.GameId;
import java.time.Instant;

public record GameCountdownTickEvent(
    GameId gameId, int remainingSeconds, int initialDuration, Instant occurredAt)
    implements GameEvent {}
