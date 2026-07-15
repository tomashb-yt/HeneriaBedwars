package fr.heneria.bedwars.api;

/**
 * Minimal public entry point for HeneriaBedWars addons.
 *
 * <p>The API is intentionally limited during Ticket 001. New contracts must not expose internal
 * implementations and must follow semantic versioning.
 */
public interface HeneriaBedWarsApi {

  /**
   * Returns the plugin version that provides this API.
   *
   * @return the semantic plugin version
   */
  String version();

  /**
   * Returns the current high-level runtime status.
   *
   * @return the current status, never {@code null}
   */
  PluginStatus status();
}
