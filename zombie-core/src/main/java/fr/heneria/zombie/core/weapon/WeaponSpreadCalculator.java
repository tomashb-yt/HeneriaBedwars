package fr.heneria.zombie.core.weapon;

import java.util.random.RandomGenerator;

/** Pure cone-spread calculator accepting an injected deterministic random generator. */
public final class WeaponSpreadCalculator {
  public Offset calculate(double spreadDegrees, RandomGenerator random) {
    if (!Double.isFinite(spreadDegrees) || spreadDegrees < 0 || spreadDegrees > 90) {
      throw new IllegalArgumentException("spreadDegrees must be between 0 and 90");
    }
    double radius = Math.sqrt(random.nextDouble()) * spreadDegrees;
    double angle = random.nextDouble() * Math.PI * 2;
    return new Offset(Math.cos(angle) * radius, Math.sin(angle) * radius);
  }

  public record Offset(double yawDegrees, double pitchDegrees) {}
}
