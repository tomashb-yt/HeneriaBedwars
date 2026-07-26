package fr.heneria.zombie.plugin.weapon;

import fr.heneria.zombie.core.enemy.ZombieDamageType;
import fr.heneria.zombie.core.weapon.WeaponDefinition;
import fr.heneria.zombie.core.weapon.WeaponDefinition.FireMode;
import fr.heneria.zombie.core.weapon.WeaponDefinition.WeaponCategory;
import fr.heneria.zombie.core.weapon.WeaponDefinition.WeaponRarity;
import fr.heneria.zombie.core.weapon.WeaponRegistry;
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
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/** Installs, validates and atomically activates versioned weapon YAML definitions. */
public final class WeaponDefinitionLoader {
  private static final List<String> BUNDLED =
      List.of("starter_pistol", "ak47", "mp5", "pump_shotgun", "raygun");

  private final JavaPlugin plugin;
  private final Executor ioExecutor;
  private final Path directory;
  private final AtomicReference<WeaponRegistry> current;

  public WeaponDefinitionLoader(JavaPlugin plugin, Executor ioExecutor) {
    this.plugin = java.util.Objects.requireNonNull(plugin, "plugin");
    this.ioExecutor = java.util.Objects.requireNonNull(ioExecutor, "ioExecutor");
    directory = plugin.getDataFolder().toPath().resolve("weapons");
    current = new AtomicReference<>(loadBundled());
  }

  public WeaponRegistry current() {
    return current.get();
  }

  public Path directory() {
    return directory;
  }

  public CompletableFuture<WeaponRegistry> initializeAsync() {
    return reloadAsync(true);
  }

  public CompletableFuture<WeaponRegistry> reloadAsync() {
    return reloadAsync(false);
  }

  private CompletableFuture<WeaponRegistry> reloadAsync(boolean installDefaults) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            Files.createDirectories(directory);
            if (installDefaults) {
              installDefaults();
            }
            List<WeaponDefinition> loaded = new ArrayList<>();
            try (var files = Files.list(directory)) {
              for (Path file :
                  files
                      .filter(path -> path.getFileName().toString().endsWith(".yml"))
                      .sorted()
                      .toList()) {
                loaded.add(
                    parse(YamlConfiguration.loadConfiguration(file.toFile()), file.toString()));
              }
            }
            WeaponRegistry candidate =
                loaded.isEmpty() ? loadBundled() : new WeaponRegistry(loaded);
            current.set(candidate);
            return candidate;
          } catch (IOException | RuntimeException failure) {
            throw new IllegalStateException(
                "Weapon definitions rejected: " + failure.getMessage(), failure);
          }
        },
        ioExecutor);
  }

  private WeaponRegistry loadBundled() {
    List<WeaponDefinition> loaded = new ArrayList<>();
    for (String id : BUNDLED) {
      String resource = "weapons/" + id + ".yml";
      try (InputStream input = plugin.getResource(resource)) {
        if (input == null) {
          throw new IllegalStateException("Missing bundled resource " + resource);
        }
        loaded.add(
            parse(
                YamlConfiguration.loadConfiguration(
                    new InputStreamReader(input, StandardCharsets.UTF_8)),
                resource));
      } catch (IOException failure) {
        throw new IllegalStateException("Cannot close bundled resource " + resource, failure);
      }
    }
    return new WeaponRegistry(loaded);
  }

  private void installDefaults() throws IOException {
    for (String id : BUNDLED) {
      Path target = directory.resolve(id + ".yml");
      if (Files.exists(target)) {
        continue;
      }
      try (InputStream input = plugin.getResource("weapons/" + id + ".yml")) {
        if (input == null) {
          throw new IOException("Missing bundled weapon " + id);
        }
        Files.copy(input, target);
      }
    }
  }

  static WeaponDefinition parse(YamlConfiguration yaml, String source) {
    if (yaml.getInt("schema-version", -1) != 1) {
      throw invalid(source, "schema-version", "expected 1");
    }
    String id = required(yaml, "id", source);
    String materialName = required(yaml, "presentation.material", source).toUpperCase(Locale.ROOT);
    Material material = Material.matchMaterial(materialName);
    if (material == null || !isItemMaterial(material)) {
      throw invalid(source, "presentation.material", "unknown item material " + materialName);
    }
    String upgradedMaterialName =
        yaml.getString("presentation.upgraded-material", materialName).toUpperCase(Locale.ROOT);
    Material upgradedMaterial = Material.matchMaterial(upgradedMaterialName);
    if (upgradedMaterial == null || !isItemMaterial(upgradedMaterial)) {
      throw invalid(
          source,
          "presentation.upgraded-material",
          "unknown item material " + upgradedMaterialName);
    }
    List<WeaponDefinition.Upgrade> upgrades = new ArrayList<>();
    for (Map<?, ?> raw : yaml.getMapList("upgrades")) {
      upgrades.add(
          new WeaponDefinition.Upgrade(
              integer(raw, "level", source),
              text(raw, "display-name", source),
              integer(raw, "cost", source),
              decimal(raw, "damage-multiplier", source),
              integer(raw, "magazine-bonus", source),
              integer(raw, "custom-model-data", source),
              strings(raw.get("effects"))));
    }
    WeaponDefinition definition =
        new WeaponDefinition(
            id,
            yaml.getString("display-name", id),
            enumValue(
                WeaponCategory.class, yaml.getString("category", "PISTOL"), source, "category"),
            enumValue(WeaponRarity.class, yaml.getString("rarity", "COMMON"), source, "rarity"),
            new WeaponDefinition.Presentation(
                materialName,
                yaml.getInt("presentation.custom-model-data", 0),
                upgradedMaterialName),
            new WeaponDefinition.Fire(
                enumValue(
                    FireMode.class,
                    yaml.getString("fire.mode", "SEMI_AUTOMATIC"),
                    source,
                    "fire.mode"),
                yaml.getInt("fire.cooldown-ticks", 5),
                yaml.getInt("fire.burst-size", 1),
                yaml.getInt("fire.burst-interval-ticks", 2),
                yaml.getInt("fire.charge-ticks", 0),
                yaml.getInt("fire.pellets", 1),
                yaml.getBoolean("fire.consumes-ammo", true)),
            new WeaponDefinition.Damage(
                yaml.getDouble("damage.base", 10),
                yaml.getDouble("damage.minimum-distance", 0),
                yaml.getDouble("damage.maximum-distance", 48),
                yaml.getDouble("damage.minimum-multiplier", 0.5),
                yaml.getDouble("damage.headshot-multiplier", 2),
                enumValue(
                    ZombieDamageType.class,
                    yaml.getString("damage.type", "BULLET"),
                    source,
                    "damage.type")),
            new WeaponDefinition.Ammo(
                yaml.getInt("ammo.magazine-size", 8),
                yaml.getInt("ammo.starting-reserve", 32),
                yaml.getInt("ammo.maximum-reserve", 64),
                yaml.getBoolean("ammo.infinite", false),
                yaml.getInt("ammo.overload-capacity", 0)),
            new WeaponDefinition.Reload(
                yaml.getInt("reload.duration-ticks", 40),
                yaml.getBoolean("reload.interruptible", true)),
            new WeaponDefinition.Spread(
                yaml.getDouble("spread.standing", 0.3),
                yaml.getDouble("spread.walking", 0.8),
                yaml.getDouble("spread.sprinting", 2),
                yaml.getDouble("spread.airborne", 3),
                yaml.getDouble("spread.aiming", 0.1)),
            new WeaponDefinition.Recoil(
                yaml.getDouble("recoil.vertical", 1),
                yaml.getDouble("recoil.horizontal", 0.4),
                yaml.getInt("recoil.recovery-ticks", 5)),
            new WeaponDefinition.Penetration(
                yaml.getInt("penetration.maximum-targets", 1),
                yaml.getDouble("penetration.damage-retention", 0.7),
                Set.copyOf(yaml.getStringList("penetration.materials"))),
            new WeaponDefinition.Economy(
                yaml.getInt("economy.wall-cost", 500),
                yaml.getInt("economy.ammo-cost", 250),
                yaml.getInt("economy.mystery-cost", 950)),
            yaml.getInt("mystery-box.weight", 100),
            upgrades,
            stringMap(yaml.getConfigurationSection("sounds")),
            Set.copyOf(yaml.getStringList("effects")));
    List<String> issues = definition.validate();
    if (!issues.isEmpty()) {
      throw invalid(source, id, String.join("; ", issues));
    }
    return definition;
  }

  private static Map<String, String> stringMap(ConfigurationSection section) {
    if (section == null) {
      return Map.of();
    }
    Map<String, String> values = new HashMap<>();
    section.getKeys(false).forEach(key -> values.put(key, section.getString(key, "")));
    return values;
  }

  private static boolean isItemMaterial(Material material) {
    return !Set.of(
            Material.AIR,
            Material.CAVE_AIR,
            Material.VOID_AIR,
            Material.WATER,
            Material.LAVA,
            Material.FIRE,
            Material.SOUL_FIRE,
            Material.NETHER_PORTAL,
            Material.END_PORTAL,
            Material.END_GATEWAY)
        .contains(material);
  }

  private static Set<String> strings(Object raw) {
    if (!(raw instanceof List<?> values)) {
      return Set.of();
    }
    HashSet<String> result = new HashSet<>();
    values.forEach(value -> result.add(String.valueOf(value)));
    return Set.copyOf(result);
  }

  private static String required(YamlConfiguration yaml, String path, String source) {
    String value = yaml.getString(path);
    if (value == null || value.isBlank()) {
      throw invalid(source, path, "required non-blank value");
    }
    return value;
  }

  private static String text(Map<?, ?> values, String key, String source) {
    Object value = values.get(key);
    if (value == null || String.valueOf(value).isBlank()) {
      throw invalid(source, "upgrades." + key, "required value");
    }
    return String.valueOf(value);
  }

  private static int integer(Map<?, ?> values, String key, String source) {
    Object value = values.get(key);
    if (value instanceof Number number) {
      return number.intValue();
    }
    throw invalid(source, "upgrades." + key, "expected integer");
  }

  private static double decimal(Map<?, ?> values, String key, String source) {
    Object value = values.get(key);
    if (value instanceof Number number) {
      return number.doubleValue();
    }
    throw invalid(source, "upgrades." + key, "expected number");
  }

  private static <E extends Enum<E>> E enumValue(
      Class<E> type, String value, String source, String path) {
    try {
      return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException failure) {
      throw invalid(source, path, "unknown value " + value);
    }
  }

  private static IllegalArgumentException invalid(String source, String path, String message) {
    return new IllegalArgumentException(source + " [" + path + "]: " + message);
  }
}
