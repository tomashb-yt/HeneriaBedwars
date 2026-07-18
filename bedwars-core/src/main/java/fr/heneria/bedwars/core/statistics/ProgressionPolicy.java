package fr.heneria.bedwars.core.statistics;

import java.util.Objects;

/** Deterministic lifetime experience derived from durable statistics. */
public final class ProgressionPolicy {
  public PlayerProgression progression(PlayerStatistics statistics) {
    Objects.requireNonNull(statistics, "statistics");
    long experience = 0;
    experience = add(experience, weighted(statistics.gamesPlayed(), 10));
    experience = add(experience, weighted(statistics.wins(), 100));
    experience = add(experience, weighted(statistics.kills(), 5));
    experience = add(experience, weighted(statistics.finalKills(), 15));
    experience = add(experience, weighted(statistics.bedsDestroyed(), 25));

    long completedLevels = (long) Math.floor(Math.sqrt(experience / 100.0));
    int level = (int) Math.min(Integer.MAX_VALUE, completedLevels + 1);
    long levelStart = Math.min(experience, squareExperience(completedLevels));
    long required = Math.max(1, weighted(add(weighted(completedLevels, 2), 1), 100));
    return new PlayerProgression(level, experience, experience - levelStart, required);
  }

  private static long squareExperience(long value) {
    if (value != 0 && value > Long.MAX_VALUE / value) return Long.MAX_VALUE;
    return weighted(value * value, 100);
  }

  private static long weighted(long value, long weight) {
    if (value > Long.MAX_VALUE / weight) return Long.MAX_VALUE;
    return value * weight;
  }

  private static long add(long left, long right) {
    if (Long.MAX_VALUE - left < right) return Long.MAX_VALUE;
    return left + right;
  }
}
