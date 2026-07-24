package fr.heneria.bedwars.core.statistics;

import fr.heneria.bedwars.core.config.RewardSettings;
import java.util.Objects;

/** Pure reward calculator; persistence and Bukkit notifications remain outside this class. */
public final class RewardPolicy {
  private final RewardSettings settings;

  public RewardPolicy(RewardSettings settings) {
    this.settings = Objects.requireNonNull(settings, "settings");
  }

  public MatchReward reward(MatchParticipantStatistics participant) {
    Objects.requireNonNull(participant, "participant");
    if (!settings.enabled()) return empty(participant);
    long participation = settings.participationCoins();
    long victory = participant.winner() ? settings.victoryCoins() : 0;
    long kills = multiply(participant.kills(), settings.killCoins());
    long finalKills = multiply(participant.finalKills(), settings.finalKillCoins());
    long beds = multiply(participant.bedsDestroyed(), settings.bedDestroyedCoins());
    long total = add(participation, victory, kills, finalKills, beds);
    if (settings.maximumCoinsPerMatch() > 0)
      total = Math.min(total, settings.maximumCoinsPerMatch());
    return new MatchReward(
        participant.playerId(), participation, victory, kills, finalKills, beds, total);
  }

  private static MatchReward empty(MatchParticipantStatistics participant) {
    return new MatchReward(participant.playerId(), 0, 0, 0, 0, 0, 0);
  }

  private static long multiply(long left, long right) {
    if (left == 0 || right == 0) return 0;
    return left > Long.MAX_VALUE / right ? Long.MAX_VALUE : left * right;
  }

  private static long add(long... values) {
    long result = 0;
    for (long value : values) {
      if (Long.MAX_VALUE - result < value) return Long.MAX_VALUE;
      result += value;
    }
    return result;
  }
}
