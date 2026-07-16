package fr.heneria.bedwars.core.arena;

import java.util.Objects;
import java.util.Optional;

/** Persisted administrative cuboid; partial points allow safe step-by-step editing. */
public record ArenaBoundary(
    boolean enabled, Optional<ArenaVector> minimum, Optional<ArenaVector> maximum) {
  public ArenaBoundary {
    Objects.requireNonNull(minimum, "minimum");
    Objects.requireNonNull(maximum, "maximum");
  }

  public ArenaBoundary(ArenaVector minimum, ArenaVector maximum) {
    this(true, Optional.of(minimum), Optional.of(maximum));
  }

  public static ArenaBoundary empty() {
    return new ArenaBoundary(false, Optional.empty(), Optional.empty());
  }

  public boolean ordered() {
    if (minimum.isEmpty() || maximum.isEmpty()) return false;
    ArenaVector min = minimum.orElseThrow();
    ArenaVector max = maximum.orElseThrow();
    return min.x() <= max.x() && min.y() <= max.y() && min.z() <= max.z();
  }
}
