package fr.heneria.bedwars.core.game;

/** World-independent coordinates copied from an administrative arena definition. */
public record RuntimeLocation(double x, double y, double z, float yaw, float pitch) {
  public RuntimeLocation {
    if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
      throw new IllegalArgumentException("Runtime coordinates must be finite");
    if (!Float.isFinite(yaw) || !Float.isFinite(pitch))
      throw new IllegalArgumentException("Runtime rotation must be finite");
  }
}
