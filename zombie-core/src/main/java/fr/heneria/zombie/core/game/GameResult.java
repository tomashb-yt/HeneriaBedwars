package fr.heneria.zombie.core.game;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/** Immutable result emitted once when a game ends. */
public record GameResult(
    UUID gameId,
    String mapId,
    GameEndReason reason,
    int maximumRound,
    Duration duration,
    Map<UUID, GamePlayer.Snapshot> players) {
  public GameResult {
    players = Map.copyOf(players);
  }
}
