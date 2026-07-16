package fr.heneria.bedwars.plugin.arena;

import fr.heneria.bedwars.core.arena.ArenaBoundary;
import fr.heneria.bedwars.core.arena.ArenaDefinition;
import fr.heneria.bedwars.core.arena.ArenaId;
import fr.heneria.bedwars.core.arena.ArenaLoadResult;
import fr.heneria.bedwars.core.arena.ArenaLocation;
import fr.heneria.bedwars.core.arena.ArenaMetadata;
import fr.heneria.bedwars.core.arena.ArenaRepository;
import fr.heneria.bedwars.core.arena.ArenaStatus;
import fr.heneria.bedwars.core.arena.ArenaVector;
import fr.heneria.bedwars.plugin.config.SafeYamlWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/** UTF-8 one-file-per-arena persistence with atomic publication and deletion backups. */
public final class YamlArenaRepository implements ArenaRepository {
  private final Path arenaDirectory;
  private final Path backupDirectory;
  private final Clock clock;
  private final SafeYamlWriter writer;

  public YamlArenaRepository(Path dataDirectory, Clock clock) {
    this(
        dataDirectory.resolve("arenas"),
        dataDirectory.resolve("backups/arenas"),
        clock,
        new SafeYamlWriter());
  }

  YamlArenaRepository(
      Path arenaDirectory, Path backupDirectory, Clock clock, SafeYamlWriter writer) {
    this.arenaDirectory = arenaDirectory;
    this.backupDirectory = backupDirectory;
    this.clock = clock;
    this.writer = writer;
  }

  @Override
  public ArenaLoadResult loadAll() throws IOException {
    Files.createDirectories(arenaDirectory);
    List<ArenaDefinition> definitions = new ArrayList<>();
    Map<ArenaId, String> failed = new LinkedHashMap<>();
    List<String> unreadable = new ArrayList<>();
    try (Stream<Path> stream = Files.list(arenaDirectory)) {
      for (Path file :
          stream
              .filter(path -> path.getFileName().toString().endsWith(".yml"))
              .sorted(Comparator.comparing(Path::toString))
              .toList()) {
        String filename = file.getFileName().toString();
        Optional<ArenaId> filenameId = idFromFilename(filename);
        try {
          ArenaDefinition arena = read(file);
          if (!filename.equals(arena.id().value() + ".yml"))
            throw new IllegalArgumentException("Arena id does not match filename");
          definitions.add(arena);
        } catch (IOException | InvalidConfigurationException | RuntimeException exception) {
          if (filenameId.isPresent()) failed.put(filenameId.orElseThrow(), safeReason(exception));
          else unreadable.add(filename + ": " + safeReason(exception));
        }
      }
    }
    return new ArenaLoadResult(definitions, failed, unreadable);
  }

  @Override
  public void save(ArenaDefinition arena) throws IOException {
    writer.write(path(arena.id()), serialize(arena));
  }

  @Override
  public synchronized void deleteWithBackup(ArenaId id) throws IOException {
    Path source = path(id);
    if (!Files.isRegularFile(source)) throw new IOException("Arena file does not exist");
    Path day = backupDirectory.resolve(LocalDate.now(clock).toString());
    Files.createDirectories(day);
    Path destination = day.resolve(id.value() + ".yml");
    int collision = 0;
    while (Files.exists(destination))
      destination = day.resolve(id.value() + "_" + ++collision + ".yml");
    Files.copy(source, destination, StandardCopyOption.COPY_ATTRIBUTES);
    Files.delete(source);
  }

  private ArenaDefinition read(Path file) throws IOException, InvalidConfigurationException {
    YamlConfiguration yaml = new YamlConfiguration();
    yaml.loadFromString(Files.readString(file, StandardCharsets.UTF_8));
    ArenaId id = new ArenaId(requiredString(yaml, "id"));
    Instant created = instant(yaml.getString("metadata.created-at"), clock.instant());
    Instant updated = instant(yaml.getString("metadata.updated-at"), created);
    Map<String, String> attributes = new LinkedHashMap<>();
    ConfigurationSection attributeSection = yaml.getConfigurationSection("metadata.attributes");
    if (attributeSection != null)
      attributeSection
          .getKeys(false)
          .forEach(key -> attributes.put(key, String.valueOf(attributeSection.get(key))));
    return new ArenaDefinition(
        yaml.getInt("config-version", 0),
        id,
        yaml.getString("display-name", id.value()),
        status(yaml.getString("status")),
        optional(yaml.getString("world")),
        optional(yaml.getString("template")),
        yaml.getString("environment", "NORMAL"),
        yaml.getInt("players.minimum", 0),
        yaml.getInt("players.maximum", 0),
        yaml.getInt("teams.count", 0),
        yaml.getInt("teams.players-per-team", 0),
        location(yaml.getConfigurationSection("locations.waiting")),
        location(yaml.getConfigurationSection("locations.spectator")),
        boundary(yaml.getConfigurationSection("boundary")),
        new ArenaMetadata(created, updated, attributes));
  }

  private static String serialize(ArenaDefinition arena) {
    YamlConfiguration yaml = new YamlConfiguration();
    yaml.set("config-version", arena.configVersion());
    yaml.set("id", arena.id().value());
    yaml.set("display-name", arena.displayName());
    yaml.set("status", arena.status().name());
    yaml.set("enabled", arena.enabled());
    yaml.set("world", arena.worldName().orElse(null));
    yaml.set("template", arena.template().orElse(null));
    yaml.set("environment", arena.environment());
    yaml.set("players.minimum", arena.minimumPlayers());
    yaml.set("players.maximum", arena.maximumPlayers());
    yaml.set("teams.count", arena.teamCount());
    yaml.set("teams.players-per-team", arena.playersPerTeam());
    arena.waitingLocation().ifPresent(location -> setLocation(yaml, "locations.waiting", location));
    arena
        .spectatorLocation()
        .ifPresent(location -> setLocation(yaml, "locations.spectator", location));
    arena
        .boundary()
        .ifPresent(
            boundary -> {
              setVector(yaml, "boundary.minimum", boundary.minimum());
              setVector(yaml, "boundary.maximum", boundary.maximum());
            });
    yaml.set("metadata.created-at", arena.metadata().createdAt().toString());
    yaml.set("metadata.updated-at", arena.metadata().updatedAt().toString());
    arena
        .metadata()
        .attributes()
        .forEach((key, value) -> yaml.set("metadata.attributes." + key, value));
    return yaml.saveToString();
  }

  private static void setLocation(YamlConfiguration yaml, String path, ArenaLocation location) {
    yaml.set(path + ".world", location.world());
    setVector(yaml, path, location.position());
    yaml.set(path + ".yaw", location.yaw());
    yaml.set(path + ".pitch", location.pitch());
  }

  private static void setVector(YamlConfiguration yaml, String path, ArenaVector vector) {
    yaml.set(path + ".x", vector.x());
    yaml.set(path + ".y", vector.y());
    yaml.set(path + ".z", vector.z());
  }

  private static Optional<ArenaLocation> location(ConfigurationSection section) {
    if (section == null) return Optional.empty();
    return Optional.of(
        new ArenaLocation(
            requiredString(section, "world"),
            vector(section),
            (float) section.getDouble("yaw"),
            (float) section.getDouble("pitch")));
  }

  private static Optional<ArenaBoundary> boundary(ConfigurationSection section) {
    if (section == null) return Optional.empty();
    ConfigurationSection minimum = section.getConfigurationSection("minimum");
    ConfigurationSection maximum = section.getConfigurationSection("maximum");
    if (minimum == null || maximum == null)
      throw new IllegalArgumentException("Incomplete boundary");
    return Optional.of(new ArenaBoundary(vector(minimum), vector(maximum)));
  }

  private static ArenaVector vector(ConfigurationSection section) {
    return new ArenaVector(section.getDouble("x"), section.getDouble("y"), section.getDouble("z"));
  }

  private static String requiredString(ConfigurationSection section, String path) {
    String value = section.getString(path);
    if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing " + path);
    return value;
  }

  private static ArenaStatus status(String value) {
    try {
      return value == null
          ? ArenaStatus.DRAFT
          : ArenaStatus.valueOf(value.toUpperCase(java.util.Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      return ArenaStatus.INVALID;
    }
  }

  private static Optional<String> optional(String value) {
    return Optional.ofNullable(value).map(String::trim).filter(text -> !text.isEmpty());
  }

  private static Instant instant(String value, Instant fallback) {
    try {
      return value == null ? fallback : Instant.parse(value);
    } catch (RuntimeException ignored) {
      return fallback;
    }
  }

  private static Optional<ArenaId> idFromFilename(String filename) {
    try {
      return Optional.of(new ArenaId(filename.substring(0, filename.length() - 4)));
    } catch (RuntimeException exception) {
      return Optional.empty();
    }
  }

  private Path path(ArenaId id) {
    return arenaDirectory.resolve(id.value() + ".yml");
  }

  private static String safeReason(Exception exception) {
    return exception.getClass().getSimpleName()
        + (exception.getMessage() == null ? "" : " (" + exception.getMessage() + ")");
  }
}
