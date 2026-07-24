package fr.heneria.zombie.api;

/**
 * Stable, read-only entry point exposed to future addons.
 *
 * <p>No Paper, Bukkit, core implementation or mutable collection is exposed by this contract.
 */
public interface ZombieApi {

  /**
   * Returns the current plugin lifecycle state.
   *
   * @return current state
   */
  PluginState state();

  /**
   * Returns the number of validated map definitions currently registered.
   *
   * @return non-negative map count
   */
  int registeredMapCount();

  /**
   * Returns the number of live game instances.
   *
   * @return non-negative instance count
   */
  int activeInstanceCount();
}
