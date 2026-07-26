package fr.heneria.zombie.core.game;

/** Pure and bounded round difficulty calculations. */
public final class RoundDifficultyCalculator {

  public int enemyCount(int round, int activePlayers, RoundConfiguration configuration) {
    if (round <= 0 || activePlayers <= 0) {
      throw new IllegalArgumentException("Round and player count must be positive");
    }
    double raw =
        (configuration.enemyBase() + (long) configuration.enemiesPerRound() * (round - 1))
            * Math.max(1.0, activePlayers * configuration.playerMultiplier());
    long rounded = Math.max(configuration.minimumEnemies(), Math.round(raw));
    return configuration.maximumEnemies() == -1
        ? Math.toIntExact(Math.min(Integer.MAX_VALUE, rounded))
        : (int) Math.min(configuration.maximumEnemies(), rounded);
  }

  public double zombieHealth(int round, RoundConfiguration configuration) {
    if (round <= 0) {
      throw new IllegalArgumentException("Round must be positive");
    }
    double health =
        configuration.baseHealth() * Math.pow(configuration.healthMultiplier(), round - 1);
    return configuration.maximumHealth() == -1
        ? health
        : Math.min(configuration.maximumHealth(), health);
  }

  public int maximumAlive(int activePlayers, RoundConfiguration configuration) {
    if (activePlayers <= 0) {
      throw new IllegalArgumentException("Player count must be positive");
    }
    return Math.addExact(
        configuration.maximumAliveBase(),
        Math.multiplyExact(configuration.maximumAlivePerPlayer(), activePlayers));
  }

  public int spawnDelayTicks(int round, RoundConfiguration configuration) {
    return Math.max(
        configuration.minimumSpawnDelayTicks(), configuration.spawnDelayTicks() - (round - 1));
  }
}
