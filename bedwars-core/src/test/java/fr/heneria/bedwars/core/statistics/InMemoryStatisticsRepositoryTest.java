package fr.heneria.bedwars.core.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
