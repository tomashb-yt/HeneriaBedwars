package fr.heneria.bedwars.core.statistics;

import java.util.Objects;

/** One already-ranked row returned by a statistics repository. */
public record StatisticsLeaderboardEntry(
    int rank, PlayerIdentity identity, PlayerStatistics statistics, LeaderboardMetric metric) {
  public StatisticsLeaderboardEntry {
    if (rank < 1) throw new IllegalArgumentException("rank must be positive");
    Objects.requireNonNull(identity, "identity");
    Objects.requireNonNull(statistics, "statistics");
    Objects.requireNonNull(metric, "metric");
    if (!identity.playerId().equals(statistics.playerId()))
      throw new IllegalArgumentException("leaderboard UUID mismatch");
  }

  public long score() {
    return metric.value(statistics);
  }
}
