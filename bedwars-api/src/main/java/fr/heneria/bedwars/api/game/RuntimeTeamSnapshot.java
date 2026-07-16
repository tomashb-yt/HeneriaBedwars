package fr.heneria.bedwars.api.game;

import java.util.Set;
import java.util.UUID;

/** Immutable public view of a runtime team scaffold. */
public record RuntimeTeamSnapshot(
    String id,
    String displayName,
    String color,
    Set<UUID> players,
    boolean bedAlive,
    boolean eliminated) {
  public RuntimeTeamSnapshot {
    players = Set.copyOf(players);
  }
}
