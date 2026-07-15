package fr.heneria.bedwars.plugin.bootstrap;

import fr.heneria.bedwars.api.PluginStatus;

/** Coordinates construction and shutdown while keeping the Paper entry point small. */
public interface PluginBootstrap {
  /** Builds and starts every required component. */
  void start() throws Exception;

  /** Stops every component and releases its resources. */
  void stop() throws Exception;

  /** Returns the externally visible runtime status. */
  PluginStatus status();

  /** Returns the number of services currently registered for diagnostics. */
  int serviceCount();
}
