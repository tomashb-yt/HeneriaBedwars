package fr.heneria.bedwars.core.statistics;

import java.util.Objects;
import java.util.UUID;

/** Final match contribution captured before the runtime instance is destroyed. */
public record MatchParticipantStatistics(
    UUID playerId,
    boolean winner,
    int kills,
    int deaths,
    int finalKills,
    int bedsDestroyed,
    long playTimeSeconds) {
  public MatchParticipantStatistics {
    Objects.requireNonNull(playerId, "playerId");
    if (kills < 0 || deaths < 0 || finalKills < 0 || bedsDestroyed < 0 || playTimeSeconds < 0)
      throw new IllegalArgumentException("match statistics cannot be negative");
  }
}
