package fr.heneria.bedwars.core.statistics;

import java.util.List;
import java.util.Objects;

/** Result exposed to the platform after the atomic match and reward write. */
public record MatchRewardOutcome(MatchRecordResult result, List<MatchReward> rewards) {
  public MatchRewardOutcome {
    Objects.requireNonNull(result, "result");
    rewards = List.copyOf(rewards);
    if (result == MatchRecordResult.ALREADY_RECORDED && !rewards.isEmpty())
      throw new IllegalArgumentException("duplicate matches cannot expose new rewards");
  }
}
