package fr.heneria.zombie.plugin.bootstrap;

import fr.heneria.zombie.api.PluginState;
import fr.heneria.zombie.api.ZombieApi;
import fr.heneria.zombie.core.bootstrap.ServiceRegistry;
import fr.heneria.zombie.core.config.ConfigurationIssue;
import fr.heneria.zombie.core.config.ZombieSettingsValidator;
import fr.heneria.zombie.core.editor.MapEditorService;
import fr.heneria.zombie.core.editor.MapRegistry;
import fr.heneria.zombie.core.editor.MapValidator;
import fr.heneria.zombie.core.game.ZombieGameService;
import fr.heneria.zombie.core.instance.GameInstanceRegistry;
import fr.heneria.zombie.core.instance.GameInstanceService;
import fr.heneria.zombie.core.isolation.AudienceSelector;
import fr.heneria.zombie.core.isolation.VisibilityPolicy;
import fr.heneria.zombie.core.session.PlayerSessionService;
import fr.heneria.zombie.core.session.ReconnectPolicy;
import fr.heneria.zombie.plugin.api.PaperZombieApi;
import fr.heneria.zombie.plugin.command.ZombieCommand;
import fr.heneria.zombie.plugin.config.ConfigurationManager;
import fr.heneria.zombie.plugin.display.ContextScoreboardService;
import fr.heneria.zombie.plugin.editor.EditorGuiModule;
import fr.heneria.zombie.plugin.editor.EditorItemService;
import fr.heneria.zombie.plugin.editor.EditorPlacementListener;
import fr.heneria.zombie.plugin.editor.EditorSessionListener;
import fr.heneria.zombie.plugin.editor.YamlMapPersistence;
import fr.heneria.zombie.plugin.editor.ZMapCommand;
import fr.heneria.zombie.plugin.game.GameCombatListener;
import fr.heneria.zombie.plugin.game.PaperGameRuntime;
import fr.heneria.zombie.plugin.game.PaperZombieSpawner;
import fr.heneria.zombie.plugin.game.ZGameCommand;
import fr.heneria.zombie.plugin.gui.GuiActionRegistry;
import fr.heneria.zombie.plugin.gui.GuiChatInputListener;
import fr.heneria.zombie.plugin.gui.GuiConfigurationService;
import fr.heneria.zombie.plugin.gui.GuiListener;
import fr.heneria.zombie.plugin.gui.GuiRegistry;
import fr.heneria.zombie.plugin.gui.GuiScreens;
import fr.heneria.zombie.plugin.gui.GuiService;
import fr.heneria.zombie.plugin.instance.InstanceCoordinator;
import fr.heneria.zombie.plugin.isolation.PaperAudienceService;
import fr.heneria.zombie.plugin.isolation.VisibilityService;
import fr.heneria.zombie.plugin.listener.ContextDeathListener;
import fr.heneria.zombie.plugin.listener.InstanceWorldProtectionListener;
import fr.heneria.zombie.plugin.listener.IsolatedChatListener;
import fr.heneria.zombie.plugin.listener.MapPreviewListener;
import fr.heneria.zombie.plugin.listener.PlayerContextListener;
import fr.heneria.zombie.plugin.lobby.LobbyService;
import fr.heneria.zombie.plugin.map.MapPreviewService;
import fr.heneria.zombie.plugin.map.MapTemplateCatalog;
import fr.heneria.zombie.plugin.message.MessageService;
import fr.heneria.zombie.plugin.player.PaperPlayerStateService;
import fr.heneria.zombie.plugin.world.PaperMainThreadExecutor;
import fr.heneria.zombie.plugin.world.PaperWorldInstanceService;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Explicit composition root for the lobby and isolated-instance runtime. */
public final class ZombieBootstrap {

  private final JavaPlugin plugin;
  private final AtomicReference<PluginState> state;
  private final ServiceRegistry services = new ServiceRegistry();
  private ZombieApi api;
  private ExecutorService ioExecutor;
  private InstanceCoordinator coordinator;
  private MapPreviewService previewService;
  private GuiService guiService;
  private MapEditorService editorService;
  private EditorItemService editorItems;
  private BukkitTask maintenanceTask;
  private PaperGameRuntime gameRuntime;
  private BukkitTask gameTask;

  /**
   * Creates the bootstrap.
   *
   * @param plugin owning Paper plugin
   * @param state shared lifecycle state
   */
  public ZombieBootstrap(JavaPlugin plugin, AtomicReference<PluginState> state) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.state = Objects.requireNonNull(state, "state");
  }

  /**
   * Builds and registers the complete Ticket 002 service graph.
   *
   * @throws Exception when a critical service cannot initialize
   */
  public void start() throws Exception {
    ConfigurationManager configurations =
        new ConfigurationManager(
            plugin.getDataFolder().toPath(),
            plugin.getClass().getClassLoader(),
            new ZombieSettingsValidator());
    List<ConfigurationIssue> issues = configurations.initialize();
    issues.forEach(
        issue ->
            plugin
                .getLogger()
                .warning(
                    "Configuration "
                        + issue.severity()
                        + " ["
                        + issue.path()
                        + "]: "
                        + issue.message()));

    AtomicInteger threadCounter = new AtomicInteger();
    ioExecutor =
        Executors.newFixedThreadPool(
            2,
            runnable -> {
              Thread thread =
                  new Thread(runnable, "heneriazombie-io-" + threadCounter.incrementAndGet());
              thread.setDaemon(true);
              return thread;
            });
    PaperMainThreadExecutor mainThread = new PaperMainThreadExecutor(plugin);
    MessageService messages = new MessageService(configurations);
    MapTemplateCatalog templates =
        new MapTemplateCatalog(
            plugin.getServer().getWorldContainer().toPath(), configurations, ioExecutor);
    templates.refreshCount();
    plugin.getLogger().info("Map templates directory: " + templates.rootDirectory());
    PaperWorldInstanceService worlds =
        new PaperWorldInstanceService(
            plugin,
            plugin.getServer().getWorldContainer().toPath(),
            configurations,
            templates,
            ioExecutor,
            mainThread);
    GameInstanceRegistry registry = new GameInstanceRegistry();
    GameInstanceService instances =
        new GameInstanceService(
            registry,
            worlds,
            () -> configurations.current().settings().instances().maximumConcurrentGames(),
            () -> configurations.current().settings().instances().preserveFailedWorlds(),
            () ->
                Duration.ofSeconds(
                    configurations.current().settings().instances().creationTimeoutSeconds()),
            UUID::randomUUID,
            Clock.systemUTC(),
            diagnostic -> plugin.getLogger().warning(diagnostic));
    PlayerSessionService sessions =
        new PlayerSessionService(
            () -> {
              var reconnect = configurations.current().settings().reconnect();
              return new ReconnectPolicy(
                  reconnect.enabled(),
                  Duration.ofSeconds(reconnect.gracePeriodSeconds()),
                  reconnect.reservePlayerSlot(),
                  reconnect.returnToLobbyAfterExpiration());
            },
            Clock.systemUTC());
    PaperPlayerStateService playerStates = new PaperPlayerStateService();
    ContextScoreboardService scoreboards = new ContextScoreboardService();
    LobbyService lobby = new LobbyService(configurations, sessions, playerStates, scoreboards);
    org.bukkit.World activeLobbyWorld = lobby.initialize();
    String configuredLobbyWorld = configurations.current().settings().lobby().world();
    if (!activeLobbyWorld.getName().equals(configuredLobbyWorld)) {
      plugin
          .getLogger()
          .warning(
              "Could not load configured lobby world "
                  + configuredLobbyWorld
                  + "; using fallback world "
                  + activeLobbyWorld.getName()
                  + '.');
    }
    VisibilityPolicy visibilityPolicy = new VisibilityPolicy();
    VisibilityService visibility = new VisibilityService(plugin, sessions, visibilityPolicy);
    AudienceSelector audienceSelector = new AudienceSelector(sessions);
    PaperAudienceService audiences = new PaperAudienceService(audienceSelector);
    coordinator =
        new InstanceCoordinator(
            instances,
            sessions,
            templates,
            worlds,
            lobby,
            playerStates,
            scoreboards,
            visibility,
            mainThread);
    previewService = new MapPreviewService(templates, worlds, lobby, mainThread);
    api = new PaperZombieApi(state, templates::count, registry::size);
    GuiRegistry guiRegistry = new GuiRegistry();
    GuiActionRegistry guiActions = new GuiActionRegistry();
    YamlMapPersistence mapPersistence =
        new YamlMapPersistence(plugin.getDataFolder().toPath(), ioExecutor);
    MapRegistry mapRegistry = new MapRegistry();
    editorService = new MapEditorService(mapRegistry, mapPersistence, Clock.systemUTC());
    MapValidator mapValidator = new MapValidator();
    editorItems = new EditorItemService(plugin);
    editorService
        .initialize()
        .whenComplete(
            (count, failure) -> {
              if (failure == null) {
                plugin.getLogger().info("Loaded " + count + " editable map definition(s).");
              } else {
                plugin.getLogger().severe("Could not load editable maps: " + failure.getMessage());
              }
            });
    GuiConfigurationService guiConfigurations =
        new GuiConfigurationService(
            plugin.getDataFolder().toPath(),
            plugin.getClass().getClassLoader(),
            ioExecutor,
            () -> {
              java.util.HashSet<String> known = new java.util.HashSet<>(GuiScreens.ACTION_IDS);
              known.addAll(EditorGuiModule.ACTION_IDS);
              known.addAll(guiActions.ids());
              return java.util.Set.copyOf(known);
            },
            diagnostic -> plugin.getLogger().warning(diagnostic));
    var guiOptions = configurations.current().settings().gui();
    guiService =
        new GuiService(
            plugin,
            guiRegistry,
            guiActions,
            guiConfigurations,
            Clock.systemUTC(),
            Duration.ofSeconds(guiOptions.sessionTimeoutSeconds()),
            () -> configurations.current().settings().gui().inputTimeoutSeconds(),
            () -> configurations.current().settings().gui().soundsEnabled());
    new GuiScreens(
            plugin,
            plugin.getPluginMeta().getVersion(),
            guiRegistry,
            guiActions,
            guiConfigurations,
            configurations,
            guiService,
            templates,
            previewService,
            coordinator,
            sessions,
            api,
            mainThread,
            Clock.systemUTC())
        .register();
    new EditorGuiModule(
            guiRegistry,
            guiActions,
            guiConf◊v‚⁄$z{-ÆÈ‹j◊ù€‹TŸX›[€ä⁄[]\ôŸ]
N√BàCBàH[ŸHYà
ŸX›[€ãö\‘›ö[ô Ÿ^JJH√Bà\ôŸ]ú]
]ŸX›[€ãôŸ]›ö[ô Ÿ^KàäJN√BàCBàCBàCBÉBàö]ò]HôX€‹ôÿ[ôY]J€€ôöY›\ò][€î€ò\⁄›€ò\⁄›\›€€ôöY›\ò][€í\‹›YOà\‹›Y\ HﬂCBüCB