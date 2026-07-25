package fr.heneria.zombie.core.editor;

import java.util.Objects;

/** Platform-neutral immutable world position. */
public record MapPoint(String world, double x, double y, double z, float yaw, float pitch) {
  public MapPoint {
    Objects.requireNonNull(world, "world");
    if (world.isBlank()
        || !Double.isFinite(x)
        || !Double.isFinite(y)
        || !Double.isFinite(z)
        || !Float.isFinite(yaw)
        || !Float.isFinite(pitch)) {
      throw new IllegalArgumentException("Invalid map point");
    }
  }
}
