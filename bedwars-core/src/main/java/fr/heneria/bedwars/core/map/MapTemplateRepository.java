package fr.heneria.bedwars.core.map;

import java.io.IOException;
import java.util.Optional;

/**
 * Metadata-only persistence port.
 *
 * <p>Implementations must write atomically and constrain every path to the metadata directory. They
 * never load or unload Bukkit worlds.
 */
public interface MapTemplateRepository {
  MapTemplateLoadResult loadAll() throws IOException;

  Optional<MapTemplate> load(MapId id) throws IOException;

  void save(MapTemplate template) throws IOException;

  void delete(MapId id) throws IOException;

  boolean exists(MapId id);
}
