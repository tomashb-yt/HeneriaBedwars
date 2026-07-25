package fr.heneria.zombie.plugin.map;

import fr.heneria.zombie.core.map.MapTemplateDefinition;
import fr.heneria.zombie.core.map.MapTemplateDefinition.MapSpawn;
import fr.heneria.zombie.plugin.config.ConfigurationManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.configuration.file.YamlConfiguration;

/** Secure, asynchronous catalog for minimal world-template metadata. */
public final class MapTemplateCatalog {

  private static final String METADATA_FILE = "zombie-map.yml";
  private final Path worldContainer;
  private final ConfigurationManager configurations;
  private final Executor ioExecutor;
  private final AtomicInteger knownTemplates = new AtomicInteger();

  /**
   * Creates the catalog.
   *
   * @param worldContainer Paper world container
   * @param configurations active settings
   * @param ioExecutor file executor
   */
  public MapTemplateCatalog(
      Path worldContainer, ConfigurationManager configurations, Executor ioExecutor) {
    this.worldContainer =
        Objects.requireNonNull(worldContainer, "worldContainer").toAbsolutePath().normalize();
    this.configurations = Objects.requireNonNull(configurations, "configurations");
    this.ioExecutor = Objects.requireNonNull(ioExecutor, "ioExecutor");
  }

  /**
   * Loads a validated template definition.
   *
   * @param mapId map identifier
   * @return future optional definition
   */
  public CompletableFuture<Optional<MapTemplateDefinition>> find(String mapId) {
    if (mapId == null || !mapId.matches("[a-z0-9][a-z0-9_-]{0,63}")) {
      return CompletableFuture.completedFuture(Optional.empty());
    }
    return CompletableFuture.supplyAsync(() -> load(mapId), ioExecutor);
  }

  /** Refreshes the non-authoritative diagnostic count asynchronously. */
  public void refreshCount() {
    CompletableFuture.runAsync(
        () -> {
          try {
            Path root = templateRoot();
            Files.createDirectories(root);
            try (var entries = Files.list(root)) {
              knownTemplates.set(
                  Math.toIntExact(
                      entries
                          .filter(path -> Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
                          .filter(path -> Files.isRegularFile(path.resolve(METADATA_FILE)))
                          .count()));
            }
          } catch (IOException ignored) {
            knownTemplates.set(0);
          }
        },
        ioExecutor);
  }

  /**
   * Returns the last asynchronously observed template count.
   *
   * @return non-negative count
   */
  public int count() {
    return knownTemplates.get();
  }

  /**
   * Returns the configured absolute template root for diagnostics.
   *
   * @return template root
   */
  public Path rootDirectory() {
    return templateRoot();
  }

  /**
   * Resolves the secure source directory.
   *
   * @param mapId validated identifier
   * @return source directory
   */
  public Path sourceDirectory(String mapId) {
    Path root = templateRoot();
    Path candidate = root.resolve(mapId).normalize();
    if (!candidate.startsWith(root)) {
      throw new IllegalArgumentException("Template escapes configured root");
    }
    return candidate;
  }

  /**
   * Returns the expected metadata path for diagnostics.
   *
   * @param mapId validated map identifier
   * @return absolute metadata path
   */
  public Path metadataPath(String mapId) {
    return sourceDirectory(mapId).resolve(METADATA_FILE);
  }

  private Optional<MapTemplateDefinition> load(String mapId) {
    try {
      Files.createDirectories(templateRoot());
    } catch (IOException failure) {
      throw new CompletionException(
          new IOException("Could not create template directory " + templateRoot(), failure));
    }
    Path directory = sourceDirectory(mapId);
    Path metadata = directory.resolve(METADATA_FILE);
    if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)
        || Files.isSymbolicLink(directory)
        || !Files.isRegularFile(metadata, LinkOption.NOFOLLOW_LINKS)
        || Files.isSymbolicLink(metadata)) {
      return Optional.empty();
    }
    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(metadata.toFile());
    if (yaml.getInt("schema-version", 0) != 1 || !mapId.equals(yaml.getString("map-id", ""))) {
      return Optional.empty();
    }
    try {
      return Optional.of(
          new MapTemplateDefinition(
              mapId,
              yaml.getInt("maximum-players", 0),
              new MapSpawn(
                  yaml.getDouble("spawn.x"),
                  yaml.getDouble("spawn.y"),
                  yaml.getDouble("spawn.z"),
                  (float) yaml.getDouble("spawn.yaw"),
                  (float) yaml.getDouble("spawn.pitch"))));
    } catch (IllegalArgumentException invalid) {
      return Optional.empty();
    }
  }

  private Path templateRoot() {
    Path root =
        worldContainer
            .resolve(configurations.current().settings().instances().templatesDirectory())
            .normalize();
    if (!root.startsWith(worldContainer)) {
      throw new IllegalStateException("Template root escapes the world container");
    }
    return root;
  }
}

