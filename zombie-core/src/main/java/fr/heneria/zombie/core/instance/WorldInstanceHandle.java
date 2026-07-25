package fr.heneria.zombie.core.instance;

import java.util.Objects;

/**
 * Platform-neutral reference to a prepared runtime world.
 *
 * @param worldName exact platform world name
 */
public record WorldInstanceHandle(String worldName) {

  /** Validates the name. */
  public WorldInstanceHandle {
    Objects.requireNonNull(worldName, "worldName");
    if (worldName.isBlank()) {
      throw new IllegalArgumentException("worldName must not be blank");
    }
  }
}
