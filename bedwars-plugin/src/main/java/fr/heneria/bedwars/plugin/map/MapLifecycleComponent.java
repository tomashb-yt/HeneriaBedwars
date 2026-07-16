package fr.heneria.bedwars.plugin.map;

import fr.heneria.bedwars.core.arena.ArenaService;
import fr.heneria.bedwars.core.config.WorldManagerSettings;
import fr.heneria.bedwars.core.lifecycle.LifecycleComponent;
import fr.heneria.bedwars.core.logging.ProjectLogger;
import fr.heneria.bedwars.core.map.MapOperationResult;
import fr.heneria.bedwars.core.map.MapTemplateLoadResult;
import fr.heneria.bedwars.core.map.MapTemplateService;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Loads map metadata, runs one central autosave task and unloads managed worlds at shutdown. */
public final class MapLifecycleComponent implements LifecycleComponent {
  private final JavaPlugin plugin;
  private final MapTemplateService maps;
  private final ArenaService arenas;
  private final WorldManagerSettings settings;
  private final ProjectLogger logger;
  private BukkitTask autosaveTask;

  public MapLifecycleComponent(
      JavaPlugin plugin,
      MapTemplateService maps,
      ArenaService arenas,
      WorldManagerSettings settings,
      ProjectLogger logger) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.maps = Objects.requireNonNull(maps, "maps");
    this.arenas = Objects.requireNonNull(arenas, "arenas");
    this.settings = Objects.requireNonNull(settings, "settings");
    this.logger = Objects.requireNonNull(logger, "logger");
  }

  @Override
  public String name() {
    return "map-templates";
  }

  @Override
  public void start() throws Exception {
    MapTemplateLoadResult result = maps.reload();
    maps.synchronizeLinks(arenas.list());
    plugin.getServer().getScheduler().runTask(plugin, () -> maps.synchronizeLinks(arenas.list()));
    long loaded =
        maps.list().stream().filter(fr.heneria.bedwars.core.map.MapTemplate::loaded).count();
    long errors =
        maps.list().stream()
            .filter(map -> map.state() == fr.heneria.bedwars.core.map.MapState.ERROR)
            .count();
    logger.info(
        "[Maps] Loaded "
            + result.templates().size()
            + " map templates: "
            + loaded
            + " loaded, "
            + (result.templates().size() - loaded - errors)
            + " unloaded, "
            + errors
            + " invalid.");
    result.failures().forEach(failure -> logger.warning("Map metadata ignored: " + failure));
    if (settings.autoSave()) {
      long ticks = settings.autoSaveIntervalMinutes() * 60L * 20L;
      autosaveTask =
          plugin.getServer().getScheduler().runTaskTimer(plugin, this::autosave, ticks, ticks);
    }
  }

  @Override
  public void stop() {
    if (autosaveTask != null) autosaveTask.cancel();
    for (var template : maps.list()) {
      if (!template.loaded()) continue;
      MapOperationResult result = maps.unload(template.id().value(), true);
      if (!result.successful())
        logger.warning("Unable to unload managed map " + template.id() + ": " + result.detail());
    }
  }

  private void autosave() {
    for (var template : maps.list()) {
      if (!template.loaded() || !template.settings().autoSave()) continue;
      MapOperationResult result = maps.save(template.id().value());
      if (!result.successful())
        logger.warning("Autosave failed for map " + template.id() + ": " + result.detail());
    }
  }
}
