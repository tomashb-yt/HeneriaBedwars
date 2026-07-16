package fr.heneria.bedwars.core.arena;

import java.io.IOException;

/** Persistence port. Implementations must publish complete writes atomically. */
public interface ArenaRepository {
  ArenaLoadResult loadAll() throws IOException;

  void save(ArenaDefinition arena) throws IOException;

  void deleteWithBackup(ArenaId id) throws IOException;
}
