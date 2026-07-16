package fr.heneria.bedwars.core.map;

import java.util.Objects;

/** Bukkit-free construction spawn persisted with a map template. */
public record MapSpawn(
    boolean configured, String world, double x, double y, double z, float yaw, float pitch) {
  public MapSpawn {
    world = Objects.requireNonNull(world, "world").trim();
    if (world.isEmpty()) throw new IllegalArgumentException("Spawn world is blank");
    if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
      throw new IllegalArgumentException("Spawn coordinates must be finite");
    if (!Float.isFinite(yaw) || !Float.isFinite(pitch))
      throw new IllegalArgumentException("Spawn rotation must be finite");
  }

  public static MapSpawn defaultSpawn(String world, double y) {
    return new MapSpawn(false, world, 0.5, y + 1, 0.5, 0, 0);
  }
}
