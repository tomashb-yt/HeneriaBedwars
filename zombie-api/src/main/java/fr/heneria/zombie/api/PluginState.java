package fr.heneria.zombie.api;

/** Observable lifecycle states of the HeneriaZombie plugin. */
public enum PluginState {
  STOPPED,
  STARTING,
  RUNNING,
  RELOADING,
  STOPPING,
  FAILED;

  /**
   * Returns whether gameplay-facing services may be queried safely.
   *
   * @return {@code true} only while the plugin is fully running
   */
  public boolean isReady() {
    return this == RUNNING;
  }
}
