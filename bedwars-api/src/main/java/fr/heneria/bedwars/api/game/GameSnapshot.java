package fr.heneria.bedwars.api.game;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Immutable, serialization-friendly public view of a live game instance. */
public record GameSnapshot(
    UUID id,
    GameState state,
    String arenaId,
    String mapTemplateId,
    Optional<String> worldName,
    List<RuntimePlayerSnapshot> players,
    List<RuntimeTeamSnapshot> teams,
    Map<String, Long> timers,
    Map<String, Long> statistics,
    Instant createdAt,
    Instant updatedAt) {
  public GameSnapshot {
    worldName = worldName == null ? Optional.empty() : worldName;
    players = List.copyOf(players);
    teams = List.copyOf(teams);
    timers = Map.copyOf(timers);
    statistics = Map.copyOf(statistics);
  }
}
