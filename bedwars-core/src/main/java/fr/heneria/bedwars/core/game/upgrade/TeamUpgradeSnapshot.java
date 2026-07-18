package fr.heneria.bedwars.core.game.upgrade;

import java.util.Map;

/** Immutable team upgrade levels. */
public record TeamUpgradeSnapshot(Map<TeamUpgradeType, Integer> levels) {
  public TeamUpgradeSnapshot {
    levels = Map.copyOf(levels);
  }

  public int level(TeamUpgradeType type) {
    return levels.getOrDefault(type, 0);
  }
}
