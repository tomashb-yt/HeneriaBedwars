package fr.heneria.zombie.plugin.editor;

import fr.heneria.zombie.core.editor.MapDefinition;
import fr.heneria.zombie.core.editor.MapObjectType;
import fr.heneria.zombie.core.editor.MapPersistence;
import fr.heneria.zombie.core.editor.MapPoint;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import org.bukkit.configuration.file.YamlConfiguration;

/** Versioned YAML persistence with backup and atomic replacement on the bounded I/O pool. */
public final class YamlMapPersistence implements MapPersistence {
  private static final String FILE = "map.yml";
  private final Path root;
  private final Executor ioExecutor;
  private final java.util.concurrent.ConcurrentHashMap<String, CompletableFuture<Void>> writes =
      new java.util.concurrent.ConcurrentHashMap<>();

  public YamlMapPersistence(Path dataDirectory, Executor ioExecutor) {
    root = dataDirectory.resolve("maps").toAbsolutePath().normalize();
    this.ioExecutor = java.util.Objects.requireNonNull(ioExecutor, "ioExecutor");
  }

  @Override
  public CompletableFuture<Collection<MapDefinition>> loadAll() {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            Files.createDirectories(root);
            List<MapDefinition> loaded = new ArrayList<>();
            try (var directories = Files.list(root)) {
              for (Path directory :
                  directories
                      .filter(path -> Files.isDirectory(path) && !Files.isSymbolicLink(path))
                      .toList()) {
                Path file = directory.resolve(FILE);
                if (Files.isRegularFile(file)) {
                  loaded.add(deserialize(file));
                }
              }
            }
            return List.copyOf(loaded);
          } catch (IOException | RuntimeException failure) {
            throw new CompletionException(failure);
          }
        },
        ioExecutor);
  }

  @Override
  public CompletableFuture<Void> save(MapDefinition definition) {
    CompletableFuture<Void> next;
    synchronized (writes) {
      CompletableFuture<Void> previous = writes.get(definition.id());
      CompletableFuture<Void> prerequisite =
          previous == null
              ? CompletableFuture.completedFuture(null)
              : previous.handle((ignored, failure) -> null);
      next = prerequisite.thenRunAsync(() -> writeAtomically(definition), ioExecutor);
      writes.put(definition.id(), next);
    }
    CompletableFuture<Void> completed = next;
    next.whenComplete((ignored, failure) -> writes.remove(definition.id(), completed));
    return next;
  }

  public Path rootDirectory() {
    return root;
  }

  private Path safeDirectory(String id) {
    if (!MapDefinition.safeId(id)) {
      throw new IllegalArgumentException("Unsafe map id");
    }
    Path directory = root.resolve(id).normalize();
    if (!directory.startsWith(root)) {
      throw new IllegalArgumentException("Map path escapes registry root");
    }
    return directory;
  }

  private void writeAtomically(MapDefinition definition) {
    try {
      Path directory = safeDirectory(definition.id());
      Files.createDirectories(directory);
      Path target = directory.resolve(FILE);
      Path temporary = directory.resolve(FILE + ".tmp");
      Path backup = directory.resolve(FILE + ".bak");
      serialize(definition).save(temporary.toFile());
      if (Files.exists(target)) {
        Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING);
      }
      try {
        Files.move(
            temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException | RuntimeException failure) {
      throw new CompletionException(failure);
    }
  }

  static YamlConfiguration serialize(MapDefinition definition) {
    YamlConfiguration yaml = new YamlConfiguration();
    yaml.set("schema-version", definition.schemaVersion());
    yaml.set("id", definition.id());
    yaml.set("name", definition.displayName());
    yaml.set("description", definition.description());
    yaml.set("creator", definition.creator().toString());
    yaml.set("created-at", definition.createdAt().toString());
    yaml.set("updated-at", definition.updatedAt().toString());
    yaml.set("world", definition.world());
    yaml.set("icon", definition.icon());
    yaml.set("image", definition.image());
    yaml.set("minimum-players", definition.minimumPlayers());
    yaml.set("maximum-players", definition.maximumPlayers());
    yaml.set("music", definition.music());
    yaml.set("difficulty", definition.difficulty());
    yaml.set("game-mode", definition.gameMode());
    definition.playerSpawn().ifPresent(point -> yaml.set("player-spawn", point(point)));
    yaml.set(
        "zones",
        definition.zones().values().stream()
            .map(
                zone -> {
                  Map<String, Object> value = base(zone.id(), zone.name(), zone.anchor());
                  value.put("color", zone.color());
                  value.put("music", zone.music());
                  value.put("ambience", zone.ambience());
                  value.put("volume", zone.volume());
                  return value;
                })
            .toList());
    yaml.set(
        "doors",
        definition.doors().values().stream()
            .map(
                door -> {
                  Map<String, Object> value = base(door.id(), door.name(), door.position());
                  value.put("price", door.price());
                  value.put("source-zone", door.sourceZone());
                  value.put("target-zone", door.targetZone());
                  value.put("type", door.type());
                  value.put("power-required", door.powerRequired());
                  value.put("key", door.key());
                  value.put("animation", door.animation());
                  value.put("sound", door.sound());
                  return value;
                })
            .toList());
    yaml.set(
        "zombie-spawns",
        definition.zombieSpawns().values().stream()
            .map(
                spawn -> {
                  Map<String, Object> value = base(spawn.id(), spawn.name(), spawn.position());
                  value.put("zone", spawn.zone());
                  value.put("weight", spawn.weight());
                  value.put("capacity", spawn.capacity());
                  value.put("minimum-round", spawn.minimumRound());
                  value.put("maximum-round", spawn.maximumRound());
                  value.put("minimum-distance", spawn.minimumDistance());
                  value.put("maximum-distance", spawn.maximumDistance());
                  value.put("visible", spawn.visible());
                  value.put("allowed-types", List.copyOf(spawn.allowedTypes()));
                  value.put("cooldown-ticks", spawn.cooldownTicks());
                  return value;
                })
            .toList());
    yaml.set(
        "objects",
        definition.objects().values().stream()
            .map(
                object -> {
                  Map<String, Object> value = base(object.id(), object.name(), object.position());
                  value.put("type", object.type().name());
                  value.put("zone", object.zone());
                  value.put("properties", object.properties());
                  return value;
                })
            .toList());
    return yaml;
  }

  static MapDefinition deserialize(Path file) {
    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
    if (yaml.getInt("schema-version") != MapDefinition.CURRENT_SCHEMA) {
      throw new IllegalArgumentException("Unsupported map schema in " + file);
    }
    Map<String, MapDefinition.Zone> zones = new LinkedHashMap<>();
    for (Map<?, ?> value : yaml.getMapList("zones")) {
      String id = text(value, "id");
      zones.put(
          id,
          new MapDefinition.Zone(
              id,
              text(value, "name"),
              text(value, "color"),
              text(value, "music"),
              text(value, "ambience"),
              number(value, "volume").doubleValue(),
              point(value)));
    }
    Map<String, MapDefinition.Door> doors = new LinkedHashMap<>();
    for (Map<?, ?> value : yaml.getMapList("doors")) {
      String id = text(value, "id");
      doors.put(
          id,
          new MapDefinition.Door(
              id,
              text(value, "name"),
              number(value, "price").intValue(),
              text(value, "source-zone"),
              text(value, "target-zone"),
              text(value, "type"),
              bool(value, "power-required"),
              text(value, "key"),
              text(value, "animation"),
              text(value, "sound"),
              point(value)));
    }
    Map<String, MapDefinition.ZombieSpawn> spawns = new LinkedHashMap<>();
    for (Map<?, ?> value : yaml.getMapList("zombie-spawns")) {
      String id = text(value, "id");
      Object types = value.get("allowed-types");
      Set<String> allowed =
          types instanceof Collection<?> collection
              ? collection.stream()
                  .map(Object::toString)
                  .collect(java.util.stream.Collectors.toSet())
              : Set.of();
      spawns.put(
          id,
          new MapDefinition.ZombieSpawn(
              id,
              text(value, "name"),
              text(value, "zone"),
              number(value, "weight").doubleValue(),
              number(value, "capacity").intValue(),
              number(value, "minimum-round").intValue(),
              number(value, "maximum-round").intValue(),
              number(value, "minimum-distance").doubleValue(),
              number(value, "maximum-distance").doubleValue(),
              bool(value, "visible"),
              allowed,
              number(value, "cooldown-ticks").intValue(),
              point(value)));
    }
    Map<String, MapDefinition.MapObject> objects = new LinkedHashMap<>();
    for (Map<?, ?> value : yaml.getMapList("objects")) {
      String id = text(value, "id");
      Map<String, String> properties = new LinkedHashMap<>();
      Object raw = value.get("properties");
      if (raw instanceof Map<?, ?> propertyMap) {
        propertyMap.forEach((key, property) -> properties.put(key.toString(), property.toString()));
      }
      objects.put(
          id,
          new MapDefinition.MapObject(
              id,
              MapObjectType.valueOf(text(value, "type")),
              text(value, "name"),
              text(value, "zone"),
              point(value),
              properties));
    }
    return new MapDefinition(
        yaml.getInt("schema-version"),
        yaml.getString("id", ""),
        yaml.getString("name", ""),
        yaml.getString("description", ""),
        UUID.fromString(yaml.getString("creator", "")),
        Instant.parse(yaml.getString("created-at", "")),
        Instant.parse(yaml.getString("updated-at", "")),
        yaml.getString("world", ""),
        yaml.getString("icon", "FILLED_MAP"),
        yaml.getString("image", ""),
        yaml.getInt("minimum-players", 1),
        yaml.getInt("maximum-players"),
        yaml.getString("music", ""),
        yaml.getString("difficulty", "NORMAL"),
        yaml.getString("game-mode", "CLASSIC"),
        yaml.isConfigurationSection("player-spawn")
            ? Optional.of(
                new MapPoint(
                    yaml.getString("player-spawn.world", ""),
                    yaml.getDouble("player-spawn.x"),
                    yaml.getDouble("player-spawn.y"),
                    yaml.getDouble("player-spawn.z"),
                    (float) yaml.getDouble("player-spawn.yaw"),
                    (float) yaml.getDouble("player-spawn.pitch")))
            : Optional.empty(),
        zones,
        doors,
        spawns,
        objects);
  }

  private static Map<String, Object> base(String id, String name, MapPoint point) {
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("id", id);
    value.put("name", name);
    value.putAll(point(point));
    return value;
  }

  private static Map<String, Object> point(MapPoint point) {
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("world", point.world());
    value.put("x", point.x());
    value.put("y", point.y());
    value.put("z", point.z());
    value.put("yaw", point.yaw());
    value.put("pitch", point.pitch());
    return value;
  }

  private static MapPoint point(Map<?, ?> value) {
    return new MapPoint(
        text(value, "world"),
        number(value, "x").doubleValue(),
        number(value, "y").doubleValue(),
        number(value, "z").doubleValue(),
        number(value, "yaw").floatValue(),
        number(value, "pitch").floatValue());
  }

  private static String text(Map<?, ?> map, String key) {
    Object value = map.get(key);
    return value == null ? "" : value.toString();
  }

  private static Number number(Map<?, ?> map, String key) {
    Object value = map.get(key);
    if (value instanceof Number number) {
      return number;
    }
    throw new IllegalArgumentException("Missing number " + key);
  }

  private static boolean bool(Map<?, ?> map, String key) {
    Object value = map.get(key);
    return value instanceof Boolean bool && bool;
  }
}
