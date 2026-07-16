package fr.heneria.bedwars.plugin.config;

import fr.heneria.bedwars.core.config.ConfigurationId;
import fr.heneria.bedwars.core.logging.ProjectLogger;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Creates only missing runtime files from resources embedded in the plugin JAR. */
public final class DefaultConfigurationInstaller {
  @FunctionalInterface
  public interface ResourceProvider {
    InputStream open(String path) throws IOException;
  }

  private final Path dataDirectory;
  private final ResourceProvider resources;
  private final ProjectLogger logger;

  public DefaultConfigurationInstaller(
      Path dataDirectory, ResourceProvider resources, ProjectLogger logger) {
    this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
    this.resources = Objects.requireNonNull(resources, "resources");
    this.logger = Objects.requireNonNull(logger, "logger");
  }

  public int installMissing() throws IOException {
    Files.createDirectories(dataDirectory);
    if (!Files.isWritable(dataDirectory))
      throw new IOException("Plugin data directory is not writable: " + dataDirectory);
    Files.createDirectories(dataDirectory.resolve("arenas"));
    Files.createDirectories(dataDirectory.resolve("languages"));
    Files.createDirectories(dataDirectory.resolve("backups"));
    Files.createDirectories(dataDirectory.resolve("maps/templates"));
    Files.createDirectories(dataDirectory.resolve("maps/metadata"));
    Files.createDirectories(dataDirectory.resolve("instances"));
    Files.createDirectories(dataDirectory.resolve("backups/maps"));
    List<String> resourcesToInstall = new java.util.ArrayList<>();
    for (ConfigurationId id : ConfigurationId.values()) resourcesToInstall.add(id.fileName());
    resourcesToInstall.add("languages/fr_FR.yml");
    resourcesToInstall.add("languages/en_US.yml");
    int created = 0;
    for (String resource : resourcesToInstall) {
      Path destination = dataDirectory.resolve(resource);
      if (Files.exists(destination)) continue;
      Files.createDirectories(destination.getParent());
      try (InputStream input = resources.open(resource)) {
        if (input == null) throw new IOException("Missing embedded resource: " + resource);
        Files.copy(input, destination);
      }
      created++;
      logger.info("Created default configuration: " + destination);
    }
    return created;
  }
}
