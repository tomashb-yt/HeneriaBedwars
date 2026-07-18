package fr.heneria.bedwars.plugin.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.heneria.bedwars.core.game.GameId;
import fr.heneria.bedwars.core.statistics.CompletedMatchStatistics;
import fr.heneria.bedwars.core.statistics.LeaderboardMetric;
import fr.heneria.bedwars.core.statistics.MatchParticipantStatistics;
import fr.heneria.bedwars.core.statistics.MatchRecordResult;
import fr.heneria.bedwars.core.statistics.PlayerIdentity;
import fr.heneria.bedwars.core.statistics.PlayerStatistics;
import fr.heneria.bedwars.core.statistics.StatisticsLeaderboardEntry;
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
      repository.saveIdentity(new PlayerIdentity(winnerId, "Winner")).toCompletableFuture().join();
      repository.saveIdentity(new PlayerIdentity(loserId, "Loser")).toCompletableFuture().join();
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
      assertEquals(
          winnerId,
          reopened.findIdentity("winner").toCompletableFuture().join().orElseThrow().playerId());
      List<StatisticsLeaderboardEntry> kills =
          reopened.leaderboard(LeaderboardMetric.KILLS, 10).toCompletableFuture().join();
      assertEquals(
          List.of("Winner", "Loser"), kills.stream().map(e -> e.identity().currentName()).toList());
      assertEquals(List.of(5L, 1L), kills.stream().map(StatisticsLeaderboardEntry::score).toList());
    }
  }

  @Test
  void migratesTicket017DatabaseAndPersistsRenamedIdentity() throws Exception {
    Path directory =
        Files.createDirectories(Path.of("build", "test-work", "statistics-" + UUID.randomUUID()));
    Path database = directory.resolve("legacy.db");
    Class.forName("org.sqlite.JDBC");
    try (var connection = java.sql.DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = connection.createStatement()) {
      statement.execute(
          """
          CREATE TABLE player_statistics (
            player_uuid TEXT PRIMARY KEY, games_played INTEGER NOT NULL DEFAULT 0,
            wins INTEGER NOT NULL DEFAULT 0, kills INTEGER NOT NULL DEFAULT 0,
            deaths INTEGER NOT NULL DEFAULT 0, final_kills INTEGER NOT NULL DEFAULT 0,
            beds_destroyed INTEGER NOT NULL DEFAULT 0,
            play_time_seconds INTEGER NOT NULL DEFAULT 0,
            current_win_streak INTEGER NOT NULL DEFAULT 0,
            best_win_streak INTEGER NOT NULL DEFAULT 0, last_played_at INTEGER
          )
          """);
    }
    UUID playerId = UUID.randomUUID();
    try (SqliteStatisticsRepository repository = new SqliteStatisticsRepository(database, 2_000)) {
      repository.initialize().toCompletableFuture().join();
      repository.saveIdentity(new PlayerIdentity(playerId, "OldName")).toCompletableFuture().join();
      repository.saveIdentity(new PlayerIdentity(playerId, "NewName")).toCompletableFuture().join();
      assertTrue(repository.findIdentity("oldname").toCompletableFuture().join().isEmpty());
      assertEquals(
          "NewName",
          repository
              .findIdentity("newname")
              .toCompletableFuture()
              .join()
              .orElseThrow()
              .currentName());
    }
  }
}
