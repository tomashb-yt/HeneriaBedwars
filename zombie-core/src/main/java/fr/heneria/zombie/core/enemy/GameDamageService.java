package fr.heneria.zombie.core.enemy;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Central port for damage dealt by enemies to game players. */
@FunctionalInterface
public interface GameDamageService {
  Result damagePlayer(Request request);

  record Request(
      UUID gameId,
      UUID targetPlayerId,
      UUID zombieId,
      ZombieDamageType type,
      double rawDamage,
      Optional<String> abilityId,
      Map<String, Double> effects,
      boolean canDown,
      boolean cancellable) {
    public Request {
      java.util.Objects.requireNonNull(gameId, "gameId");
      java.util.Objects.requireNonNull(targetPlayerId, "targetPlayerId");
      java.util.Objects.requireNonNull(zombieId, "zombieId");
      java.util.Objects.requireNonNull(type, "type");
      java.util.Objects.requireNonNull(abilityId, "abilityId");
      effects = Map.copyOf(effects);
    }
  }

  record Result(double appliedDamage, boolean downed, boolean cancelled) {}
}
