package fr.heneria.bedwars.core.game.event;

import fr.heneria.bedwars.core.game.GameId;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public record PlayerGameDeathEvent(
    GameId gameId, UUID playerId, Optional<UUID> killerId, boolean finalDeath, Instant occurredAt)
    implements GameEvent {}
