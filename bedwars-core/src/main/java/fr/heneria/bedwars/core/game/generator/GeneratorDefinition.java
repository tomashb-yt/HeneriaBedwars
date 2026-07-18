package fr.heneria.bedwars.core.game.generator;

import fr.heneria.bedwars.core.game.RuntimeLocation;
import java.time.Duration;
import java.util.Objects;

/** Immutable generator rules captured for one running match. */
public record GeneratorDefinition(
    GeneratorId id,
    GeneratorResource resource,
    RuntimeLocation location,
    int level,
    Duration interval,
    int amountPerEmission,
    int localCapacity,
    GeneratorStackingStrategy stackingStrategy) {
  public GeneratorDefinition {
    id = Objects.requireNonNull(id, "id");
    resource = Objects.requireNonNull(resource, "resource");
    location = Objects.requireNonNull(location, "location");
    interval = Objects.requireNonNull(interval, "interval");
    stackingStrategy = Objects.requireNonNull(stackingStrategy, "stackingStrategy");
    if (level < 1) throw new IllegalArgumentException("Generator level must be positive");
    if (interval.isZero() || interval.isNegative())
      throw new IllegalArgumentException("Generator interval must be positive");
    if (amountPerEmission < 1)
      throw new IllegalArgumentException("Generator emission amount must be positive");
    if (localCapacity < 1)
      throw new IllegalArgumentException("Generator local capacity must be positive");
  }

  public GeneratorDefinition withLevel(int newLevel, Duration newInterval) {
    return new GeneratorDefinition(
        id,
        resource,
        location,
        newLevel,
        newInterval,
        amountPerEmission,
        localCapacity,
        stackingStrategy);
  }

  public GeneratorDefinition withInterval(Duration newInterval) {
    return new GeneratorDefinition(
        id,
        resource,
        location,
        level,
        newInterval,
        amountPerEmission,
        localCapacity,
        stackingStrategy);
  }
}
