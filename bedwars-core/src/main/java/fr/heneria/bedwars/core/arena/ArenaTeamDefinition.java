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
  private static final String BED_HEAD_WORLD = "bed-head-world";
  private static final String BED_HEAD_X = "bed-head-x";
  private static final String BED_HEAD_Y = "bed-head-y";
  private static final String BED_HEAD_Z = "bed-head-z";
  private static final String BED_FACING = "bed-facing";

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

  /** Stores the item-shop NPC position in the administrative template world. */
  public ArenaTeamDefinition withShopLocation(Optional<ArenaLocation> value) {
    return new ArenaTeamDefinition(
        id,
        displayName,
        color,
        order,
        capacity,
        spawn,
        bedLocation,
        value,
        upgradeShopLocation,
        metadata);
  }

  /** Stores the team-upgrade NPC position in the administrative template world. */
  public ArenaTeamDefinition withUpgradeShopLocation(Optional<ArenaLocation> value) {
    return new ArenaTeamDefinition(
        id, displayName, color, order, capacity, spawn, bedLocation, shopLocation, value, metadata);
  }

  /** Stores both bed halves while preserving the historical foot location field. */
  public ArenaTeamDefinition withBedDefinition(Optional<ArenaBedDefinition> value) {
    Map<String, String> next = new java.util.LinkedHashMap<>(metadata);
    next.keySet()
        .removeAll(
            java.util.Set.of(BED_HEAD_WORLD, BED_HEAD_X, BED_HEAD_Y, BED_HEAD_Z, BED_FACING));
    Optional<ArenaLocation> foot = Optional.empty();
    if (value.isPresent()) {
      ArenaBedDefinition bed = value.orElseThrow();
      foot = Optional.of(bed.foot().location());
      next.put(BED_HEAD_WORLD, bed.head().world());
      next.put(BED_HEAD_X, Integer.toString(bed.head().x()));
      next.put(BED_HEAD_Y, Integer.toString(bed.head().y()));
      next.put(BED_HEAD_Z, Integer.toString(bed.head().z()));
      next.put(BED_FACING, bed.facing());
    }
    return new ArenaTeamDefinition(
        id,
        displayName,
        color,
        order,
        capacity,
        spawn,
        foot,
        shopLocation,
        upgradeShopLocation,
        next);
  }

  /** Returns a complete bed only for definitions selected by the two-block editor. */
  public Optional<ArenaBedDefinition> bedDefinition() {
    if (bedLocation.isEmpty()) return Optional.empty();
    try {
      ArenaBlockPosition head =
          new ArenaBlockPosition(
              metadata.get(BED_HEAD_WORLD),
              Integer.parseInt(metadata.get(BED_HEAD_X)),
              Integer.parseInt(metadata.get(BED_HEAD_Y)),
              Integer.parseInt(metadata.get(BED_HEAD_Z)));
      return Optional.of(
          new ArenaBedDefinition(
              ArenaBlockPosition.from(bedLocation.orElseThrow()), head, metadata.get(BED_FACING)));
    } catch (RuntimeException ignored) {
      return Optional.empty();
    }
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
