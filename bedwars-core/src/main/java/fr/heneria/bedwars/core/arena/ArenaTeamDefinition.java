package fr.heneria.bedwars.core.arena;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable, persistent configuration of one team within an arena. */
public record ArenaTeamDefinition(
    TeamId id,
    String displayName,
    TeamColor color,
    int order,
    int capacity,
    Optional<ArenaLocation> spawn,
    Optional<ArenaLocation> bedLocation,
    Optional<ArenaLocation> shopLocation,
    Optional<ArenaLocation> upgradeShopLocation,
    Map<String, String> metadata) {
  public ArenaTeamDefinition {
    id = Objects.requireNonNull(id, "id");
    displayName = Objects.requireNonNull(displayName, "displayName").trim();
    if (displayName.isEmpty() || displayName.length() > 64)
      throw new IllegalArgumentException("Team display name must contain 1 to 64 characters");
    color = Objects.requireNonNull(color, "color");
    if (order < 1) throw new IllegalArgumentException("Team order must be positive");
    if (capacity < 1) throw new IllegalArgumentException("Team capacity must be positive");
    spawn = Objects.requireNonNull(spawn, "spawn");
    bedLocation = Objects.requireNonNull(bedLocation, "bedLocation");
    shopLocation = Objects.requireNonNull(shopLocation, "shopLocation");
    upgradeShopLocation = Objects.requireNonNull(upgradeShopLocation, "upgradeShopLocation");
    metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
  }

  public ArenaTeamDefinition withSpawn(Optional<ArenaLocation> value) {
    return new ArenaTeamDefinition(
        id,
        displayName,
        color,
        order,
        capacity,
        value,
        bedLocation,
        shopLocation,
        upgradeShopLocation,
        metadata);
  }

  /** Stores the foot-half selection of the team's administrative bed. */
  public ArenaTeamDefinition withBedLocation(Optional<ArenaLocation> value) {
    return new ArenaTeamDefinition(
        id,
        displayName,
        color,
        order,
        capacity,
        spawn,
        value,
        shopLocation,
        upgradeShopLocation,
        metadata);
  }

  public ArenaTeamDefinition withDisplayName(String value) {
    return new ArenaTeamDefinition(
        id,
        value,
        color,
        order,
        capacity,
        spawn,
        bedLocation,
        shopLocation,
        upgradeShopLocation,
        metadata);
  }

  public ArenaTeamDefinition withColor(TeamColor value) {
    return new ArenaTeamDefinition(
        id,
        displayName,
        value,
        order,
        capacity,
        spawn,
        bedLocation,
        shopLocation,
        upgradeShopLocation,
        metadata);
  }

  public ArenaTeamDefinition withCapacity(int value) {
    return new ArenaTeamDefinition(
        id,
        displayName,
        color,
        order,
        value,
        spawn,
        bedLocation,
        shopLocation,
        upgradeShopLocation,
        metadata);
  }

  public ArenaTeamDefinition withOrder(int value) {
    return new ArenaTeamDefinition(
        id,
        displayName,
        color,
        value,
        capacity,
        spawn,
        bedLocation,
        shopLocation,
        upgradeShopLocation,
        metadata);
  }
}
