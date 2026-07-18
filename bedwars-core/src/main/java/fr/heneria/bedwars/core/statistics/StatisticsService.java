package fr.heneria.bedwars.core.statistics;

import fr.heneria.bedwars.core.game.GameInstance;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/** Captures final runtime snapshots and exposes durable player aggregates. */
public final class StatisticsService {
  private final StatisticsRepository repository;

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

  public void close() {
    repository.close();
  }
}
