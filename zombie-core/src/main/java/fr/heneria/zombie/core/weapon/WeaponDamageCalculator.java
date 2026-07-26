package fr.heneria.zombie.core.weapon;

/** Pure damage falloff, upgrade, headshot and penetration calculator. */
public final class WeaponDamageCalculator {
  public double calculate(
      WeaponDefinition definition,
      double distance,
      boolean headshot,
      double upgradeMultiplier,
      int penetrationIndex,
      double externalMultiplier) {
    if (distance < 0
        || penetrationIndex < 0
        || !Double.isFinite(upgradeMultiplier)
        || upgradeMultiplier <= 0
        || !Double.isFinite(externalMultiplier)
        || externalMultiplier <= 0) {
      throw new IllegalArgumentException("Invalid damage context");
    }
    WeaponDefinition.Damage damage = definition.damage();
    double distanceMultiplier;
    if (distance <= damage.minimumDistance()) {
      distanceMultiplier = 1;
    } else if (distance >= damage.maximumDistance()) {
      distanceMultiplier = damage.minimumMultiplier();
    } else {
      double progress =
          (distance - damage.minimumDistance())
              / (damage.maximumDistance() - damage.minimumDistance());
      distanceMultiplier = 1 - progress * (1 - damage.minimumMultiplier());
    }
    double penetration = Math.pow(definition.penetration().damageRetention(), penetrationIndex);
    double critical = headshot ? damage.headshotMultiplier() : 1;
    double result =
        damage.baseDamage()
            * distanceMultiplier
            * penetration
            * critical
            * upgradeMultiplier
            * externalMultiplier;
    if (!Double.isFinite(result) || result < 0) {
      throw new IllegalArgumentException("Calculated damage is outside finite bounds");
    }
    return result;
  }
}
