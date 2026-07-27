package fr.heneria.zombie.plugin.editor;

import fr.heneria.zombie.core.editor.MapDefinition;
import fr.heneria.zombie.core.editor.MapPublication;
import fr.heneria.zombie.core.editor.MapPublicationPersistence;
import fr.heneria.zombie.core.editor.MapStatus;
import fr.heneria.zombie.core.editor.PublishedMapVersion;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import org.bukkit.configuration.file.YamlConfiguration;

/** YAML adapter storing publication manifests and immutable definition snapshots off-thread. */
public final class YamlMapPublicationPersistence implements MapPublicationPersistence {
  private static final String MANIFEST = "publication.yml";
  private final Path root;
  private final Path templates;
  private final Executor ioExecutor;

  public YamlMapPublicationPersistence(
      Path dataDirectory, Path templateDirectory, Executor ioExecutor) {
    this.root = dataDirectory.resolve("maps").toAbsolutePath().normalize();
    this.templates = templateDirectory.toAbsolutePath().normalize();
    this.ioExecutor = java.util.Objects.requireNonNull(ioExecutor, "ioExecutor");
  }

  @Override
  public CompletableFuture<Collection<MapPublication>> loadAll() {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            Files.createDirectories(root);
            List<MapPublication> result = new ArrayList<>();
            try (var directories = Files.list(root)) {
              for (Path directory :
                  directories
                      .filter(path -> Files.isDirectory(path) && !Files.isSymbolicLink(path))
                      .toList()) {
                Path manifest = directory.resolve(MANIFEST);
                if (Files.isRegularFile(manifest)) {
                  result.add(read(manifest));
                }
              }
            }
            return List.copyOf(result);
          } catch (IOException | RuntimeException failure) {
            throw new CompletionException(failure);
          }
        },
        ioExecutor);
  }

  @Override
  public CompletableFuture<Void> save(MapPublication publication) {
    return CompletableFuture.runAsync(() -> write(publication), ioExecutor);
  }

  private MapPublication read(Path manifest) {
    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(manifest.toFile());
    if (yaml.getInt("schema-version") != 1) {
      throw new IllegalArgumentException("Unsupported publication schema in " + manifest);
    }
    String mapId = yaml.getString("map-id", "");
    Path directory = safeDirectory(mapId);
    List<PublishedMapVersion> versions = new ArrayList<>();
    for (var section : yaml.getMapList("versions")) {
      int number = ((Number) section.get("version")).intValue();
      Path snapshot = directory.resolve("versions").resolve("v" + number + ".yml");
      if (!Files.isRegularFile(snapshot)) {
        throw new IllegalArgumentException("Missing published snapshot " + snapshot);
      }
      versions.add(
          new PublishedMapVersion(
              number,
              YamlMapPersistence.deserialize(snapshot),
              Instant.parse(section.get("published-at").toString()),
              UUID.fromString(section.get("published-by").toString()),
              number(section, "restored-from")));
    }
    int active = yaml.getInt("active-version", 0);
    return new MapPublication(
        mapId,
        MapStatus.valueOf(yaml.getString("status", MapStatus.DRAFT.name())),
        active > 0 ? Optional.of(active) : Optional.empty(),
        versions);
  }

  private void write(MapPublication publication) {
    try {
      Path directory = safeDirectory(publication.mapId());
      Path versions = directory.resolve("versions");
      Path worldVersions = directory.resolve("world-versions");
      Files.createDirectories(versions);
      Files.createDirectories(worldVersions);
      for (PublishedMapVersion version : publication.versions()) {
        Path snapshot = versions.resolve("v" + version.version() + ".yml");
        Path worldSnapshot = worldVersions.resolve("v" + version.version());
        if (!Files.exists(snapshot)) {
          Path temporary = versions.resolve("v" + version.version() + ".yml.tmp");
          YamlMapPersistence.serialize(version.definition()).save(temporary.toFile());
          move(temporary, snapshot);
        }
        if (!Files.isDirectory(worldSnapshot)) {
          Path source =
              version
                  .restoredFrom()
                  .map(value -> worldVersions.resolve("v" + value))
                  .orElseGet(() -> safeTemplate(publication.mapId()));
          WorldDirectoryCopier.replace(source, worldSnapshot);
        }
        if (version.version() == publication.activeVersion().orElse(0)
            && version.restoredFrom().isPresent()) {
          WorldDirectoryCopier.replace(worldSnapshot, safeTemplate(publication.mapId()));
        }
      }
      YamlConfiguration yaml = new YamlConfiguration();
      yaml.set("schema-version", 1);
      yaml.set("map-id", publication.mapId());
      yaml.set("status", publication.status().name());
      yaml.set("active-version", publication.activeVersion().orElse(0));
      yaml.set(
          "versions",
          publication.versions().stream()
              .map(
                  version -> {
                    java.util.Map<String, Object> value = new java.util.LinkedHashMap<>();
                    value.put("version", version.version());
                    value.put("published-at", version.publishedAt().toString());
                    value.put("published-by", version.publishedBy().toString());
                    version.restoredFrom().ifPresent(source -> value.put("restored-from", source));
                    return value;
                  })
              .toList());
      Path target = directory.resolve(MANIFEST);
      Path temporary = directory.resolve(MANIFEST + ".tmp");
      yaml.save(temporary.toFile());
      move(temporary, target);
    } catch (IOException | RuntimeException failure) {
      throw new CompletionException(failure);
    }
  }

  private Path safeDirectory(String mapId) {
    if (!MapDefinition.safeId(mapId)) {
      throw new IllegalArgumentException("Unsafe map id");
    }
    Path directory = root.resolve(mapId).normalize();
    if (!directory.startsWith(root)) {
      throw new IllegalArgumentException("Publication path escapes registry root");
    }
    return directory;
  }

  private Path safeTemplate(String mapId) {
    if (!MapDefinition.safeId(mapId)) {
      throw new IllegalArgumentException("Unsafe map id");
    }
    Path directory = templates.resolve(mapId).normalize();
    if (!directory.startsWith(templates)) {
      throw new IllegalArgumentException("Template path escapes root");
    }
    return directory;
  }

  private static Optional<Integer> number(java.util.Map<?, ?> value, String key) {
    Object number = value.get(key);
    return number instanceof Number typed ? Optional.of(typed.intValue()) : Optional.empty();
  }

  private static void move(Path source, Path target) throws IOException {
    try {
      Files.move(
          source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
      Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
