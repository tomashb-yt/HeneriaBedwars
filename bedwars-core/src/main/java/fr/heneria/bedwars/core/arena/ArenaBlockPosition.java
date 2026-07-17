package fr.heneria.bedwars.core.arena;

import java.util.Objects;

/** Integer block coordinate persisted independently from Bukkit. */
public record ArenaBlockPosition(String world, int x, int y, int z) {
  public ArenaBlockPosition {
    world = Objects.requireNonNull(world, "world").trim();
    if (world.isEmpty()) throw new IllegalArgumentException("Block world cannot be blank");
  }

  public static ArenaBlockPosition from(ArenaLocation location) {
    Objects.requireNonNull(location, "location");
    return new ArenaBlockPosition(
        location.world(),
        (int) Math.floor(location.position().x()),
        (int) Math.floor(location.position().y()),
        (int) Math.floor(location.position().z()));
  }

  public ArenaLocation location() {
    return new ArenaLocation(world, new ArenaVector(x, y, z), 0, 0);
  }
}
