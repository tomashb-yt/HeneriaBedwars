package fr.heneria.bedwars.core.arena;

import java.util.Objects;

/** World-qualified position without a Bukkit dependency. */
public record ArenaLocation(String world, ArenaVector position, float yaw, float pitch) {
  public ArenaLocation {
    world = Objects.requireNonNull(world, "world").trim();
    position = Objects.requireNonNull(position, "position");
    if (world.isEmpty()) throw new IllegalArgumentException("Location world cannot be blank");
    if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) {
      throw new IllegalArgumentException("Location rotation must be finite");
    }
  }
}
