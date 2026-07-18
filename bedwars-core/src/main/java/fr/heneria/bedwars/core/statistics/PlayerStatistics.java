package fr.heneria.bedwars.core.statistics;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Immutable durable aggregate for one player across completed matches. */
public record PlayerStatistics(
    UUID playerId,
    long gamesPlayed,
    long wins,
    long kills,
    long deaths,
    long finalKills,
    long bedsDestroyed,
    long playTimeSeconds,
    long currentWinStreak,
    long bestWinStreak,
    Optional<Instant> lastPlayedAt) {
  public PlayerStatistics {
    Objects.requireNonNull(playerId, "playerId");
    lastPlayedAt = lastPlayedAt == null ? Optional.empty() : lastPlayedAt;
    if (gamesPlayed < 0
        || wins < 0
        || kills < 0
        || deaths < 0
        || finalKills < 0
        || bedsDestroyed < 0
        || playTimeSeconds < 0
        || currentWinStreak < 0
        || bestWinStreak < 0) throw new IllegalArgumentException("statistics cannot be negative");
    if (wins > gamesPlayed) throw new IllegalArgumentException("wins cannot exceed games");
  }

  public static PlayerStatistics empty(UUID playerId) {
    return new PlayerStatistics(playerId, 0, 0, 0, 0, 0, 0, 0, 0, 0, Optional.empty());
  }

  public long losses() {
    return gamesPlayed - wins;
  }

  public double winRate() {
    return gamesPlayed == 0 ? 0.0 : wins * 100.0 / gamesPlayed;
  }

  public double killDeathRatio() {
    return deaths == 0 ? kills : (double) kills / deaths;
  }
}
