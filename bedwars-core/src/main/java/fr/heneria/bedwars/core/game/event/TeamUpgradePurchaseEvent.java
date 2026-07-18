package fr.heneria.bedwars.core.game.event;

import fr.heneria.bedwars.core.game.GameId;
import fr.heneria.bedwars.core.game.upgrade.TeamUpgradeType;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Internal notification emitted after a team upgrade has been paid and applied. */
public record TeamUpgradePurchaseEvent(
    GameId gameId,
    UUID playerId,
    String teamId,
    TeamUpgradeType type,
    int level,
    Instant occurredAt)
    implements GameEvent {
  public TeamUpgradePurchaseEvent {
    Objects.requireNonNull(gameId, "gameId");
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(teamId, "teamId");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(occurredAt, "occurredAt");
  }
}
