package fr.heneria.bedwars.plugin;

import fr.heneria.bedwars.api.PluginStatus;
import fr.heneria.bedwars.core.arena.ArenaMapTemplateStatus;
import fr.heneria.bedwars.core.arena.ArenaRegistry;
import fr.heneria.bedwars.core.arena.ArenaService;
import fr.heneria.bedwars.core.arena.ArenaValidator;
import fr.heneria.bedwars.core.arena.editor.ArenaEditorStateStore;
import fr.heneria.bedwars.core.gui.TextInputManager;
import fr.heneria.bedwars.core.map.MapId;
import fr.heneria.bedwars.core.map.MapOperationLock;
import fr.heneria.bedwars.core.map.MapState;
import fr.heneria.bedwars.core.map.MapTemplateRegistry;
import fr.heneria.bedwars.core.map.MapTemplateService;
import fr.heneria.bedwars.core.map.MapTemplateValidator;
import fr.heneria.bedwars.core.map.MapType;
import fr.heneria.bedwars.core.map.MapWorldSettings;
import fr.heneria.bedwars.core.map.editor.MapEditorStateStore;
import fr.heneria.bedwars.core.map.operation.MapOperationTracker;
import fr.heneria.bedwars.plugin.arena.ArenaEditorMenuFactory;
import fr.heneria.bedwars.plugin.arena.ArenaLifecycleComponent;
import fr.heneria.bedwars.plugin.arena.YamlArenaRepository;
import fr.heneria.bedwars.plugin.bootstrap.BedWarsBootstrap;
import fr.heneria.bedwars.plugin.bootstrap.PluginBootstrap;
import fr.heneria.bedwars.plugin.command.BedWarsCommand;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import fr.heneria.bedwars.plugin.gui.BukkitGuiService;
import fr.heneria.bedwars.plugin.gui.BukkitTextInputService;
import fr.heneria.bedwars.plugin.item.BukkitItemService;
import fr.heneria.bedwars.plugin.logging.BukkitProjectLogger;
import fr.heneria.bedwars.plugin.map.BukkitMapWorldService;
import fr.heneria.bedwars.plugin.map.MapLifecycleComponent;
import fr.heneria.bedwars.plugin.map.MapMenuFactory;
import fr.heneria.bedwars.plugin.map.MapMenuNavigation;
import fr.heneria.bedwars.plugin.map.SecureMapFileService;
import fr.heneria.bedwars.plugin.map.YamlMapTemplateRepository;
import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/** Paper entry point. All construction and lifecycle work is delegated to a bootstrap. */
public final class HeneriaBedWarsPlugin extends JavaPlugin {
  private PluginBootstrap bootstrap;
  private ConfigurationService configurations;
  private BukkitGuiService guiService;
  private BukkitItemService itemService;
  private ArenaService arenaService;
  private BukkitTextInputService textInputService;
  private ArenaEditorMenuFactory arenaEditor;
  private MapTemplateService mapService;
  private BukkitMapWorldService mapWorldService;
  private MapMenuFactory mapMenus;

  @Override
  public void onEnable() {
    try {
      BukkitProjectLogger projectLogger = new BukkitProjectLogger(getLogger(), false);
      configurations =
          new ConfigurationService(
              getDataFolder().toPath(),
              this::getResource,
              projectLogger,
              Clock.systemDefaultZone());
      configurations.initialize();
      projectLogger.setDebug(configurations.snapshot().plugin().debug());
      Clock clock = Clock.systemDefaultZone();
      var worldSettings = configurations.snapshot().worlds();
      MapTemplateRegistry mapRegistry = new MapTemplateRegistry();
      arenaService =
          new ArenaService(
              new YamlArenaRepository(getDataFolder().toPath(), clock),
              new ArenaRegistry(),
              new ArenaValidator(
                  world -> getServer().getWorld(world) != null,
                  mapId -> {
                    try {
                      var template = mapRegistry.find(MapId.parse(mapId)).orElse(null);
                      if (template == null) return ArenaMapTemplateStatus.NOT_FOUND;
                      if (template.type() != MapType.BEDWARS)
                        return ArenaMapTemplateStatus.INVALID_TYPE;
                      return template.state() == MapState.ERROR
                          ? ArenaMapTemplateStatus.ERROR
                          : ArenaMapTemplateStatus.VALID;
                    } catch (RuntimeException exception) {
                      return ArenaMapTemplateStatus.NOT_FOUND;
                    }
                  }),
              clock);
      mapWorldService = new BukkitMapWorldService(this, worldSettings);
      var data = getDataFolder().toPath();
      mapService =
          new MapTemplateService(
              new YamlMapTemplateRepository(data.resolve(worldSettings.metadataDirectory()), clock),
              mapRegistry,
              mapWorldService,
              new SecureMapFileService(
                  data.resolve(worldSettings.templatesDirectory()),
                  data.resolve(worldSettings.metadataDirectory()),
                  getServer().getWorldContainer().toPath(),
                  data.resolve(worldSettings.backupsDirectory()),
                  worldSettings.excludedFiles(),
                  worldSettings.excludedDirectories(),
                  clock),
              new MapOperationLock(),
              new MapTemplateValidator(),
              clock,
              worldSettings.templateWorldPrefix(),
              worldSettings.environment(),
              new MapWorldSettings(
                  worldSettings.autoSave(),
                  worldSettings.animals(),
                  worldSettings.monsters(),
                  worldSettings.fixedTime(),
                  worldSettings.clearWeather(),
                  false,
                  false,
                  worldSettings.difficulty(),
                  worldSettings.pvp(),
                  false,
                  false),
              worldSettings.platformY(),
              worldSettings.saveBeforeUnload(),
              getDescription().getVersion(),
              mapId ->
                  arenaService.list().stream()
                      .filter(
                          arena ->
                              arena.template().filter(mapId.value()::equalsIgnoreCase).isPresent())
                      .map(arena -> arena.id().value())
                      .collect(java.util.stream.Collectors.toUnmodifiableSet()),
              template ->
                  configurations.snapshot().lobby().configured()
                      && configurations
                          .snapshot()
                          .lobby()
                          .world()
                          .equalsIgnoreCase(template.worldName()));
      itemService = new BukkitItemService(this, configurations, projectLogger);
      guiService = new BukkitGuiService(this, configurations, itemService, projectLogger);
      ArenaEditorStateStore editorStates = new ArenaEditorStateStore();
      MapEditorStateStore mapEditorStates = new MapEditorStateStore();
      MapOperationTracker mapOperations = new MapOperationTracker(clock);
      textInputService =
          new BukkitTextInputService(
              this,
              new TextInputManager(clock),
              playerId -> {
                editorStates.remove(playerId);
                mapEditorStates.remove(playerId);
              },
              () -> {
                editorStates.clear();
                mapEditorStates.clear();
              },
              projectLogger);
      AtomicReference<ArenaEditorMenuFactory> arenaEditorReference = new AtomicReference<>();
      MapMenuNavigation mapNavigation =
          new MapMenuNavigation() {
            @Override
            public fr.heneria.bedwars.core.gui.Gui dashboard(UUID playerId) {
              return arenaEditorReference.get().setup(playerId);
            }

            @Override
            public fr.heneria.bedwars.core.gui.Gui arenaEditor(UUID playerId, String arenaId) {
              return arenaEditorReference.get().editor(playerId, arenaId);
            }
          };
      mapMenus =
          new MapMenuFactory(
              this,
              mapService,
              mapWorldService,
              arenaService,
              configurations,
              guiService,
              textInputService,
              mapEditorStates,
              mapOperations,
              mapNavigation,
              projectLogger,
              clock);
      arenaEditor =
          new ArenaEditorMenuFactory(
              this,
              arenaService,
              configurations,
              guiService,
              textInputService,
              editorStates,
              projectLogger,
              mapService,
              mapMenus);
      arenaEditorReference.set(arenaEditor);
      bootstrap =
          new BedWarsBootstrap(
              getDescription().getVersion(),
              configurations,
              arenaService,
              mapService,
              new MapLifecycleComponent(
                  this,
                  mapService,
                  arenaService,
                  worldSettings,
                  mapEditorStates,
                  mapOperations,
                  projectLogger),
              new ArenaLifecycleComponent(arenaService, projectLogger),
              textInputService,
              textInputService,
              itemService,
              guiService,
              projectLogger);
      bootstrap.start();
      registerDiagnosticCommand();
      getLogger().info("HeneriaBedWars " + getDescription().getVersion() + " enabled.");
    } catch (Exception exception) {
      getLogger()
          .log(Level.SEVERE, "Critical startup failure; disabling HeneriaBedWars.", exception);
      getServer().getPluginManager().disablePlugin(this);
    }
  }

  @Override
  public void onDisable() {
    if (bootstrap == null || bootstrap.status() != PluginStatus.RUNNING) {
      return;
    }
    try {
      bootstrap.stop();
      getLogger().info("HeneriaBedWars disabled cleanly.");
    } catch (Exception exception) {
      getLogger().log(Level.SEVERE, "HeneriaBedWars failed during shutdown.", exception);
    }
  }

  private void registerDiagnosticCommand() {
    PluginCommand command = getCommand("bedwars");
    if (command == null) {
      throw new IllegalStateException("The 'bedwars' command is not declared in plugin.yml");
    }
    BedWarsCommand executor =
        new BedWarsCommand(
            this,
            bootstrap,
            configurations,
            arenaService,
            mapService,
            mapWorldService,
            itemService,
            guiService,
            arenaEditor,
            mapMenus,
            new BukkitProjectLogger(getLogger(), configurations.snapshot().plugin().debug()));
    command.setExecutor(executor);
    command.setTabCompleter(executor);
  }
}
