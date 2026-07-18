package fr.heneria.bedwars.core.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProgressionPolicyTest {
  private final ProgressionPolicy policy = new ProgressionPolicy();

  @Test
  void derivesLevelAndProgressFromLifetimeContributions() {
    UUID playerId = UUID.randomUUID();
    PlayerStatistics statistics =
        new PlayerStatistics(playerId, 1, 1, 3, 1, 1, 1, 20, 1, 1, Optional.empty());

    PlayerProgression result = policy.progression(statistics);

    assertEquals(2, result.level());
    assertEquals(165, result.totalExperience());
    assertEquals(65, result.currentLevelExperience());
    assertEquals(300, result.requiredLevelExperience());
  }

  @Test
  void startsEmptyProfilesAtLevelOneAndHandlesHugeAggregatesSafely() {
    UUID playerId = UUID.randomUUID();
    assertEquals(
        new PlayerProgression(1, 0, 0, 100), policy.progression(PlayerStatistics.empty(playerId)));

    PlayerStatistics huge =
        new PlayerStatistics(
            playerId,
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            Optional.empty());
    PlayerProgression progression = policy.progression(huge);
    assertEquals(Long.MAX_VALUE, progression.totalExperience());
    assertTrue(progression.currentLevelExperience() < progression.requiredLevelExperience());
  }

  @Test
  void validatesIdentityAndMetricKeys() {
    UUID playerId = UUID.randomUUID();
    assertEquals("player_one", new PlayerIdentity(playerId, "Player_One").normalizedName());
    assertEquals(LeaderboardMetric.FINAL_KILLS, LeaderboardMetric.find("FINALS").orElseThrow());
    assertThrows(IllegalArgumentException.class, () -> new PlayerIdentity(playerId, "bad name"));
  }
}
