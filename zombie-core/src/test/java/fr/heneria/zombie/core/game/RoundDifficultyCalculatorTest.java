package fr.heneria.zombie.core.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RoundDifficultyCalculatorTest {
  private final RoundDifficultyCalculator calculator = new RoundDifficultyCalculator();

  @Test
  void scalesAndBoundsEveryDifficultyDimension() {
    RoundConfiguration configuration = GameFixtures.configuration();

    assertEquals(5, calculator.enemyCount(1, 1, configuration));
    assertEquals(9, calculator.enemyCount(3, 2, configuration));
    assertEquals(22.0, calculator.zombieHealth(2, configuration), 0.001);
    assertEquals(10, calculator.maximumAlive(3, configuration));
    assertEquals(5, calculator.spawnDelayTicks(30, configuration));
  }

  @Test
  void rejectsInvalidRuntimeInputs() {
    assertThrows(
        IllegalArgumentException.class,
        () -> calculator.enemyCount(0, 1, GameFixtures.configuration()));
    assertThrows(
        IllegalArgumentException.class,
        () -> calculator.maximumAlive(0, GameFixtures.configuration()));
  }
}
