package fr.heneria.bedwars.plugin.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import fr.heneria.bedwars.core.game.GameId;
import fr.heneria.bedwars.core.statistics.CompletedMatchStatistics;
import fr.heneria.bedwars.core.statistics.MatchParticipantStatistics;
import fr.heneria.bedwars.core.statistics.MatchRecordResult;
import fr.heneria.bedwars.core.statistics.PlayerStatistics;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SqliteStatisticsRepositoryTest {
  @Test
  void persistsAggregatesAcrossRestartAndDeduplicatesMatches() throws Exception {
    Path directory =
        Files.createDirectories(Path.of("build", "test-work", "statistics-" + UUID.randomUUID()));
    Path database = directory.resolve("players.db");
    UUID winnerId = UUID.randomUUID();
    UUID loserId = UUID.randomUUID();
    GameId gameId = GameId.random();
    Instant completedAt = Instant.parse("2026-07-18T14:30:00Z");
    CompletedMatchStatistics match =
        new CompletedMatchStatistics(
            gameId,
            "arena-one",
            "map-one",
            "red",
            completedAt,
            List.of(
                new MatchParticipantStatistics(winnerId, true, 5, 1, 2, 1, 300),
                new MatchParticipantStatistics(loserId, false, 1, 2, 0, 0, 280)));

    try (SqliteStatisticsRepository repository = new SqliteStatisticsRepository(database, 2_000)) {
      repository.initialize().toCompletableFuture().join();
      assertEquals(
          MatchRecordResult.RECORDED, repository.record(match).toCompletableFuture().join());
      assertEquals(
          MatchRecordResult.ALREADY_RECORDED,
          repository.record(match).toCompletableFuture().join());
    }

    try (SqliteStatisticsRepository reopened = new SqliteStatisticsRepository(database, 2_000)) {
      PlayerStatistics winner = reopened.find(winnerId).toCompletableFuture().join().orElseThrow();
      PlayerStatistics loser = reopened.find(loserId).toCompletableFuture().join().orElseThrow();
      assertEquals(1, winner.gamesPlayed());
      assertEquals(1, winner.wins());
      assertEquals(5, winner.kills());
      assertEquals(1, winner.currentWinStreak());
      assertEquals(1, loser.gamesPlayed());
      assertEquals(0, loser.wins());
      assertEquals(1, loser.losses());
      assertEquals(completedAt, loser.lastPlayedAt().orElseThrow());
    }
  }
}
