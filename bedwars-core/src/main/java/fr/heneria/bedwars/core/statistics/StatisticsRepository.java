package fr.heneria.bedwars.core.statistics;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/** Asynchronous persistence port; implementations must atomically deduplicate matches. */
public interface StatisticsRepository extends AutoCloseable {
  CompletionStage<Void> initialize();

  CompletionStage<MatchRecordResult> record(CompletedMatchStatistics match);

  CompletionStage<Optional<PlayerStatistics>> find(UUID playerId);

  CompletionStage<Void> saveIdentity(PlayerIdentity identity);

  CompletionStage<Optional<PlayerIdentity>> findIdentity(UUID playerId);

  CompletionStage<Optional<PlayerIdentity>> findIdentity(String normalizedName);

  CompletionStage<List<StatisticsLeaderboardEntry>> leaderboard(
      LeaderboardMetric metric, int limit);

  @Override
  void close();
}
