package fr.heneria.bedwars.plugin.game;

import fr.heneria.bedwars.core.arena.ArenaGeneratorDefinition;
import fr.heneria.bedwars.core.arena.ArenaLocation;
import fr.heneria.bedwars.core.config.ConfigurationDocument;
import fr.heneria.bedwars.core.config.ConfigurationId;
import fr.heneria.bedwars.core.game.generator.GeneratorId;
import fr.heneria.bedwars.core.game.generator.GeneratorResource;
import fr.heneria.bedwars.core.game.generator.GeneratorStackingStrategy;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.bukkit.Material;

/** Resolves validated generator defaults without exposing Bukkit to the core. */
public final class BukkitGeneratorCatalog {
  private final ConfigurationService configurations;

  public BukkitGeneratorCatalog(ConfigurationService configurations) {
    this.configurations = Objects.requireNonNull(configurations, "configurations");
  }

  public ArenaGeneratorDefinition create(
      GeneratorResource resource, ArenaLocation location, List<ArenaGeneratorDefinition> existing) {
    String type = resource.name().toLowerCase(Locale.ROOT);
    int sequence = 1;
    while (contains(existing, type + '-' + sequence)) sequence++;
    return new ArenaGeneratorDefinition(
        new GeneratorId(type + '-' + sequence),
        resource,
        location,
        1,
        integer("generators.defaults." + type + ".interval-ticks", fallbackInterval(resource)),
        integer("generators.defaults." + type + ".amount", 1),
        integer("generators.defaults." + type + ".local-capacity", 48),
        stacking("generators.defaults." + type + ".stacking"));
  }

  public Material material(GeneratorResource resource) {
    String type = resource.name().toLowerCase(Locale.ROOT);
    String configured = string("generators.defaults." + type + ".material", resource.name());
    Material material = Material.matchMaterial(configured);
    if (material != null && material.isItem()) return material;
    return switch (resource) {
      case IRON -> Material.IRON_INGOT;
      case GOLD -> Material.GOLD_INGOT;
      case DIAMOND -> Material.DIAMOND;
      case EMERALD -> Material.EMERALD;
    };
  }

  public double mergeRadius() {
    Object value = document().value("generators.merge-radius");
    if (value instanceof Number number) return Math.max(0.25, Math.min(8, number.doubleValue()));
    return 1.5;
  }

  private int integer(String path, int fallback) {
    Object value = document().value(path);
    if (value instanceof Number number) return Math.max(1, number.intValue());
    return fallback;
  }

  private String string(String path, String fallback) {
    Object value = document().value(path);
    return value == null ? fallback : String.valueOf(value).trim();
  }

  private GeneratorStackingStrategy stacking(String path) {
    try {
      return GeneratorStackingStrategy.valueOf(
          string(path, "MERGE_NEARBY").toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      return GeneratorStackingStrategy.MERGE_NEARBY;
    }
  }

  private ConfigurationDocument document() {
    return configurations.snapshot().documents().get(ConfigurationId.GENERATORS);
  }

  private static boolean contains(List<ArenaGeneratorDefinition> values, String id) {
    return values.stream().anyMatch(value -> value.id().value().equals(id));
  }

  private static int fallbackInterval(GeneratorResource resource) {
    return switch (resource) {
      case IRON -> 20;
      case GOLD -> 160;
      case DIAMOND -> 600;
      case EMERALD -> 1200;
    };
  }
}
