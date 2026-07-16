package fr.heneria.bedwars.api.game;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/** Immutable public view of one player inside a game instance. */
public record RuntimePlayerSnapshot(
    UUID playerId,
    Optional<String> teamId,
    int kills,
    int deaths,
    int finalKills,
    int bedsDestroyed,
    boolean spectator,
    Duration playTime) {
  public RuntimePlayerSnapshot {
    teamId = teamId == null ? Optional.empty() : teamId;
  }
}
