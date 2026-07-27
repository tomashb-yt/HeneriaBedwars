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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.configuration.file.YamlConfiguration;

/** Secure, asynchronous catalog for minimal world-template metadata. */
public final class MapTemplateCatalog {

  private static final String METADATA_FILE = "zombie-map.yml";
  private final Path worldContainer;
  private final ConfigurationManager configurations;
  private final Executor ioExecutor;
  private final AtomicReference<Set<String>> knownTemplates = new AtomicReference<>(Set.of());

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

  /**
   * Loads technical metadata from an immutable published world snapshot.
   *
   * @param mapId logical identifier
   * @param source published source directory
   * @return future optional definition
   */
  public CompletableFuture<Optional<MapTemplateDefinition>> findPublished(
      String mapId, Path source) {
    if (mapId == null || !mapId.matches("[a-z0-9][a-z0-9_-]{0,63}") || source == null) {
      return CompletableFuture.completedFuture(Optional.empty());
    }
    Path normalized = source.toAbsolutePath().normalize();
    return CompletableFuture.supplyAsync(() -> load(mapId, normalized), ioExecutor);
  }

  /** Refreshes the non-authoritative map snapshot asynchronously. */
  public void refreshCount() {
    discover().exceptionally(ignored -> java.util.List.of());
  }

  /**
   * Discovers every direct child containing a valid {@code level.dat}.
   *
   * @return future sorted map identifiers
   */
  public CompletableFuture<java.util.List<String>> discover() {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            Path root = templateRoot();
            Files.createDirectories(root);
            try (var entries = Files.list(root)) {
              Set<String> discovered =
                  entries
                      .filter(path -> Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
                      .filter(path -> !Files.isSymbolicLink(path))
                      .filter(
                          path -> path.getFileName().toString().matches("[a-z0-9][a-z0-9_-]{0,63}"))
                      .filter(
                          path ->
                              Files.isRegularFile(
                                  path.resolve("level.dat"), LinkOption.NOFOLLOW_LINKS))
                      .map(path -> path.getFileName().toString())
                      .collect(java.util.stream.Collectors.toUnmodifiableSet());
              knownTemplates.set(discovered);
              return discovered.stream().sorted().toList();
            }
          } catch (IOException failure) {
            throw new CompletionException(failure);
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
    return knownTemplates.get().size();
  }

  /**
   * Returns the last asynchronously discovered identifiers for tab completion.
   *
   * @return immutable identifiers
   */
  public Set<String> knownMapIds() {
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
    return load(mapId, sourceDirectory(mapId));
  }

  private Optional<MapTemplateDefinition> load(String mapId, Path directory) {
    try {
      Files.createDirectories(templateRoot());
    } catch (IOException failure) {
      throw new CompletionException(
          new IOException("Could not create template directory " + templateRoot(), failure));
    }
    Path metadata = directory.resolve(METADATA_FILE);
    Path levelDat = directory.resolve("level.dat");
    if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)
        || Files.isSymbolicLink(directory)
        || !Files.isRegularFile(levelDat, LinkOption.NOFOLLOW_LINKS)
        || Files.isSymbolicLink(levelDat)) {
      return Optional.empty();
    }
    if (!Files.exists(metadata, LinkOption.NOFOLLOW_LINKS)) {
      try {
        MapTemplateDefinition definition =
            new MapTemplateDefinition(
                mapId,
                configurations.current().settings().instances().defaultMapMaximumPlayers(),
                LevelDatSpawnReader.read(levelDat));
        knownTemplates.updateAndGet(
            current -> {
              java.util.HashSet<String> updated = new java.util.HashSet<>(current);
              updated.add(mapId);
              return Set.copyOf(updated);
            });
        return Optional.of(definition);
      } catch (IOException failure) {
        throw new CompletionException(
            new IOException("Could not read world spawn from " + levelDat, failure));
      }
    }
    if (!Files.isRegularFile(metadata, LinkOption.NOFOLLOW_LINKS)
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
