package fr.heneria.bedwars.core.statistics;

import fr.heneria.bedwars.core.game.GameId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Non-durable compatibility fallback used when a future backend is selected. */
public final class InMemoryStatisticsRepository implements StatisticsRepository {
  private final Map<UUID, PlayerStatistics> players = new LinkedHashMap<>();
  private final Map<UUID, PlayerIdentity> identities = new LinkedHashMap<>();
  private final Map<String, UUID> identityNames = new LinkedHashMap<>();
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
  public synchronized CompletionStage<Void> saveIdentity(PlayerIdentity identity) {
    PlayerIdentity previous = identities.get(identity.playerId());
    if (previous != null) identityNames.remove(previous.normalizedName());
    UUID previousOwner = identityNames.put(identity.normalizedName(), identity.playerId());
    if (previousOwner != null && !previousOwner.equals(identity.playerId()))
      identities.remove(previousOwner);
    identities.put(identity.playerId(), identity);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public synchronized CompletionStage<Optional<PlayerIdentity>> findIdentity(UUID playerId) {
    return CompletableFuture.completedFuture(Optional.ofNullable(identities.get(playerId)));
  }

  @Override
  public synchronized CompletionStage<Optional<PlayerIdentity>> findIdentity(
      String normalizedName) {
    UUID playerId = identityNames.get(PlayerIdentity.normalize(normalizedName));
    return CompletableFuture.completedFuture(Optional.ofNullable(identities.get(playerId)));
  }

  @Override
  public synchronized CompletionStage<List<StatisticsLeaderboardEntry>> leaderboard(
      LeaderboardMetric metric, int limit) {
    if (limit < 1 || limit > 100) throw new IllegalArgumentException("limit must be 1 to 100");
    List<PlayerStatistics> ranking =
        players.values().stream()
            .sorted(
                Comparator.comparingLong(metric::value)
                    .reversed()
                    .thenComparing(Comparator.comparingLong(PlayerStatistics::wins).reversed())
                    .thenComparing(
                        Comparator.comparingLong(PlayerStatistics::finalKills).reversed())
                    .thenComparing(value -> value.playerId().toString()))
            .limit(limit)
            .toList();
    List<StatisticsLeaderboardEntry> entries = new java.util.ArrayList<>();
    for (int index = 0; index < ranking.size(); index++) {
      PlayerStatistics statistics = ranking.get(index);
      PlayerIdentity identity =
          identities.getOrDefault(
              statistics.playerId(),
              new PlayerIdentity(
                  statistics.playerId(), statistics.playerId().toString().substring(0, 8)));
      entries.add(new StatisticsLeaderboardEntry(index + 1, identity, statistics, metric));
    }
    return CompletableFuture.completedFuture(List.copyOf(entries));
  }

  @Override
  public void close() {}
}
