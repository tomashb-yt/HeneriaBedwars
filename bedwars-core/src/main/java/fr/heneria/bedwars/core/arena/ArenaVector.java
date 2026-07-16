package fr.heneria.bedwars.core.arena;

/** Pure three-dimensional coordinate. */
public record ArenaVector(double x, double y, double z) {
  public ArenaVector {
    if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
      throw new IllegalArgumentException("Arena coordinates must be finite");
    }
  }
}
