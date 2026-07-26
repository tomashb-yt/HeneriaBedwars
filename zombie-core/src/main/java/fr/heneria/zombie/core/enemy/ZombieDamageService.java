package fr.heneria.zombie.core.enemy;

import java.util.Optional;
import java.util.UUID;

/** Central enemy damage calculation with explicit headshot and resistance ordering. */
public final class ZombieDamageService {

  public Result damage(ZombieInstance target, Request request) {
    if (request.baseDamage() < 0 || !Double.isFinite(request.baseDamage())) {
      throw new IllegalArgumentException("baseDamage must be finite and >= 0");
    }
    ZombieDefinition.DamageProfile profile = target.definition().damage();
    if (profile.immunities().contains(request.type())) {
      return new Result(0, target.snapshot().health(), false, true, request.headshot());
    }
    double multiplier = profile.multipliers().getOrDefault(request.type(), 1.0);
    if (request.headshot() && profile.headshotsEnabled()) {
      multiplier *= profile.headshotMultiplier();
    }
    double applied =
        target.applyDamage(request.baseDamage() * multiplier, request.attackerPlayerId());
    double remaining = target.snapshot().health();
    return new Result(applied, remaining, applied > 0 && remaining <= 0, false, request.headshot());
  }

  public record Request(
      UUID attackerPlayerId,
      ZombieDamageType type,
      double baseDamage,
      boolean headshot,
      Optional<String> weaponId,
      Optional<String> abilityId) {
    public Request {
      type = java.util.Objects.requireNonNull(type, "type");
      weaponId = java.util.Objects.requireNonNull(weaponId, "weaponId");
      abilityId = java.util.Objects.requireNonNull(abilityId, "abilityId");
    }
  }

  public record Result(
      double appliedDamage,
      double remainingHealth,
      boolean lethal,
      boolean immune,
      boolean headshot) {}
}
