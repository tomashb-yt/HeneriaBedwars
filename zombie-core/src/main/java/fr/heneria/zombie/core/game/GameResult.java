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
    Map<UUID, GamePlayer.Snapshot> players,
    Map<UUID, EconomyPlayerResult> economy) {
  public GameResult {
    players = Map.copyOf(players);
    economy = economy == null ? Map.of() : Map.copyOf(economy);
  }

  public GameResult(
      UUID gameId,
      String mapId,
      GameEndReason reason,
      int maximumRound,
      Duration duration,
      Map<UUID, GamePlayer.Snapshot> players) {
    this(gameId, mapId, reason, maximumRound, duration, players, Map.of());
  }

  public GameResult withEconomy(Map<UUID, EconomyPlayerResult> values) {
    return new GameResult(gameId, mapId, reason, maximumRound, duration, players, values);
  }

  /** Aggregated financial result safe for asynchronous persistence. */
  public record EconomyPlayerResult(
      long earned,
      long spent,
      long refunded,
      long finalBalance,
      long transactionCount,
      long purchaseCount,
      long largestExpense,
      long collectedPowerUps) {}
}
