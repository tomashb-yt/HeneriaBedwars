package fr.heneria.bedwars.core.statistics;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.function.ToLongFunction;

/** Public, stable ranking dimensions backed by durable aggregate columns. */
public enum LeaderboardMetric {
  WINS("wins", PlayerStatistics::wins),
  FINAL_KILLS("finals", PlayerStatistics::finalKills),
  KILLS("kills", PlayerStatistics::kills),
  BEDS_DESTROYED("beds", PlayerStatistics::bedsDestroyed),
  GAMES_PLAYED("games", PlayerStatistics::gamesPlayed),
  BEST_WIN_STREAK("streak", PlayerStatistics::bestWinStreak);

  private final String key;
  private final ToLongFunction<PlayerStatistics> value;

  LeaderboardMetric(String key, ToLongFunction<PlayerStatistics> value) {
    this.key = key;
    this.value = value;
  }

  public String key() {
    return key;
  }

  public long value(PlayerStatistics statistics) {
    return value.applyAsLong(statistics);
  }

  public static Optional<LeaderboardMetric> find(String value) {
    String key = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    return Arrays.stream(values()).filter(metric -> metric.key.equals(key)).findFirst();
  }
}
