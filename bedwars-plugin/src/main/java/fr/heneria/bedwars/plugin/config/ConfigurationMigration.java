package fr.heneria.bedwars.plugin.config;

import java.io.IOException;
import java.nio.file.Path;

/** Contract for future sequential configuration migrations. */
public interface ConfigurationMigration {
  int sourceVersion();

  int targetVersion();

  void migrate(Path file) throws IOException;
}
