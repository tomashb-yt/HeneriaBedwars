package fr.heneria.bedwars.core.statistics;

import fr.heneria.bedwars.core.game.GameInstance;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/** Captures final runtime snapshots and exposes durable player aggregates. */
public final class StatisticsService {
  private final StatisticsRepository repository;
  private final ProgressionPolicy progression = new ProgressionPolicy();

  public StatisticsService(StatisticsRepository repository) {
    this.repository = Objects.requireNonNull(repository, "repository");
  }

  public CompletionStage<Void> initialize() {
    return repository.initialize();
  }

  public CompletionStage<MatchRecordResult> record(
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
    return repository.record(
        new CompletedMatchStatistics(
            game.id(),
            snapshot.arenaId(),
            snapshot.mapTemplateId(),
            winnerTeamId,
            completedAt,
            participants));
  }

  public CompletionStage<PlayerStatistics> statistics(UUID playerId) {
    Objects.requireNonNull(playerId, "playerId");
    return repository
        .find(playerId)
        .thenApply(found -> found.orElseGet(() -> PlayerStatistics.empty(playerId)));
  }

  public CompletionStage<Void> rememberPlayer(UUID playerId, String name) {
    return repository.saveIdentity(new PlayerIdentity(playerId, name));
  }

  public CompletionStage<PlayerStatisticsProfile> profile(UUID playerId, String currentName) {
    PlayerIdentity identity = new PlayerIdentity(playerId, currentName);
    return repository
        .saveIdentity(identity)
        .thenCompose(ignored -> statistics(playerId))
        .thenApply(value -> profile(identity, value));
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
                                .thenApply(statistics -> Optional.of(profile(value, statistics))))
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

  private PlayerStatisticsProfile profile(PlayerIdentity identity, PlayerStatistics value) {
    return new PlayerStatisticsProfile(identity, value, progression.progression(value));
  }

  public void close() {
    repository.close();
  }
}
