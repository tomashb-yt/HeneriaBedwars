package fr.heneria.bedwars.core.statistics;

import java.util.Objects;
import java.util.UUID;

/** Durable Heneria coin balance kept separate from match statistics. */
public record PlayerBalance(UUID playerId, long coins) {
  public PlayerBalance {
    Objects.requireNonNull(playerId, "playerId");
    if (coins < 0) throw new IllegalArgumentException("coins cannot be negative");
  }

  public static PlayerBalance empty(UUID playerId) {
    return new PlayerBalance(playerId, 0);
  }
}
