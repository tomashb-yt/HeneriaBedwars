package fr.heneria.bedwars.core.game.event;

import fr.heneria.bedwars.core.game.GameId;
import java.time.Instant;
import java.util.UUID;

public record PlayerLeaveGameEvent(GameId gameId, UUID playerId, Instant occurredAt)
    implements GameEvent {}
