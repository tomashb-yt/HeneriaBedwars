package fr.heneria.bedwars.core.statistics;

import fr.heneria.bedwars.core.game.GameInstance;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/** Captures final runtime snapshots and exposes durable player aggregates. */
public final class StatisticsService {
  private final StatisticsRepository repository;
  private final ProgressionPolicy progression = new ProgressionPolicy();
  private final Supplier<fr.heneria.bedwars.core.config.RewardSettings> rewards;

  public StatisticsService(StatisticsRepository repository) {
    this(
        repository,
        () -> new fr.heneria.bedwars.core.config.RewardSettings(false, 0, 0, 0, 0, 0, 0));
  }

  public StatisticsService(
      StatisticsRepository repository,
      Supplier<fr.heneria.bedwars.core.config.RewardSettings> rewards) {
    this.repository = Objects.requireNonNull(repository, "repository");
    this.rewards = Objects.requireNonNull(rewards, "rewards");
  }

  public CompletionStage<Void> initialize() {
    return repository.initialize();
  }

  public CompletionStage<MatchRewardOutcome> record(
      GameInstance game, String winnerTeamId, Instant completedAt) {
    Objects.requireNonNull(game, "game");
    Objects.requireNonNull(winnerTeamId, "winnerTeamId");
    Objects.requireNonNull(completedAt, "completedAt");
    var snapshot = game.snapshot(completedAt);
    var participants =
        game.participantSnapshots(completedAt).stream()
            .map(
                player ->
                    new MatchParticipantStatistics(
                        player.playerId(),
                        player.teamId().filter(winnerTeamId::equals).isPresent(),
                        player.kills(),
                        player.deaths(),
                        player.finalKills(),
                        player.bedsDestroyed(),
                        Math.max(0, player.playTime().toSeconds())))
            .toList();
    CompletedMatchStatistics match =
        new CompletedMatchStatistics(
            game.id(),
            snapshot.arenaId(),
            snapshot.mapTemplateId(),
            winnerTeamId,
            completedAt,
            participants);
    RewardPolicy policy = new RewardPolicy(rewards.get());
    List<MatchReward> grants = participants.stream().map(policy::reward).toList();
    return repository
        .record(match, grants)
        .thenApply(
            result ->
                new MatchRewardOutcome(
                    result, result == MatchRecordResult.RECORDED ? grants : List.of()));
  }

  public CompletionStage<PlayerStatistics> statistics(UUID playerId) {
    Objects.requireNonNull(playerId, "playerId");
    return repository
        .find(playerId)
        .thenApply(found -> found.orElseGet(() -> PlayerStatistics.empty(playerId)));
  }

  public CompletionStage<PlayerBalance> balance(UUID playerId) {
    Objects.requireNonNull(playerId, "playerId");
    return repository.balance(playerId);
  }

  public CompletionStage<Void> rememberPlayer(UUID playerId, String name) {
    return repository.saveIdentity(new PlayerIdentity(playerId, name));
  }

  public CompletionStage<PlayerStatisticsProfile> profile(UUID playerId, String currentName) {
    PlayerIdentity identity = new PlayerIdentity(playerId, currentName);
    return repository
        .saveIdentity(identity)
        .thenCompose(
            ignored ->
                statistics(playerId)
                    .thenCombine(balance(playerId), (value, balance) -> profile(identity, value, balance)));
  }

  public CompletionStage<Optional<PlayerStatisticsProfile>> profile(String playerName) {
    String normalized = PlayerIdentity.normalize(playerName);
    return repository
        .findIdentity(normalized)
        .thenCompose(
            identity ->
                identity
                    .map(
                        value ->
                            statistics(value.playerId())
                                .thenCombine(
                                    balance(value.playerId()),
                                    (statistics, balance) ->
                                        Optional.of(profile(value, statistics, balance))))
                    .orElseGet(
                        () ->
                            java.util.concurrent.CompletableFuture.completedFuture(
                                Optional.empty())));
  }

  public CompletionStage<List<StatisticsLeaderboardEntry>> leaderboard(
      LeaderboardMetric metric, int limit) {
    Objects.requireNonNull(metric, "metric");
    if (limit < 1 || limit > 100) throw new IllegalArgumentException("limit must be 1 to 100");
    return repository.leaderboard(metric, limit);
  }

  public PlayerProgression progression(PlayerStatistics value) {
    return progression.progression(value);
  }

  private PlayerStatisticsProfile profile(
      PlayerIdentity identity, PlayerStatistics value, PlayerBalance balance) {
    return new PlayerStatisticsProfile(identity, value, progression.progression(value), balance);
  }

  public void close() {
    repository.close();
  }
}
