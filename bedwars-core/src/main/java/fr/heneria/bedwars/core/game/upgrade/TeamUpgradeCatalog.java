package fr.heneria.bedwars.core.game.upgrade;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Immutable upgrade catalog with one definition per type. */
public final class TeamUpgradeCatalog {
  private final Map<TeamUpgradeType, TeamUpgradeDefinition> definitions;

  public TeamUpgradeCatalog(List<TeamUpgradeDefinition> values) {
    EnumMap<TeamUpgradeType, TeamUpgradeDefinition> indexed = new EnumMap<>(TeamUpgradeType.class);
    for (TeamUpgradeDefinition value : values)
      if (indexed.putIfAbsent(value.type(), value) != null)
        throw new IllegalArgumentException("Duplicate upgrade " + value.type());
    definitions = Map.copyOf(indexed);
  }

  public Optional<TeamUpgradeDefinition> find(TeamUpgradeType type) {
    return Optional.ofNullable(definitions.get(type));
  }

  public List<TeamUpgradeDefinition> all() {
    return definitions.values().stream()
        .sorted(Comparator.comparingInt(TeamUpgradeDefinition::order))
        .toList();
  }
}
