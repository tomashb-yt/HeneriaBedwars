package fr.heneria.zombie.plugin.enemy;

import fr.heneria.zombie.core.enemy.ZombieDamageType;
import fr.heneria.zombie.core.enemy.ZombieDefinition;
import fr.heneria.zombie.core.enemy.ZombieDefinition.BehaviorType;
import fr.heneria.zombie.core.enemy.ZombieDefinition.NavigationMode;
import fr.heneria.zombie.core.enemy.ZombieDefinition.StuckAction;
import fr.heneria.zombie.core.enemy.ZombieDefinition.TargetStrategy;
import fr.heneria.zombie.core.enemy.ZombieDefinition.ZombieCategory;
import fr.heneria.zombie.core.enemy.ZombieDefinitionRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Loads versioned enemy YAML off-thread and atomically publishes only a complete valid registry.
 */
public final class ZombieDefinitionLoader {
  private static final List<String> BUNDLED =
      List.of("classic_zombie", "sprinter_zombie", "armored_zombie", "toxic_zombie");

  private final JavaPlugin plugin;
  private final Path directory;
  private final Executor ioExecutor;
  private final AtomicReference<ZombieDefinitionRegistry> current;

  public ZombieDefinitionLoader(JavaPlugin plugin, Executor ioExecutor) {
    this.plugin = plugin;
    this.directory = plugin.getDataFolder().toPath().resolve("zombies");
    this.ioExecutor = ioExecutor;
    this.current = new AtomicReference<>(loadBundled());
  }

  public ZombieDefinitionRegistry current() {
    return current.get();
  }

  public Path directory() {
    return directory;
  }

  public CompletableFuture<ZombieDefinitionRegistry> initializeAsync() {
    return reloadAsync(true);
  }

  public CompletableFuture<ZombieDefinitionRegistry> reloadAsync() {
    return reloadAsync(false);
  }

  private CompletableFuture<ZombieDefinitionRegistry> reloadAsync(boolean installDefaults) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            Files.createDirectories(directory);
            if (installDefaults) {
              installDefaults();
            }
            List<ZombieDefinition> definitions = new ArrayList<>();
            try (var files = Files.list(directory)) {
              for (Path file :
                  files
                      .filter(path -> path.getFileName().toString().endsWith(".yml"))
                      .sorted()
                      .toList()) {
                definitions.add(
                    parse(YamlConfiguration.loadConfiguration(file.toFile()), file.toString()));
              }
            }
            ZombieDefinitionRegistry candidate =
                definitions.isEmpty() ? loadBundled() : new ZombieDefinitionRegistry(definitions);
            current.set(candidate);
            return candidate;
          } catch (IOException | RuntimeException failure) {
            throw new IllegalStateException(
                "Zombie definitions rejected: " + failure.getMessage(), failure);
          }
        },
        ioExecutor);
  }

  private ZombieDefinitionRegistry loadBundled() {
    List<ZombieDefinition> definitions = new ArrayList<>();
    for (String id : BUNDLED) {
      String resource = "zombies/" + id + ".yml";
      try (InputStream input = plugin.getResource(resource)) {
        if (input == null) {
          throw new IllegalStateException("Missing bundled resource " + resource);
        }
        YamlConfiguration yaml =
            YamlConfiguration.loadConfiguration(
                new InputStreamReader(input, StandardCharsets.UTF_8));
        definitions.add(parse(yaml, resource));
      } catch (IOException failure) {
        throw new IllegalStateException("Cannot close bundled resource " + resource, failure);
      }
    }
    return new ZombieDefinitionRegistry(definitions);
  }

  private void installDefaults() throws IOException {
    for (String id : BUNDLED) {
      Path target = directory.resolve(id + ".yml");
      if (Files.exists(target)) {
        continue;
      }
      try (InputStream input = plugin.getResource("zombies/" + id + ".yml")) {
        if (input == null) {
          throw new IOException("Missing bundled zombie " + id);
        }
        Files.copy(input, target);
      }
    }
  }

  static ZombieDefinition parse(YamlConfiguration yaml, String source) {
    int schema = yaml.getInt("schema-version", -1);
    if (schema != 1) {
      throw invalid(source, "schema-version", "expected 1");
    }
    String id = required(yaml, "id", source);
    String entityType = required(yaml, "entity.type", source).toUpperCase(Locale.ROOT);
    EntityType type;
    try {
      type = EntityType.valueOf(entityType);
    } catch (IllegalArgumentException failure) {
      throw invalid(source, "entity.type", "unknown Bukkit entity type " + entityType);
    }
    if (!type.isAlive()
        || type.getEntityClass() == null
        || !org.bukkit.entity.Mob.class.isAssignableFrom(type.getEntityClass())) {
      throw invalid(source, "entity.type", "must be a spawnable Mob");
    }
    ZombieDefinition definition =
        new ZombieDefinition(
            id,
            yaml.getString("display-name", id),
            entityType,
            enumValue(
                ZombieCategory.class, yaml.getString("category", "NORMAL"), source, "category"),
            new ZombieDefinition.Attributes(
                yaml.getDouble("attributes.health.base", 20),
                yaml.getBoolean("attributes.health.use-round-scaling", true),
                yaml.getDouble("attributes.health.round-multiplier", 1.1),
                yaml.getDouble("attributes.health.maximum", 2048),
                yaml.getDouble("attributes.damage.base", 3),
                yaml.getBoolean("attributes.damage.use-round-scaling", false),
                yaml.getDouble("attributes.damage.round-multiplier", 1),
                yaml.getDouble("attributes.speed.base", 0.23),
                yaml.getDouble("attributes.follow-range", 40),
                yaml.getDouble("attributes.knockback-resistance", 0),
                yaml.getDouble("attributes.attack-knockback", 0)),
            new ZombieDefinition.Behavior(
                enumValue(
                    BehaviorType.class,
                    yaml.getString("behavior.type", "MELEE"),
                    source,
                    "behavior.type"),
                enumValue(
                    TargetStrategy.class,
                    yaml.getString("behavior.target-strategy", "NEAREST_VALID_PLAYER"),
                    source,
                    "behavior.target-strategy"),
                yaml.getDouble("behavior.attack-range", 1.8),
                yaml.getInt("behavior.attack-cooldown-ticks", 20),
                yaml.getInt("behavior.target-update-interval-ticks", 20),
                yaml.getDouble("behavior.lose-target-distance", 48)),
            new ZombieDefinition.Navigation(
                enumValue(
                    NavigationMode.class,
                    yaml.getString("navigation.mode", "GROUND"),
                    source,
                    "navigation.mode"),
                yaml.getBoolean("navigation.avoid-water", false),
                yaml.getBoolean("navigation.can-open-doors", false),
                yaml.getBoolean("navigation.can-break-doors", false),
                yaml.getBoolean("navigation.stuck-detection.enabled", true),
                yaml.getInt("navigation.stuck-detection.check-interval-ticks", 40),
                yaml.getDouble("navigation.stuck-detection.minimum-movement-distance", 0.35),
                yaml.getInt("navigation.stuck-detection.timeout-ticks", 160),
                yaml.getInt("navigation.stuck-detection.maximum-repath-attempts", 3),
                enumValue(
                    StuckAction.class,
                    yaml.getString(
                        "navigation.stuck-detection.fallback-action", "TELEPORT_TO_VALID_SPAWN"),
                    source,
                    "navigation.stuck-detection.fallback-action")),
            damage(yaml, source),
            new ZombieDefinition.SpawnRules(
                yaml.getInt("spawn-rules.minimum-round", 1),
                yaml.getInt("spawn-rules.maximum-round", Integer.MAX_VALUE),
                yaml.getInt("spawn-rules.weight", 100),
                yaml.getInt("spawn-rules.maximum-alive", -1),
                Set.copyOf(yaml.getStringList("spawn-rules.zones")),
                Set.copyOf(yaml.getStringList("spawn-rules.pools"))),
            new ZombieDefinition.Rewards(
                yaml.getInt("rewards.points-on-hit", 10),
                yaml.getInt("rewards.points-on-kill", 60),
                yaml.getInt("rewards.points-on-headshot-kill", 100),
                yaml.getInt("rewards.experience", 0)),
            List.copyOf(yaml.getStringList("abilities")),
            new ZombieDefinition.Environment(
                yaml.getBoolean("environment.burn-in-daylight", false),
                yaml.getBoolean("environment.remove-when-far-away", false)),
            new ZombieDefinition.Appearance(
                yaml.getBoolean("appearance.custom-name-visible", false),
                yaml.getBoolean("appearance.glowing", false),
                yaml.getBoolean("entity.baby", false),
                yaml.getBoolean("entity.silent", false),
                equipment(yaml)));
    List<String> issues = definition.validate();
    if (!issues.isEmpty()) {
      throw invalid(source, id, String.join("; ", issues));
    }
    return definition;
  }

  private static ZombieDefinition.DamageProfile damage(YamlConfiguration yaml, String source) {
    Set<ZombieDamageType> immunities = new HashSet<>();
    for (String value : yaml.getStringList("damage.immunities")) {
      immunities.add(enumValue(ZombieDamageType.class, value, source, "damage.immunities"));
    }
    Map<ZombieDamageType, Double> multipliers = new HashMap<>();
    ConfigurationSection section = yaml.getConfigurationSection("damage.multipliers");
    if (section != null) {
      for (String key : section.getKeys(false)) {
        multipliers.put(
            enumValue(ZombieDamageType.class, key, source, "damage.multipliers." + key),
            section.getDouble(key));
      }
    }
    return new ZombieDefinition.DamageProfile(
        immunities,
        multipliers,
        yaml.getBoolean("damage.headshot.enabled", true),
        yaml.getDouble("damage.headshot.multiplier", 2));
  }

  private static Map<String, String> equipment(YamlConfiguration yaml) {
    ConfigurationSection section = yaml.getConfigurationSection("appearance.equipment");
    if (section == null) {
      return Map.of();
    }
    Map<String, String> result = new HashMap<>();
    for (String slot : section.getKeys(false)) {
      String material = section.getString(slot + ".material");
      if (material != null && !material.isBlank()) {
        result.put(slot, material.toUpperCase(Locale.ROOT));
      }
    }
    return result;
  }

  private static String required(YamlConfiguration yaml, String path, String source) {
    String value = yaml.getString(path);
    if (value == null || value.isBlank()) {
      throw invalid(source, path, "required non-blank value");
    }
    return value;
  }

  private static <E extends Enum<E>> E enumValue(
      Class<E> type, String value, String source, String path) {
    try {
      return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException failure) {
      throw invalid(source, path, "unknown value " + value);
    }
  }

  private static IllegalArgumentException invalid(String source, String path, String expected) {
    return new IllegalArgumentException(source + " [" + path + "]: " + expected);
  }
}
