package fr.heneria.bedwars.core.config;

/** Typed and bounded coin rewards applied to one completed match. */
public record RewardSettings(
    boolean enabled,
    int participationCoins,
    int victoryCoins,
    int killCoins,
    int finalKillCoins,
    int bedDestroyedCoins,
    int maximumCoinsPerMatch) {
  public RewardSettings {
    if (participationCoins < 0
        || victoryCoins < 0
        || killCoins < 0
        || finalKillCoins < 0
        || bedDestroyedCoins < 0
        || maximumCoinsPerMatch < 0) {
      throw new IllegalArgumentException("reward values cannot be negative");
    }
  }
}
