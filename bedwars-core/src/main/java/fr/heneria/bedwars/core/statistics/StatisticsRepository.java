package fr.heneria.bedwars.core.statistics;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/** Asynchronous persistence port; implementations must atomically deduplicate matches. */
public interface StatisticsRepository extends AutoCloseable {
  CompletionStage<Void> initialize();

  CompletionStage<MatchRecordResult> record(CompletedMatchStatistics match);

  CompletionStage<Optional<PlayerStatistics>> find(UUID playerId);

  @Override
  void close();
}
