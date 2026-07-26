package fr.heneria.zombie.core.game;

/** Immutable validated gameplay values resolved before a game starts. */
public record RoundConfiguration(
    int minimumPlayers,
    int countdownSeconds,
    boolean cancelCountdownWhenInsufficient,
    boolean joinInProgress,
    int endScreenSeconds,
    int startingPoints,
    int maximumRound,
    int firstRoundDelaySeconds,
    int transitionSeconds,
    int enemyBase,
    int enemiesPerRound,
    double playerMultiplier,
    int minimumEnemies,
    int maximumEnemies,
    double baseHealth,
    double healthMultiplier,
    double maximumHealth,
    int maximumAliveBase,
    int maximumAlivePerPlayer,
    int initialSpawnDelayTicks,
    int spawnDelayTicks,
    int minimumSpawnDelayTicks,
    int batchSize,
    boolean downedEnabled,
    int bleedOutSeconds,
    int reviveSeconds,
    double reviveHealth,
    int pointsPerKill) {

  public RoundConfiguration {
    if (minimumPlayers <= 0
        || countdownSeconds < 0
        || endScreenSeconds < 0
        || startingPoints < 0
        || maximumRound == 0
        || maximumRound < -1
        || firstRoundDelaySeconds < 0
        || transitionSeconds < 0
        || enemyBase < 0
        || enemiesPerRound < 0
        || playerMultiplier < 0
        || minimumEnemies <= 0
        || maximumEnemies == 0
        || maximumEnemies < -1
        || baseHealth <= 0
        || healthMultiplier < 1
        || maximumHealth == 0
        || maximumHealth < -1
        || maximumAliveBase <= 0
        || maximumAlivePerPlayer < 0
        || initialSpawnDelayTicks < 0
        || spawnDelayTicks <= 0
        || minimumSpawnDelayTicks <= 0
        || batchSize <= 0
        || bleedOutSeconds <= 0
        || reviveSeconds <= 0
        || reviveHealth <= 0
        || pointsPerKill < 0) {
      throw new IllegalArgumentException("Invalid round configuration");
    }
  }
}
