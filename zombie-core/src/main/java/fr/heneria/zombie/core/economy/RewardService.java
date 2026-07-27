package fr.heneria.zombie.core.economy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToDoubleFunction;

/** Central combat and cooperation reward boundary with contribution and anti-farm tracking. */
public final class RewardService {
  private final TransactionService transactions;
  private final ToDoubleFunction<UUID> multiplier;
  private final Clock clock;
  private final RewardPolicy policy;
  private final Map<UUID, Map<UUID, Contributions>> contributions = new ConcurrentHashMap<>();
  private final Map<String, Instant> revives = new ConcurrentHashMap<>();

  public RewardService(
      TransactionService transactions,
      ToDoubleFunction<UUID> multiplier,
      Clock clock,
      RewardPolicy policy) {
    this.transactions = transactions;
    this.multiplier = multiplier;
    this.clock = clock;
    this.policy = policy;
  }

  public TransactionResult hit(
      UUID gameId,
      UUID playerId,
      UUID zombieId,
      double appliedDamage,
      long baseReward,
      String operationId) {
    if (!Double.isFinite(appliedDamage) || appliedDamage <= 0) {
      return TransactionResult.failure(TransactionStatus.INVALID_AMOUNT, "No applied damage");
    }
    contributions
        .computeIfAbsent(gameId, ignored -> new ConcurrentHashMap<>())
        .computeIfAbsent(zombieId, ignored -> new Contributions())
        .add(playerId, appliedDamage);
    return reward(
        gameId,
        playerId,
        Math.min(baseReward, policy.maximumHitReward()),
        TransactionReason.ZOMBIE_HIT,
        operationId,
        Map.of("zombieId", zombieId.toString()));
  }

  public KillRewards kill(
      UUID gameId,
      UUID killerId,
      UUID zombieId,
      long baseReward,
      TransactionReason reason,
      String operationId) {
    Contributions damage =
        Optional.ofNullable(contributions.get(gameId))
            .map(values -> values.remove(zombieId))
            .orElse(null);
    TransactionResult killer =
        killerId == null
            ? TransactionResult.failure(TransactionStatus.PLAYER_NOT_FOUND, "No killer")
            : reward(
                gameId,
                killerId,
                baseReward,
                reason,
                operationId + ":killer",
                Map.of("zombieId", zombieId.toString()));
    LinkedHashMap<UUID, TransactionResult> assists = new LinkedHashMap<>();
    if (policy.assistsEnabled() && damage != null && damage.total > 0) {
      damage.values.forEach(
          (playerId, value) -> {
            if (!playerId.equals(killerId)
                && value / damage.total >= policy.minimumAssistPercentage()) {
              assists.put(
                  playerId,
                  reward(
                      gameId,
                      playerId,
                      policy.fixedAssistReward(),
                      TransactionReason.ZOMBIE_ASSIST,
                      operationId + ":assist:" + playerId,
                      Map.of("zombieId", zombieId.toString())));
            }
          });
    }
    return new KillRewards(killer, Map.copyOf(assists));
  }

  public TransactionResult revive(UUID gameId, UUID reviverId, UUID targetId, String operationId) {
    String key = gameId + ":" + reviverId + ":" + targetId;
    Instant now = clock.instant();
    Instant previous = revives.get(key);
    if (previous != null && previous.plus(policy.reviveAntiFarmWindow()).isAfter(now)) {
      return TransactionResult.failure(TransactionStatus.CANCELLED, "Revive anti-farm window");
    }
    revives.put(key, now);
    return reward(
        gameId,
        reviverId,
        policy.reviveReward(),
        TransactionReason.PLAYER_REVIVE,
        operationId,
        Map.of("targetId", targetId.toString()));
  }

  public TransactionResult reward(
      UUID gameId,
      UUID playerId,
      long baseReward,
      TransactionReason reason,
      String operationId,
      Map<String, String> metadata) {
    if (baseReward < 0) {
      return TransactionResult.failure(TransactionStatus.INVALID_AMOUNT, "Negative reward");
    }
    long amount;
    try {
      amount =
          BigDecimal.valueOf(baseReward)
              .multiply(BigDecimal.valueOf(multiplier.applyAsDouble(gameId)))
              .setScale(0, RoundingMode.HALF_UP)
              .longValueExact();
    } catch (ArithmeticException failure) {
      return TransactionResult.failure(TransactionStatus.INVALID_AMOUNT, "Reward overflow");
    }
    return transactions.credit(
        new TransactionRequest(gameId, playerId, amount, reason, operationId, metadata));
  }

  public void clear(UUID gameId) {
    contributions.remove(gameId);
    revives.keySet().removeIf(key -> key.startsWith(gameId + ":"));
  }

  public record RewardPolicy(
      long maximumHitReward,
      boolean assistsEnabled,
      double minimumAssistPercentage,
      long fixedAssistReward,
      long reviveReward,
      Duration reviveAntiFarmWindow) {
    public RewardPolicy {
      if (maximumHitReward < 0
          || !Double.isFinite(minimumAssistPercentage)
          || minimumAssistPercentage < 0
          || minimumAssistPercentage > 1
          || fixedAssistReward < 0
          || reviveReward < 0
          || reviveAntiFarmWindow.isNegative()) {
        throw new IllegalArgumentException("Invalid reward policy");
      }
    }
  }

  public record KillRewards(TransactionResult killer, Map<UUID, TransactionResult> assistants) {}

  private static final class Contributions {
    private final Map<UUID, Double> values = new LinkedHashMap<>();
    private double total;

    private void add(UUID playerId, double damage) {
      values.merge(playerId, damage, Double::sum);
      total += damage;
    }
  }
}
