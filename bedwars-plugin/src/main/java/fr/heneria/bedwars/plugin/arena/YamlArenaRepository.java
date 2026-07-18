package fr.heneria.bedwars.plugin.arena;

import fr.heneria.bedwars.core.arena.ArenaBoundary;
import fr.heneria.bedwars.core.arena.ArenaDefinition;
import fr.heneria.bedwars.core.arena.ArenaGeneratorDefinition;
import fr.heneria.bedwars.core.arena.ArenaId;
import fr.heneria.bedwars.core.arena.ArenaLoadResult;
import fr.heneria.bedwars.core.arena.ArenaLocation;
import fr.heneria.bedwars.core.arena.ArenaMetadata;
import fr.heneria.bedwars.core.arena.ArenaRepository;
import fr.heneria.bedwars.core.arena.ArenaStatus;
import fr.heneria.bedwars.core.arena.ArenaTeamDefinition;
import fr.heneria.bedwars.core.arena.ArenaVector;
import fr.heneria.bedwars.core.arena.TeamColor;
import fr.heneria.bedwars.core.arena.TeamId;
import fr.heneria.bedwars.core.game.generator.GeneratorId;
import fr.heneria.bedwars.core.game.generator.GeneratorResource;
import fr.heneria.bedwars.core.game.generator.GeneratorStackingStrategy;
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
        Math.max(1, yaml.getLong("revision", 1)),
        id,
        yaml.getString("display-name", id.value()),
        status(yaml.getString("status")),
        optional(yaml.getString("world")),
        optional(yaml.getString("map.template-id", yaml.getString("template"))),
        yaml.getString("environment", "NORMAL"),
        yaml.getInt("players.minimum", 0),
        yaml.getInt("players.maximum", 0),
        yaml.getInt("teams.count", 0),
        yaml.getInt("teams.players-per-team", 0),
        location(yaml.getConfigurationSection("locations.waiting")),
        location(yaml.getConfigurationSection("locations.spectator")),
        boundary(yaml.getConfigurationSection("boundary")),
        new ArenaMetadata(created, updated, attributes),
        teams(yaml.getConfigurationSection("teams.definitions")),
        generators(yaml.getConfigurationSection("generators.definitions")));
  }

  private static String serialize(ArenaDefinition arena) {
    YamlConfiguration yaml = new YamlConfiguration();
    yaml.set("config-version", arena.configVersion());
    yaml.set("revision", arena.revision());
    yaml.set("id", arena.id().value());
    yaml.set("display-name", arena.displayName());
    yaml.set("status", arena.status().name());
    yaml.set("enabled", arena.enabled());
    yaml.set("world", arena.worldName().orElse(null));
    yaml.set("map.template-id", arena.template().orElse(null));
    yaml.set("environment", arena.environment());
    yaml.set("players.minimum", arena.minimumPlayers());
    yaml.set("players.maximum", arena.maximumPlayers());
    yaml.set("teams.count", arena.teamCount());
    yaml.set("teams.players-per-team", arena.playersPerTeam());
    for (ArenaTeamDefinition team : arena.teams()) {
      String prefix = "teams.definitions." + team.id().value();
      yaml.set(prefix + ".display-name", team.displayName());
      yaml.set(prefix + ".color", team.color().name());
      yaml.set(prefix + ".order", team.order());
      yaml.set(prefix + ".capacity", team.capacity());
      team.spawn().ifPresent(location -> setLocation(yaml, prefix + ".spawn", location));
      team.bedLocation().ifPresent(location -> setLocation(yaml, prefix + ".bed", location));
      team.shopLocation().ifPresent(location -> setLocation(yaml, prefix + ".shop", location));
      team.upgradeShopLocation()
          .ifPresent(location -> setLocation(yaml, prefix + ".upgrade-shop", location));
      team.metadata().forEach((key, value) -> yaml.set(prefix + ".metadata." + key, value));
    }
    for (ArenaGeneratorDefinition generator : arena.generators()) {
      String prefix = "generators.definitions." + generator.id().value();
      yaml.set(prefix + ".resource", generator.resource().name());
      setLocation(yaml, prefix + ".location", generator.location());
      yaml.set(prefix + ".level", generator.level());
      yaml.set(prefix + ".interval-ticks", generator.intervalTicks());
      yaml.set(prefix + ".amount", generator.amountPerEmission());
      yaml.set(prefix + ".local-capacity", generator.localCapacity());
      yaml.set(prefix + ".stacking", generator.stackingStrategy().name());
    }
    arena.waitingLocation().ifPresent(location -> setLocation(yaml, "locations.waiting", location));
    arena
        .spectatorLocation()
        .ifPresent(location -> setLocation(yaml, "locations.spectator", location));
    arena
        .boundary()
        .ifPresent(
            boundary -> {
              yaml.set("boundary.enabled", boundary.enabled());
              boundary.minimum().ifPresent(value -> setVector(yaml, "boundary.minimum", value));
              boundary.maximum().ifPresent(value -> setVector(yaml, "boundary.maximum", value));
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

  private static List<ArenaTeamDefinition> teams(ConfigurationSection section) {
    if (section == null) return List.of();
    List<ArenaTeamDefinition> teams = new ArrayList<>();
    for (String rawId : section.getKeys(false)) {
      ConfigurationSection value = section.getConfigurationSection(rawId);
      if (value == null) throw new IllegalArgumentException("Invalid team " + rawId);
      Map<String, String> metadata = new LinkedHashMap<>();
      ConfigurationSection metadataSection = value.getConfigurationSection("metadata");
      if (metadataSection != null)
        metadataSection
            .getKeys(false)
            .forEach(key -> metadata.put(key, String.valueOf(metadataSection.get(key))));
      teams.add(
          new ArenaTeamDefinition(
              new TeamId(rawId),
              requiredString(value, "display-name"),
              TeamColor.valueOf(requiredString(value, "color").toUpperCase(java.util.Locale.ROOT)),
              value.getInt("order"),
              value.getInt("capacity"),
              location(value.getConfigurationSection("spawn")),
              location(value.getConfigurationSection("bed")),
              location(value.getConfigurationSection("shop")),
              location(value.getConfigurationSection("upgrade-shop")),
              metadata));
    }
    return List.copyOf(teams);
  }

  private static List<ArenaGeneratorDefinition> generators(ConfigurationSection section) {
    if (section == null) return List.of();
    List<ArenaGeneratorDefinition> generators = new ArrayList<>();
    for (String rawId : section.getKeys(false)) {
      ConfigurationSection value = section.getConfigurationSection(rawId);
      if (value == null) throw new IllegalArgumentException("Invalid generator " + rawId);
      generators.add(
          new ArenaGeneratorDefinition(
              new GeneratorId(rawId),
              GeneratorResource.valueOf(
                  requiredString(value, "resource").toUpperCase(java.util.Locale.ROOT)),
              location(value.getConfigurationSection("location"))
                  .orElseThrow(() -> new IllegalArgumentException("Missing generator location")),
              value.getInt("level", 1),
              value.getLong("interval-ticks"),
              value.getInt("amount", 1),
              value.getInt("local-capacity", 48),
              GeneratorStackingStrategy.valueOf(
                  value.getString("stacking", "MERGE_NEARBY").toUpperCase(java.util.Locale.ROOT))));
    }
    return List.copyOf(generators);
  }

  private static Optional<ArenaBoundary> boundary(ConfigurationSection section) {
    if (section == null) return Optional.empty();
    ConfigurationSection minimum = section.getConfigurationSection("minimum");
    ConfigurationSection maximum = section.getConfigurationSection("maximum");
    return Optional.of(
        new ArenaBoundary(
            section.getBoolean("enabled", true),
            minimum == null ? Optional.empty() : Optional.of(vector(minimum)),
            maximum == null ? Optional.empty() : Optional.of(vector(maximum))));
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
