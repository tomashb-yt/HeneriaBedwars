package fr.heneria.bedwars.core.map;

/**
 * Main-thread boundary for managed Bukkit worlds.
 *
 * <p>Implementations create/load/save/unload only the controlled template world represented by the
 * supplied immutable metadata. File copying is deliberately excluded from this port.
 */
public interface MapWorldService {
  MapWorldResult createVoidWorld(MapTemplate template);

  MapWorldResult load(MapTemplate template);

  MapWorldResult save(MapTemplate template);

  MapWorldResult unload(MapTemplate template, boolean save);

  boolean isLoaded(MapTemplate template);

  int playerCount(MapTemplate template);
}
