package fr.heneria.zombie.core.enemy;

import java.util.Optional;
import java.util.UUID;

/** Boundary between enemy navigation and the future destructible-barricade runtime. */
@FunctionalInterface
public interface BarricadeTargetService {
  Optional<BarricadeTarget> findBlockingBarricade(ZombieInstance zombie, UUID targetPlayerId);

  record BarricadeTarget(String id, String zoneId, double health) {
    public BarricadeTarget {
      if (id == null || id.isBlank() || zoneId == null || health < 0) {
        throw new IllegalArgumentException("Invalid barricade target");
      }
    }
  }
}
