package fr.heneria.zombie.core.enemy;

/** Pure, deterministic attribute calculator used before touching a Paper entity. */
public final class ZombieAttributeCalculator {

  public CalculatedAttributes calculate(
      ZombieDefinition definition, int round, double healthModifier, double damageModifier) {
    if (round <= 0
        || !Double.isFinite(healthModifier)
        || healthModifier <= 0
        || !Double.isFinite(damageModifier)
        || damageModifier <= 0) {
      throw new IllegalArgumentException("Invalid attribute calculation context");
    }
    ZombieDefinition.Attributes source = definition.attributes();
    double health =
        source.healthBase()
            * (source.scaleHealth()
                ? Math.pow(source.healthRoundMultiplier(), Math.max(0, round - 1))
                : 1)
            * healthModifier;
    if (source.maximumHealth() > 0) {
      health = Math.min(health, source.maximumHealth());
    }
    double damage =
        source.damageBase()
            * (source.scaleDamage()
                ? Math.pow(source.damageRoundMultiplier(), Math.max(0, round - 1))
                : 1)
            * damageModifier;
    if (!Double.isFinite(health) || health <= 0 || !Double.isFinite(damage) || damage < 0) {
      throw new IllegalArgumentException("Calculated attributes are outside finite bounds");
    }
    return new CalculatedAttributes(
        health,
        damage,
        source.speedBase(),
        source.followRange(),
        source.knockbackResistance(),
        source.attackKnockback());
  }

  public record CalculatedAttributes(
      double maximumHealth,
      double attackDamage,
      double movementSpeed,
      double followRange,
      double knockbackResistance,
      double attackKnockback) {}
}
