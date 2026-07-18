package fr.heneria.bedwars.core.arena;

import fr.heneria.bedwars.core.game.RuntimeLocation;
import fr.heneria.bedwars.core.game.generator.GeneratorDefinition;
import fr.heneria.bedwars.core.game.generator.GeneratorId;
import fr.heneria.bedwars.core.game.generator.GeneratorResource;
import fr.heneria.bedwars.core.game.generator.GeneratorStackingStrategy;
import java.time.Duration;
import java.util.Objects;

/** Persistent generator definition expressed in the administrative template world. */
public record ArenaGeneratorDefinition(
    GeneratorId id,
    GeneratorResource resource,
    ArenaLocation location,
    int level,
    long intervalTicks,
    int amountPerEmission,
    int localCapacity,
    GeneratorStackingStrategy stackingStrategy) {
  private static final long MAX_INTERVAL_TICKS = 1_728_000L;
  private static final int MAX_AMOUNT_PER_EMISSION = 4096;
  private static final int MAX_LOCAL_CAPACITY = 65_536;

  public ArenaGeneratorDefinition {
    id = Objects.requireNonNull(id, "id");
    resource = Objects.requireNonNull(resource, "resource");
    location = Objects.requireNonNull(location, "location");
    stackingStrategy = Objects.requireNonNull(stackingStrategy, "stackingStrategy");
    if (level < 1) throw new IllegalArgumentException("Generator level must be positive");
    if (intervalTicks < 1)
      throw new IllegalArgumentException("Generator interval must be positive");
    if (intervalTicks > MAX_INTERVAL_TICKS)
      throw new IllegalArgumentException("Generator interval cannot exceed 24 hours");
    if (amountPerEmission < 1)
      throw new IllegalArgumentException("Generator amount must be positive");
    if (amountPerEmission > MAX_AMOUNT_PER_EMISSION)
      throw new IllegalArgumentException("Generator amount is too large");
    if (localCapacity < 1)
      throw new IllegalArgumentException("Generator capacity must be positive");
    if (localCapacity > MAX_LOCAL_CAPACITY)
      throw new IllegalArgumentException("Generator capacity is too large");
  }

  public GeneratorDefinition runtime() {
    return new GeneratorDefinition(
        id,
        resource,
        new RuntimeLocation(
            location.position().x(),
            location.position().y(),
            location.position().z(),
            location.yaw(),
            location.pitch()),
        level,
        Duration.ofMillis(Math.multiplyExact(intervalTicks, 50L)),
        amountPerEmission,
        localCapacity,
        stackingStrategy);
  }

  public ArenaGeneratorDefinition at(ArenaLocation value) {
    return new ArenaGeneratorDefinition(
        id,
        resource,
        value,
        level,
        intervalTicks,
        amountPerEmission,
        localCapacity,
        stackingStrategy);
  }
}
