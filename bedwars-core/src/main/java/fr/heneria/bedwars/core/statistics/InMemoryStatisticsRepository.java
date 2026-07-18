package fr.heneria.bedwars.core.statistics;

import fr.heneria.bedwars.core.game.GameId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Non-durable compatibility fallback used when a future backend is selected. */
public final class InMemoryStatisticsRepository implements StatisticsRepository {
  private final Map<UUID, PlayerStatistics> players = new LinkedHashMap<>();
  private final Set<GameId> matches = new java.util.HashSet<>();

  @Override
  public CompletionStage<Void> initialize() {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public synchronized CompletionStage<MatchRecordResult> record(CompletedMatchStatistics match) {
    if (!matches.add(match.gameId()))
      return CompletableFuture.completedFuture(MatchRecordResult.ALREADY_RECORDED);
    for (MatchParticipantStatistics participant : match.participants()) {
      PlayerStatistics current =
          players.getOrDefault(
              participant.playerId(), PlayerStatistics.empty(participant.playerId()));
      long streak = participant.winner() ? current.currentWinStreak() + 1 : 0;
      players.put(
          participant.playerId(),
          new PlayerStatistics(
              participant.playerId(),
              current.gamesPlayed() + 1,
              current.wins() + (participant.winner() ? 1 : 0),
              current.kills() + participant.kills(),
              current.deaths() + participant.deaths(),
              current.finalKills() + participant.finalKills(),
              current.bedsDestroyed() + participant.bedsDestroyed(),
              current.playTimeSeconds() + participant.playTimeSeconds(),
              streak,
              Math.max(current.bestWinStreak(), streak),
              Optional.of(match.completedAt())));
    }
    return CompletableFuture.completedFuture(MatchRecordResult.RECORDED);
  }

  @Override
  public synchronized CompletionStage<Optional<PlayerStatistics>> find(UUID playerId) {
    return CompletableFuture.completedFuture(Optional.ofNullable(players.get(playerId)));
  }

  @Override
  public void close() {}
}
