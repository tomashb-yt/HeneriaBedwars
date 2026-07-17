package fr.heneria.bedwars.core.game.event;

import fr.heneria.bedwars.core.game.GameId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record BedDestroyedEvent(GameId gameId, String teamId, UUID destroyerId, Instant occurredAt)
    implements GameEvent {
  public BedDestroyedEvent {
    Objects.requireNonNull(gameId, "gameId");
    Objects.requireNonNull(teamId, "teamId");
    Objects.requireNonNull(destroyerId, "destroyerId");
    Objects.requireNonNull(occurredAt, "occurredAt");
  }
}
