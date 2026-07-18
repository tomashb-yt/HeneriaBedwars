package fr.heneria.bedwars.core.statistics;

import fr.heneria.bedwars.core.game.GameId;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Immutable result persisted exactly once for a victorious match. */
public record CompletedMatchStatistics(
    GameId gameId,
    String arenaId,
    String mapTemplateId,
    String winnerTeamId,
    Instant completedAt,
    List<MatchParticipantStatistics> participants) {
  public CompletedMatchStatistics {
    Objects.requireNonNull(gameId, "gameId");
    arenaId = text(arenaId, "arenaId");
    mapTemplateId = text(mapTemplateId, "mapTemplateId");
    winnerTeamId = text(winnerTeamId, "winnerTeamId");
    Objects.requireNonNull(completedAt, "completedAt");
    participants = List.copyOf(participants);
    if (participants.isEmpty()) throw new IllegalArgumentException("participants are empty");
    if (participants.stream().map(MatchParticipantStatistics::playerId).distinct().count()
        != participants.size()) throw new IllegalArgumentException("duplicate participant");
  }

  private static String text(String value, String field) {
    String clean = Objects.requireNonNull(value, field).trim();
    if (clean.isEmpty()) throw new IllegalArgumentException(field + " is blank");
    return clean;
  }
}
