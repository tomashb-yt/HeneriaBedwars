package fr.heneria.bedwars.api;

/** Describes the externally visible state of the plugin foundation. */
public enum PluginStatus {
  /** Components are currently starting. */
  STARTING,
  /** All required components are available. */
  RUNNING,
  /** Components are currently stopping. */
  STOPPING,
  /** The plugin is not running. */
  STOPPED,
  /** A critical lifecycle operation failed. */
  FAILED
}
