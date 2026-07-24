package fr.heneria.bedwars.core.statistics;

import java.util.Objects;
import java.util.UUID;

/** Immutable and auditable reward breakdown for one match participant. */
public record MatchReward(
    UUID playerId,
    long participationCoins,
    long victoryCoins,
    long killCoins,
    long finalKillCoins,
    long bedDestroyedCoins,
    long totalCoins) {
  public MatchReward {
    Objects.requireNonNull(playerId, "playerId");
    if (participationCoins < 0
        || victoryCoins < 0
        || killCoins < 0
        || finalKillCoins < 0
        || bedDestroyedCoins < 0
        || totalCoins < 0) throw new IllegalArgumentException("reward values cannot be negative");
    long uncapped =
        saturatingAdd(
            participationCoins, victoryCoins, killCoins, finalKillCoins, bedDestroyedCoins);
    if (totalCoins > uncapped) throw new IllegalArgumentException("total exceeds reward breakdown");
  }

  private static long saturatingAdd(long... values) {
    long result = 0;
    for (long value : values) {
      if (Long.MAX_VALUE - result < value) return Long.MAX_VALUE;
      result += value;
    }
    return result;
  }
}
