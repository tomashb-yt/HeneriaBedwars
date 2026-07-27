package fr.heneria.zombie.core.powerup;

import java.time.Duration;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Optional;

/** Immutable registry of known power-up definitions. */
public final class PowerUpRegistry {
  private final EnumMap<PowerUpType, PowerUpDefinition> definitions;

  public PowerUpRegistry(Collection<PowerUpDefinition> definitions) {
    this.definitions = new EnumMap<>(PowerUpType.class);
    for (PowerUpDefinition definition : definitions) {
      if (this.definitions.put(definition.type(), definition) != null) {
        throw new IllegalArgumentException("Duplicate power-up " + definition.type());
      }
    }
  }

  public Optional<PowerUpDefinition> find(PowerUpType type) {
    return Optional.ofNullable(definitions.get(type));
  }

  public Collection<PowerUpDefinition> all() {
    return java.util.List.copyOf(definitions.values());
  }

  public static PowerUpRegistry defaults() {
    return new PowerUpRegistry(
        java.util.List.of(
            new PowerUpDefinition(
                PowerUpType.DOUBLE_POINTS,
                "Points doubles",
                PowerUpScope.GAME,
                Duration.ofSeconds(30),
                Duration.ofSeconds(60),
                StackPolicy.EXTEND_DURATION,
                2,
                30),
            new PowerUpDefinition(
                PowerUpType.INSTA_KILL,
                "Mort instantanee",
                PowerUpScope.GAME,
                Duration.ofSeconds(30),
                Duration.ofSeconds(60),
                StackPolicy.EXTEND_DURATION,
                1,
                25),
            new PowerUpDefinition(
                PowerUpType.MAX_AMMO,
                "Munitions max",
                PowerUpScope.GAME,
                Duration.ZERO,
                Duration.ZERO,
                StackPolicy.REJECT,
                1,
                25),
            new PowerUpDefinition(
                PowerUpType.NUKE,
                "Nuke",
                PowerUpScope.GAME,
                Duration.ZERO,
                Duration.ZERO,
                StackPolicy.REJECT,
                1,
                20)));
  }
}
