package fr.heneria.bedwars.core.game.event;

import fr.heneria.bedwars.core.game.GameId;
import java.time.Instant;
import java.util.UUID;

public record PlayerRespawnScheduledEvent(
    GameId gameId, UUID playerId, Instant respawnAt, Instant occurredAt) implements GameEvent {}
