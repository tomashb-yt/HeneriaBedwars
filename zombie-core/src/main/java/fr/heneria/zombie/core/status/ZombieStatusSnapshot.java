package fr.heneria.zombie.core.status;

import fr.heneria.zombie.api.PluginState;
import java.util.Objects;

/**
 * Immutable status rendered by commands and future diagnostics.
 *
 * @param pluginName plugin display name
 * @param version plugin version
 * @param state lifecycle state
 * @param registeredMaps map count
 * @param activeInstances instance count
 */
public record ZombieStatusSnapshot(
    String pluginName, String version, PluginState state, int registeredMaps, int activeInstances) {

  /** Validates status invariants. */
  public ZombieStatusSnapshot {
    Objects.requireNonNull(pluginName, "pluginName");
    Objects.requireNonNull(version, "version");
    Objects.requireNonNull(state, "state");
    if (registeredMaps < 0 || activeInstances < 0) {
      throw new IllegalArgumentException("Runtime counts cannot be negative");
    }
  }
}
