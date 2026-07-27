package fr.heneria.zombie.core.powerup;

import java.time.Duration;

/** Validated, immutable bonus definition. */
public record PowerUpDefinition(
    PowerUpType type,
    String displayName,
    PowerUpScope scope,
    Duration duration,
    Duration maximumDuration,
    StackPolicy stackPolicy,
    double pointMultiplier,
    int dropWeight) {
  public PowerUpDefinition {
    java.util.Objects.requireNonNull(type, "type");
    java.util.Objects.requireNonNull(displayName, "displayName");
    java.util.Objects.requireNonNull(scope, "scope");
    java.util.Objects.requireNonNull(duration, "duration");
    java.util.Objects.requireNonNull(maximumDuration, "maximumDuration");
    java.util.Objects.requireNonNull(stackPolicy, "stackPolicy");
    if (duration.isNegative()
        || maximumDuration.isNegative()
        || !Double.isFinite(pointMultiplier)
        || pointMultiplier < 0
        || dropWeight < 1) {
      throw new IllegalArgumentException("Invalid power-up definition");
    }
  }
}
