package fr.heneria.bedwars.core.arena;

import java.util.Objects;

/** Optional cuboid reserved for later arena editing and gameplay checks. */
public record ArenaBoundary(ArenaVector minimum, ArenaVector maximum) {
  public ArenaBoundary {
    Objects.requireNonNull(minimum, "minimum");
    Objects.requireNonNull(maximum, "maximum");
  }

  public boolean ordered() {
    return minimum.x() <= maximum.x() && minimum.y() <= maximum.y() && minimum.z() <= maximum.z();
  }
}
