package fr.heneria.bedwars.plugin.map;

import fr.heneria.bedwars.core.config.WorldManagerSettings;
import fr.heneria.bedwars.core.map.MapSpawn;
import fr.heneria.bedwars.core.map.MapTemplate;
import fr.heneria.bedwars.core.map.MapWorldResult;
import fr.heneria.bedwars.core.map.MapWorldService;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main-thread Bukkit adapter for template working worlds.
 *
 * <p>Managed names always use the validated configured prefix. Forced unload moves every player to
 * the configured fallback world before calling Bukkit's unload API.
 */
public final class BukkitMapWorldService implements MapWorldService {
  private final JavaPlugin plugin;
  private final WorldManagerSettings settings;
  private final VoidChunkGenerator generator = new VoidChunkGenerator();

  public BukkitMapWorldService(JavaPlugin plugin, WorldManagerSettings settings) {
    this.plugin = plugin;
    this.settings = settings;
  }

  @Override
  public MapWorldResult createVoidWorld(MapTemplate template) {
    requirePrimaryThread();
    if (Bukkit.getWorld(template.worldName()) != null)
      return MapWorldResult.failure("A Bukkit world with this name is already loaded");
    World world = creator(template).createWorld();
    if (world == null) return MapWorldResult.failure("Bukkit returned no world");
    applySettings(world, template);
    createSafetyPlatform(world);
    world.save();
    return MapWorldResult.success();
  }

  @Override
  public MapWorldResult load(MapTemplate template) {
    requirePrimaryThread();
    if (Bukkit.getWorld(template.worldName()) != null)
      return MapWorldResult.failure("World is already loaded");
    World world = creator(template).createWorld();
    if (world == null) return MapWorldResult.failure("Bukkit could not load the world folder");
    applySettings(world, template);
    return MapWorldResult.success();
  }

  @Override
  public MapWorldResult save(MapTemplate template) {
    requirePrimaryThread();
    World world = Bukkit.getWorld(template.worldName());
    if (world == null) return MapWorldResult.failure("World is not loaded");
    world.save();
    return MapWorldResult.success();
  }

  @Override
  public MapWorldResult unload(MapTemplate template, boolean save) {
    requirePrimaryThread();
    World world = Bukkit.getWorld(template.worldName());
    if (world == null) return MapWorldResult.failure("World is not loaded");
    if (!world.getPlayers().isEmpty()) {
      World fallback = Bukkit.getWorld(settings.fallbackWorld());
      if (fallback == null || fallback.equals(world))
        return MapWorldResult.failure("Fallback world is missing or unsafe");
      Location target = fallback.getSpawnLocation();
      for (Player player : java.util.List.copyOf(world.getPlayers())) {
        if (!player.teleport(target)) return MapWorldResult.failure("Player evacuation failed");
      }
    }
    return Bukkit.unloadWorld(world, save)
        ? MapWorldResult.success()
        : MapWorldResult.failure("Bukkit refused to unload the world");
  }

  @Override
  public boolean isLoaded(MapTemplate template) {
    return Bukkit.getWorld(template.worldName()) != null;
  }

  @Override
  public int playerCount(MapTemplate template) {
    World world = Bukkit.getWorld(template.worldName());
    return world == null ? 0 : world.getPlayers().size();
  }

  /** Loads the spawn chunk and teleports one administrator on the main thread. */
  public MapWorldResult teleport(Player player, MapTemplate template) {
    requirePrimaryThread();
    World world = Bukkit.getWorld(template.worldName());
    if (world == null) return MapWorldResult.failure("World is not loaded");
    MapSpawn spawn = template.spawn();
    Location location =
        spawn.configured()
            ? new Location(world, spawn.x(), spawn.y(), spawn.z(), spawn.yaw(), spawn.pitch())
            : world.getSpawnLocation();
    world.getChunkAt(location).load();
    return player.teleport(location)
        ? MapWorldResult.success()
        : MapWorldResult.failure("Teleportation was refused");
  }

  private WorldCreator creator(MapTemplate template) {
    return new WorldCreator(template.worldName())
        .environment(World.Environment.valueOf(template.environment().toUpperCase(Locale.ROOT)))
        .generator(generator)
        .generateStructures(false);
  }

  private void applySettings(World world, MapTemplate template) {
    world.setDifficulty(Difficulty.valueOf(settings.difficulty()));
    world.setPVP(settings.pvp());
    world.setSpawnFlags(template.settings().allowMonsters(), template.settings().allowAnimals());
    world.setAutoSave(template.settings().autoSave());
    world.setTime(template.settings().fixedTime());
    if (template.settings().clearWeather()) {
      world.setStorm(false);
      world.setThundering(false);
      world.setWeatherDuration(0);
    }
    rule(world, "doDaylightCycle", false);
    rule(world, "doWeatherCycle", false);
    rule(world, "doMobSpawning", false);
    rule(world, "doPatrolSpawning", false);
    rule(world, "doTraderSpawning", false);
    rule(world, "doInsomnia", false);
    rule(world, "doFireTick", false);
    rule(world, "mobGriefing", false);
    rule(world, "keepInventory", true);
    rule(world, "announceAdvancements", false);
    rule(world, "showDeathMessages", false);
    rule(world, "doImmediateRespawn", true);
    rule(world, "randomTickSpeed", 0);
    rule(world, "spawnRadius", 0);
    MapSpawn spawn = template.spawn();
    world.setSpawnLocation(
        (int) Math.floor(spawn.x()), (int) Math.floor(spawn.y()), (int) Math.floor(spawn.z()));
  }

  private void createSafetyPlatform(World world) {
    if (!settings.createSafetyPlatform()) return;
    Material material = Material.valueOf(settings.platformMaterial());
    int radius = settings.platformRadius();
    int y = settings.platformY();
    for (int x = -radius; x <= radius; x++)
      for (int z = -radius; z <= radius; z++) world.getBlockAt(x, y, z).setType(material, false);
    world.setSpawnLocation(0, y + 1, 0);
  }

  @SuppressWarnings({"rawtypes", "unchecked", "deprecation"})
  private static void rule(World world, String name, Object value) {
    GameRule rule = GameRule.getByName(name);
    if (rule != null) world.setGameRule(rule, value);
  }

  private static void requirePrimaryThread() {
    if (!Bukkit.isPrimaryThread())
      throw new IllegalStateException("Bukkit map-world operation must run on the server thread");
  }
}
