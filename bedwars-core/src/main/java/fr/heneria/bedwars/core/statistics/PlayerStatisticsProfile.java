package fr.heneria.bedwars.core.statistics;

import java.util.Objects;

/** Named player profile combining durable aggregates and derived progression. */
public record PlayerStatisticsProfile(
    PlayerIdentity identity,
    PlayerStatistics statistics,
    PlayerProgression progression,
    PlayerBalance balance) {
  public PlayerStatisticsProfile {
    Objects.requireNonNull(identity, "identity");
    Objects.requireNonNull(statistics, "statistics");
    Objects.requireNonNull(progression, "progression");
    Objects.requireNonNull(balance, "balance");
    if (!identity.playerId().equals(statistics.playerId()))
      throw new IllegalArgumentException("profile UUID mismatch");
    if (!identity.playerId().equals(balance.playerId()))
      throw new IllegalArgumentException("balance UUID mismatch");
  }
}
