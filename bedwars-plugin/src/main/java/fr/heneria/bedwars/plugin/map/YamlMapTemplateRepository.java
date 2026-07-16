package fr.heneria.bedwars.plugin.map;

import fr.heneria.bedwars.core.map.MapGeneratorType;
import fr.heneria.bedwars.core.map.MapId;
import fr.heneria.bedwars.core.map.MapSpawn;
import fr.heneria.bedwars.core.map.MapState;
import fr.heneria.bedwars.core.map.MapTemplate;
import fr.heneria.bedwars.core.map.MapTemplateLoadResult;
import fr.heneria.bedwars.core.map.MapTemplateRepository;
import fr.heneria.bedwars.core.map.MapType;
import fr.heneria.bedwars.core.map.MapWorldSettings;
import fr.heneria.bedwars.plugin.config.SafeYamlWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/** UTF-8 metadata repository with one atomic YAML file per validated map id. */
public final class YamlMapTemplateRepository implements MapTemplateRepository {
  private final Path metadataRoot;
  private final Clock clock;
  private final SafeYamlWriter writer = new SafeYamlWriter();

  public YamlMapTemplateRepository(Path metadataRoot, Clock clock) {
    this.metadataRoot = metadataRoot.toAbsolutePath().normalize();
    this.clock = clock;
  }

  @Override
  public MapTemplateLoadResult loadAll() throws IOException {
    Files.createDirectories(metadataRoot);
    List<MapTemplate> templates = new ArrayList<>();
    List<String> failures = new ArrayList<>();
    try (Stream<Path> files = Files.list(metadataRoot)) {
      for (Path file :
          files
              .filter(path -> path.getFileName().toString().endsWith(".yml"))
              .sorted(Comparator.comparing(Path::toString))
              .toList()) {
        try {
          MapTemplate template = read(file);
          if (!file.getFileName().toString().equals(template.id().value() + ".yml"))
            throw new IllegalArgumentException("Map id does not match metadata filename");
          templates.add(template);
        } catch (IOException | InvalidConfigurationException | RuntimeException exception) {
          failures.add(file.getFileName() + ": " + safeReason(exception));
        }
      }
    }
    return new MapTemplateLoadResult(templates, failures);
  }

  @Override
  public Optional<MapTemplate> load(MapId id) throws IOException {
    Path file = path(id);
    if (!Files.isRegularFile(file)) return Optional.empty();
    try {
      return Optional.of(read(file));
    } catch (InvalidConfigurationException | RuntimeException exception) {
      throw new IOException("Invalid map metadata for " + id, exception);
    }
  }

  @Override
  public void save(MapTemplate template) throws IOException {
    writer.write(path(template.id()), serialize(template));
  }

  @Override
  public void delete(MapId id) throws IOException {
    Files.deleteIfExists(path(id));
  }

  @Override
  public boolean exists(MapId id) {
    return Files.isRegularFile(path(id));
  }

  private MapTemplate read(Path file) throws IOException, InvalidConfigurationException {
    YamlConfiguration yaml = new YamlConfiguration();
    yaml.loadFromString(Files.readString(file, StandardCharsets.UTF_8));
    MapId id = MapId.parse(required(yaml.getString("id"), "id"));
    String world = required(yaml.getString("world-name"), "world-name");
    Instant created = instant(yaml.getString("timestamps.created-at"), clock.instant());
    Instant updated = instant(yaml.getString("timestamps.updated-at"), created);
    return new MapTemplate(
        yaml.getInt("config-version", 0),
        Math.max(1, yaml.getLong("revision", 1)),
        id,
        yaml.getString("display-name", id.value()),
        enumValue(MapType.class, yaml.getString("type"), MapType.GENERIC),
        MapState.UNLOADED,
        yaml.getString("folder-name", id.value()),
        world,
        yaml.getString("environment", "NORMAL"),
        enumValue(MapGeneratorType.class, yaml.getString("generator"), MapGeneratorType.VOID),
        new MapSpawn(
            yaml.getBoolean("spawn.configured", false),
            yaml.getString("spawn.world", world),
            yaml.getDouble("spawn.x", 0.5),
            yaml.getDouble("spawn.y", 65),
            yaml.getDouble("spawn.z", 0.5),
            (float) yaml.getDouble("spawn.yaw", 0),
            (float) yaml.getDouble("spawn.pitch", 0)),
        new MapWorldSettings(
            yaml.getBoolean("settings.auto-save", false),
            yaml.getBoolean("settings.allow-animals", false),
            yaml.getBoolean("settings.allow-monsters", false),
            yaml.getLong("settings.fixed-time", 6000),
            yaml.getBoolean("settings.clear-weather", true),
            yaml.getBoolean("settings.daylight-cycle", false),
            yaml.getBoolean("settings.weather-cycle", false),
            yaml.getString("settings.difficulty", "PEACEFUL"),
            yaml.getBoolean("settings.pvp", false),
            yaml.getBoolean("settings.fire-tick", false),
            yaml.getBoolean("settings.environmental-damage", false)),
        created,
        updated,
        optionalInstant(yaml.getString("timestamps.last-loaded-at")),
        optionalInstant(yaml.getString("timestamps.last-saved-at")),
        yaml.getString("metadata.author", ""),
        yaml.getString("metadata.description", ""),
        new LinkedHashSet<>(yaml.getStringList("metadata.tags")),
        new LinkedHashSet<>(yaml.getStringList("links.arenas")),
        false,
        Optional.empty());
  }

  private static String serialize(MapTemplate template) {
    YamlConfiguration yaml = new YamlConfiguration();
    yaml.set("config-version", template.configVersion());
    yaml.set("id", template.id().value());
    yaml.set("display-name", template.displayName());
    yaml.set("type", template.type().name());
    yaml.set("folder-name", template.folderName());
    yaml.set("world-name", template.worldName());
    yaml.set("environment", template.environment());
    yaml.set("generator", template.generatorType().name());
    yaml.set("state.expected-loaded", false);
    yaml.set("spawn.configured", template.spawn().configured());
    yaml.set("spawn.world", template.spawn().world());
    yaml.set("spawn.x", template.spawn().x());
    yaml.set("spawn.y", template.spawn().y());
    yaml.set("spawn.z", template.spawn().z());
    yaml.set("spawn.yaw", template.spawn().yaw());
    yaml.set("spawn.pitch", template.spawn().pitch());
    yaml.set("settings.auto-save", template.settings().autoSave());
    yaml.set("settings.allow-animals", template.settings().allowAnimals());
    yaml.set("settings.allow-monsters", template.settings().allowMonsters());
    yaml.set("settings.fixed-time", template.settings().fixedTime());
    yaml.set("settings.clear-weather", template.settings().clearWeather());
    yaml.set("settings.daylight-cycle", template.settings().daylightCycle());
    yaml.set("settings.weather-cycle", template.settings().weatherCycle());
    yaml.set("settings.difficulty", template.settings().difficulty());
    yaml.set("settings.pvp", template.settings().pvp());
    yaml.set("settings.fire-tick", template.settings().fireTick());
    yaml.set("settings.environmental-damage", template.settings().environmentalDamage());
    yaml.set("metadata.author", template.author());
    yaml.set("metadata.description", template.description());
    yaml.set("metadata.tags", template.tags().stream().sorted().toList());
    yaml.set("links.arenas", template.linkedArenaIds().stream().sorted().toList());
    yaml.set("revision", template.revision());
    yaml.set("timestamps.created-at", template.createdAt().toString());
    yaml.set("timestamps.updated-at", template.updatedAt().toString());
    yaml.set(
        "timestamps.last-loaded-at", template.lastLoadedAt().map(Instant::toString).orElse(null));
    yaml.set(
        "timestamps.last-saved-at", template.lastSavedAt().map(Instant::toString).orElse(null));
    return yaml.saveToString();
  }

  private Path path(MapId id) {
    Path candidate = metadataRoot.resolve(id.value() + ".yml").normalize();
    if (!candidate.startsWith(metadataRoot)) throw new IllegalArgumentException("Unsafe map path");
    return candidate;
  }

  private static <T extends Enum<T>> T enumValue(Class<T> type, String value, T fallback) {
    try {
      return value == null
          ? fallback
          : Enum.valueOf(type, value.toUpperCase(java.util.Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      return fallback;
    }
  }

  private static String required(String value, String name) {
    if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing " + name);
    return value;
  }

  private static Instant instant(String value, Instant fallback) {
    try {
      return value == null ? fallback : Instant.parse(value);
    } catch (RuntimeException exception) {
      return fallback;
    }
  }

  private static Optional<Instant> optionalInstant(String value) {
    try {
      return value == null ? Optional.empty() : Optional.of(Instant.parse(value));
    } catch (RuntimeException exception) {
      return Optional.empty();
    }
  }

  private static String safeReason(Exception exception) {
    return exception.getClass().getSimpleName()
        + (exception.getMessage() == null ? "" : " (" + exception.getMessage() + ")");
  }
}
