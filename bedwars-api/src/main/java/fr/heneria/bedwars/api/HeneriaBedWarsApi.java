package fr.heneria.bedwars.api;

import fr.heneria.bedwars.api.game.ArenaGameApi;
import fr.heneria.bedwars.api.game.GameApi;
import fr.heneria.bedwars.api.game.PlayerGameApi;

/**
 * Minimal public entry point for HeneriaBedWars addons.
 *
 * <p>Public contracts expose immutable snapshots only. Implementations, Bukkit objects and mutable
 * runtime collections never cross this boundary.
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

  /** Returns the read-only game-instance API. */
  GameApi games();

  /** Returns the read-only player runtime API. */
  PlayerGameApi players();

  /** Returns the read-only arena runtime API. */
  ArenaGameApi arenas();
}
