package fr.heneria.bedwars.core.game.event;

import fr.heneria.bedwars.core.game.GameId;
import java.time.Instant;

public record GameCountdownAccelerateEvent(GameId gameId, int remainingSeconds, Instant occurredAt)
    implements GameEvent {}
