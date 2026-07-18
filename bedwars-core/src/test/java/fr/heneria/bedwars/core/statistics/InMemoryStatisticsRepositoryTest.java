package fr.heneria.bedwars.core.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.heneria.bedwars.core.game.GameId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InMemoryStatisticsRepositoryTest {
  private static final Instant COMPLETED_AT = Instant.parse("2026-07-18T12:00:00Z");

  @Test
  void aggregatesMatchesUpdatesStreaksAndRejectsDuplicates() {
    InMemoryStatisticsRepository repository = new InMemoryStatisticsRepository();
    UUID playerId = UUID.randomUUID();
    CompletedMatchStatistics victory = match(GameId.random(), playerId, true, 4, 2, 1, 1, 90);

    assertEquals(
        MatchRecordResult.RECORDED, repository.record(victory).toCompletableFuture().join());
    assertEquals(
        MatchRecordResult.ALREADY_RECORDED,
        repository.record(victory).toCompletableFuture().join());
    repository
        .record(match(GameId.random(), playerId, true, 2, 1, 2, 0, 60))
        .toCompletableFuture()
        .join();
    repository
        .record(match(GameId.random(), playerId, false, 1, 3, 0, 0, 30))
        .toCompletableFuture()
        .join();

    PlayerStatistics result = repository.find(playerId).toCompletableFuture().join().orElseThrow();
    assertEquals(3, result.gamesPlayed());
    assertEquals(2, result.wins());
    assertEquals(1, result.losses());
    assertEquals(7, result.kills());
    assertEquals(6, result.deaths());
    assertEquals(3, result.finalKills());
    assertEquals(1, result.bedsDestroyed());
    assertEquals(180, result.playTimeSeconds());
    assertEquals(0, result.currentWinStreak());
    assertEquals(2, result.bestWinStreak());
    assertEquals(Optional.of(COMPLETED_AT), result.lastPlayedAt());
  }

  @Test
  void exposesSafeRatiosAndRejectsImpossibleAggregates() {
    UUID playerId = UUID.randomUUID();
    PlayerStatistics empty = PlayerStatistics.empty(playerId);

    assertEquals(0.0, empty.winRate());
    assertEquals(0.0, empty.killDeathRatio());
    assertThrows(
        IllegalArgumentException.class,
        () -> new PlayerStatistics(playerId, 1, 2, 0, 0, 0, 0, 0, 0, 0, Optional.empty()));
  }

  @Test
  void remembersCaseInsensitiveNamesAndHandlesRenames() {
    InMemoryStatisticsRepository repository = new InMemoryStatisticsRepository();
    UUID playerId = UUID.randomUUID();
    repository.saveIdentity(new PlayerIdentity(playerId, "FirstName")).toCompletableFuture().join();
    assertEquals(
        playerId,
        repository.findIdentity("firstname").toCompletableFuture().join().orElseThrow().playerId());

    repository.saveIdentity(new PlayerIdentity(playerId, "NewName")).toCompletableFuture().join();

    assertTrue(repository.findIdentity("FirstName").toCompletableFuture().join().isEmpty());
    assertEquals(
        "NewName",
        repository
            .findIdentity("NEWNAME")
            .toCompletableFuture()
            .join()
            .orElseThrow()
            .currentName());
  }

  @Test
  void ranksAggregatesByTheRequestedMetricWithStableRanks() {
    InMemoryStatisticsRepository repository = new InMemoryStatisticsRepository();
    UUID alpha = UUID.randomUUID();
    UUID beta = UUID.randomUUID();
    repository.saveIdentity(new PlayerIdentity(alpha, "Alpha")).toCompletableFuture().join();
    repository.saveIdentity(new PlayerIdentity(beta, "Beta")).toCompletableFuture().join();
    repository
        .record(match(GameId.random(), alpha, true, 2, 0, 1, 0, 20))
        .toCompletableFuture()
        .join();
    repository
        .record(match(GameId.random(), beta, false, 10, 1, 0, 0, 20))
        .toCompletableFuture()
        .join();

    List<StatisticsLeaderboardEntry> kills =
        repository.leaderboard(LeaderboardMetric.KILLS, 10).toCompletableFuture().join();
    List<StatisticsLeaderboardEntry> wins =
        repository.leaderboard(LeaderboardMetric.WINS, 1).toCompletableFuture().join();

    assertEquals(
        List.of("Beta", "Alpha"), kills.stream().map(e -> e.identity().currentName()).toList());
    assertEquals(List.of(1, 2), kills.stream().map(StatisticsLeaderboardEntry::rank).toList());
    assertEquals("Alpha", wins.getFirst().identity().currentName());
    assertEquals(1, wins.size());
  }

  private static CompletedMatchStatistics match(
      GameId id,
      UUID playerId,
      boolean winner,
      int kills,
      int deaths,
      int finalKills,
      int beds,
      long playTime) {
    return new CompletedMatchStatistics(
        id,
        "arena",
        "template",
        "red",
        COMPLETED_AT,
        List.of(
            new MatchParticipantStatistics(
                playerId, winner, kills, deaths, finalKills, beds, playTime)));
  }
}
