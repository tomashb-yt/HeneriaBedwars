package fr.heneria.zombie.plugin.bootstrap;

import fr.heneria.zombie.api.PluginState;
import fr.heneria.zombie.api.ZombieApi;
import fr.heneria.zombie.core.bootstrap.ServiceRegistry;
import fr.heneria.zombie.core.config.ConfigurationIssue;
import fr.heneria.zombie.core.config.ZombieSettingsValidator;
import fr.heneria.zombie.core.economy.EconomyEventDispatcher;
import fr.heneria.zombie.core.economy.EconomyService;
import fr.heneria.zombie.core.economy.PriceResolver;
import fr.heneria.zombie.core.economy.PurchaseService;
import fr.heneria.zombie.core.economy.RewardService;
import fr.heneria.zombie.core.economy.TransactionService;
import fr.heneria.zombie.core.editor.MapEditorService;
import fr.heneria.zombie.core.editor.MapRegistry;
import fr.heneria.zombie.core.editor.MapValidator;
import fr.heneria.zombie.core.game.ZombieGameService;
import fr.heneria.zombie.core.instance.GameInstanceRegistry;
import fr.heneria.zombie.core.instance.GameInstanceService;
import fr.heneria.zombie.core.isolation.AudienceSelector;
import fr.heneria.zombie.core.isolation.VisibilityPolicy;
import fr.heneria.zombie.core.powerup.PowerUpDropService;
import fr.heneria.zombie.core.powerup.PowerUpRegistry;
import fr.heneria.zombie.core.powerup.PowerUpService;
import fr.heneria.zombie.core.session.PlayerSessionService;
import fr.heneria.zombie.core.session.ReconnectPolicy;
import fr.heneria.zombie.plugin.api.PaperZombieApi;
import fr.heneria.zombie.plugin.command.ZombieCommand;
import fr.heneria.zombie.plugin.config.ConfigurationManager;
import fr.heneria.zombie.plugin.display.ContextScoreboardService;
import fr.heneria.zombie.plugin.economy.PaperPowerUpService;
import fr.heneria.zombie.plugin.economy.PointDisplayService;
import fr.heneria.zombie.plugin.economy.ZEconomyCommand;
import fr.heneria.zombie.plugin.editor.EditorGuiModule;
import fr.heneria.zombie.plugin.editor.EditorItemService;
import fr.heneria.zombie.plugin.editor.EditorPlacementListener;
import fr.heneria.zombie.plugin.editor.EditorSessionListener;
import fr.heneria.zombie.plugin.editor.MapVisualizationService;
import fr.heneria.zombie.plugin.editor.YamlMapPersistence;
import fr.heneria.zombie.plugin.editor.ZMapCommand;
import fr.heneria.zombie.plugin.enemy.PaperZombieEngine;
import fr.heneria.zombie.plugin.enemy.ZZombieCommand;
import fr.heneria.zombie.plugin.enemy.ZombieDefinitionLoader;
import fr.heneria.zombie.plugin.enemy.ZombieProtectionListener;
import fr.heneria.zombie.plugin.game.GameCombatListener;
import fr.heneria.zombie.plugin.game.PaperGameRuntime;
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
import fr.heneria.zombie.plugin.weapon.PaperWeaponService;
import fr.heneria.zombie.plugin.weapon.WeaponDefinitionLoader;
import fr.heneria.zombie.plugin.weapon.WeaponGuiModule;
import fr.heneria.zombie.plugin.weapon.WeaponListener;
import fr.heneria.zombie.plugin.weapon.ZWeaponCommand;
import fr.heneria.zombie.plugin.world.PaperMainThreadExecutor;
import fr.heneria.zombie.plugin.world.PaperWorldInstanceService;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
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
  private MapVisualizationService mapVisualizations;
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
    mapVisualizations = new MapVisualizationService(plugin);
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
            guiConfigurations,
            guiService,
            editorService,
            mapValidator,
            editorItems,
            mainThread,
            Clock.systemUTC())
        .register();
    ZombieGameService gameService =
        new ZombieGameService(
            Clock.systemUTC(),
            event ->
                plugin
                    .getLogger()
                    .fine(
                        "Game event "
                            + event.getClass().getSimpleName()
                            + " for "
                            + event.gameId()));
    EconomyService economies = new EconomyService();
    PointDisplayService pointDisplay =
        new PointDisplayService(
            configurations.current().settings().economy().displayGroupWindowTicks());
    EconomyEventDispatcher economyEvents =
        event -> {
          pointDisplay.accept(event);
          plugin
              .getLogger()
              .fine(
                  "Economy event "
                      + event.type()
                      + " for "
                      + event.gameId()
                      + "/"
                      + event.playerId());
        };
    TransactionService transactions =
        new TransactionService(
            economies, Clock.systemUTC(), economyEvents, plugin.getLogger()::warning);
    PurchaseService purchases =
        new PurchaseService(
            transactions,
            new PriceResolver(
                PriceResolver.Rounding.valueOf(
                    configurations
                        .current()
                        .settings()
                        .economy()
                        .priceRounding()
                        .toUpperCase(java.util.Locale.ROOT)),
                configurations.current().settings().economy().minimumPrice(),
                configurations.current().settings().economy().maximumPrice()),
            economyEvents,
            Clock.systemUTC());
    var powerUpOptions = configurations.current().settings(×½¶‰žËkºwµç@€€€ô4(€€€€€€€€€€€ô¤ì4(€€€Ñ½µ¥I•™•É•¹”ñA…Á•É…µ•IÕ¹Ñ¥µ”ø…µ•I•™•É•¹”€ô¹•ÜÑ½µ¥I•™•É•¹”ðø ¤ì4(€€€A…Á•Éi½µ‰¥•¹¥¹”é½µ‰¥•¹¥¹”€ô4(€€€€€€€¹•ÜA…Á•Éi½µ‰¥•¹¥¹” 4(€€€€€€€€€€€Á±Õ¥¸°4(€€€€€€€€€€€é½µ‰¥••™¥¹¥Ñ¥½¹Ì°4(€€€€€€€€€€€…µ•%€´øì4(€€€€€€€€€€€€€A…Á•É…µ•IÕ¹Ñ¥µ”ÉÕ¹Ñ¥µ”€ô…µ•I•™•É•¹”¹•Ð ¤ì4(€€€€€€€€€€€€€É•ÑÕÉ¸ÉÕ¹Ñ¥µ”€ôô¹Õ±°€ü1¥ÍÐ¹½˜ ¤€èÉÕ¹Ñ¥µ”¹Á±…å•É%‘Ì¡…µ•%¤ì4(€€€€€€€€€€€ô°4(€€€€€€€€€€€€¡…µ•%°Á±…å•É%¤€´øì4(€€€€€€€€€€€€€A…Á•É…µ•IÕ¹Ñ¥µ”ÉÕ¹Ñ¥µ”€ô…µ•I•™•É•¹”¹•Ð ¤ì4(€€€€€€€€€€€€€É•ÑÕÉ¸ÉÕ¹Ñ¥µ”€„ô¹Õ±°€˜˜ÉÕ¹Ñ¥µ”¹¥ÍQ…É•Ñ…‰±”¡…µ•%°Á±…å•É%¤ì4(€€€€€€€€€€€ô°4(€€€€€€€€€€€€¡Í½ÕÉ”°Ñ…É•Ð°…µ½Õ¹Ð¤€´øì4(€€€€€€€€€€€€€A…Á•É…µ•IÕ¹Ñ¥µ”ÉÕ¹Ñ¥µ”€ô…µ•I•™•É•¹”¹•Ð ¤ì4(€€€€€€€€€€€€€¥˜€¡ÉÕ¹Ñ¥µ”€„ô¹Õ±°¤ì4(€€€€€€€€€€€€€€€ÉÕ¹Ñ¥µ”¹‘…µ…•A±…å•È¡Í½ÕÉ”¹…µ•% ¤°Ñ…É•Ð°…µ½Õ¹Ð¤ì4(€€€€€€€€€€€€€ô4(€€€€€€€€€€€ô°4(€€€€€€€€€€€€¡é½µ‰¥”°É•…Í½¸°­¥±±•É%°É•Ý…É°É•Ý…É‘I•…Í½¸°‘•…Ñ¡1½…Ñ¥½¸¤€´øì4(€€€€€€€€€€€€€A…Á•É…µ•IÕ¹Ñ¥µ”ÉÕ¹Ñ¥µ”€ô…µ•I•™•É•¹”¹•Ð ¤ì4(€€€€€€€€€€€€€¥˜€¡ÉÕ¹Ñ¥µ”€„ô¹Õ±°€˜˜É•…Í½¸¹½µÁ±•Ñ•ÍI½Õ¹‘M±½Ð ¤¤ì4(€€€€€€€€€€€€€€€ÉÕ¹Ñ¥µ”¹é½µ‰¥•I•µ½Ù• 4(€€€€€€€€€€€€€€€€€€€é½µ‰¥”¹•¹Ñ¥Ñå% ¤°­¥±±•É%°É•Ý…É°É•Ý…É‘I•…Í½¸°‘•…Ñ¡1½…Ñ¥½¸¤ì4(€€€€€€€€€€€€€ô4(€€€€€€€€€€€ô¤ì4(€€€A…Á•ÉA½Ý•ÉUÁM•ÉÙ¥”Á…Á•ÉA½Ý•ÉUÁÌ€ô4(€€€€€€€¹•ÜA…Á•ÉA½Ý•ÉUÁM•ÉÙ¥” 4(€€€€€€€€€€€Á½Ý•ÉUÁÌ°4(€€€€€€€€€€€Á½Ý•ÉUÁÉ½ÁÌ°4(€€€€€€€€€€€é½µ‰¥•¹¥¹”°4(€€€€€€€€€€€…µ•%€´øì4(€€€€€€€€€€€€€A…Á•É…µ•IÕ¹Ñ¥µ”ÉÕ¹Ñ¥µ”€ô…µ•I•™•É•¹”¹•Ð ¤ì4(€€€€€€€€€€€€€É•ÑÕÉ¸ÉÕ¹Ñ¥µ”€ôô¹Õ±°€ü1¥ÍÐ¹½˜ ¤€èÉÕ¹Ñ¥µ”¹Á±…å•É%‘Ì¡…µ•%¤ì4(€€€€€€€€€€€ô¤ì4(€€€]•…Á½¹•™¥¹¥Ñ¥½¹1½…‘•ÈÝ•…Á½¹•™¥¹¥Ñ¥½¹Ì€ô¹•Ü]•…Á½¹•™¥¹¥Ñ¥½¹1½…‘•È¡Á±Õ¥¸°¥½á•ÕÑ½È¤ì4(€€€Ý•…Á½¹•™¥¹¥Ñ¥½¹Ì4(€€€€€€€€¹¥¹¥Ñ¥…±¥é•Íå¹Œ ¤4(€€€€€€€€¹Ý¡•¹½µÁ±•Ñ” 4(€€€€€€€€€€€€¡±½…‘•°™…¥±ÕÉ”¤€´øì4(€€€€€€€€€€€€€¥˜€¡™…¥±ÕÉ”€ôô¹Õ±°¤ì4(€€€€€€€€€€€€€€€Á±Õ¥¸4(€€€€€€€€€€€€€€€€€€€€¹•Ñ1½•È ¤4(€€€€€€€€€€€€€€€€€€€€¹¥¹™¼ 4(€€€€€€€€€€€€€€€€€€€€€€€€‰1½…‘•€ˆ4(€€€€€€€€€€€€€€€€€€€€€€€€€€€€¬±½…‘•¹Í¥é” ¤4(€€€€€€€€€€€€€€€€€€€€€€€€€€€€¬€ˆÝ•…Á½¸‘•™¥¹¥Ñ¥½¸¡Ì¤™É½´€ˆ4(€€€€€€€€€€€€€€€€€€€€€€€€€€€€¬Ý•…Á½¹•™¥¹¥Ñ¥½¹Ì¹‘¥É•Ñ½Éä ¤¤ì4(€€€€€€€€€€€€€ô•±Í”ì4(€€€€€€€€€€€€€€€Á±Õ¥¸4(€€€€€€€€€€€€€€€€€€€€¹•Ñ1½•È ¤4(€€€€€€€€€€€€€€€€€€€€¹Í•Ù•É” 4(€€€€€€€€€€€€€€€€€€€€€€€€‰áÑ•É¹…°Ý•…Á½¸‘•™¥¹¥Ñ¥½¹ÌÉ•©•Ñ•ì±…ÍÐÙ…±¥Í¹…ÁÍ¡½ÐÉ•µ…¥¹Ì…Ñ¥Ù”è€ˆ4(€€€€€€€€€€€€€€€€€€€€€€€€€€€€¬™…¥±ÕÉ”¹•Ñ5•ÍÍ…” ¤¤ì4(€€€€€€€€€€€€€ô4(€€€€€€€€€€€ô¤ì4(€€€A…Á•É]•…Á½¹M•ÉÙ¥”Ý•…Á½¹M•ÉÙ¥”€ô4(€€€€€€€¹•ÜA…Á•É]•…Á½¹M•ÉÙ¥” 4(€€€€€€€€€€€Á±Õ¥¸°4(€€€€€€€€€€€Ý•…Á½¹•™¥¹¥Ñ¥½¹Ì°4(€€€€€€€€€€€é½µ‰¥•¹¥¹”°4(€€€€€€€€€€€Á±…å•É%€´øì4(€€€€€€€€€€€€€A…Á•É…µ•IÕ¹Ñ¥µ”ÉÕ¹Ñ¥µ”€ô…µ•I•™•É•¹”¹•Ð ¤ì4(€€€€€€€€€€€€€É•ÑÕÉ¸ÉÕ¹Ñ¥µ”€ôô¹Õ±°€ü©…Ù„¹ÕÑ¥°¹=ÁÑ¥½¹…°¹•µÁÑä ¤€èÉÕ¹Ñ¥µ”¹…µ•½È¡Á±…å•É%¤ì4(€€€€€€€€€€€ô°4(€€€€€€€€€€€…µ•%€´øì4(€€€€€€€€€€€€€A…Á•É…µ•IÕ¹Ñ¥µ”ÉÕ¹Ñ¥µ”€ô…µ•I•™•É•¹”¹•Ð ¤ì4(€€€€€€€€€€€€€É•ÑÕÉ¸ÉÕ¹Ñ¥µ”€ôô¹Õ±°€ü©…Ù„¹ÕÑ¥°¹=ÁÑ¥½¹…°¹•µÁÑä ¤€èÉÕ¹Ñ¥µ”¹µ…Á½È¡…µ•%¤ì4(€€€€€€€€€€€ô°4(€€€€€€€€€€€ÁÕÉ¡…Í•Ì°4(€€€€€€€€€€€É•Ý…É‘Ì°4(€€€€€€€€€€€…µ•%€´ø4(€€€€€€€€€€€€€€€Á½Ý•ÉUÁÌ¹…Ñ¥Ù”¡…µ•%°™È¹¡•¹•É¥„¹é½µ‰¥”¹½É”¹Á½Ý•ÉÕÀ¹A½Ý•ÉUÁQåÁ”¹%9MQ}-%10¤°4(€€€€€€€€€€€€¡…µ•%°Á±…å•É%°…ÁÁ±¥•‘…µ…”°¡•…‘Í¡½Ð¤€´øì4(€€€€€€€€€€€€€A…Á•É…µ•IÕ¹Ñ¥µ”ÉÕ¹Ñ¥µ”€ô…µ•I•™•É•¹”¹•Ð ¤ì4(€€€€€€€€€€€€€¥˜€¡ÉÕ¹Ñ¥µ”€„ô¹Õ±°¤ì4(€€€€€€€€€€€€€€€ÉÕ¹Ñ¥µ”¹Ý•…Á½¹!¥Ð¡…µ•%°Á±…å•É%°…ÁÁ±¥•‘…µ…”°¡•…‘Í¡½Ð¤ì4(€€€€€€€€€€€€€ô4(€€€€€€€€€€€ô¤ì4(€€€Á…Á•ÉA½Ý•ÉUÁÌ¹Ý•…Á½¹Ì¡Ý•…Á½¹M•ÉÙ¥”¤ì4(€€€…µ•IÕ¹Ñ¥µ”€ô4(€€€€€€€¹•ÜA…Á•É…µ•IÕ¹Ñ¥µ” 4(€€€€€€€€€€€…µ•M•ÉÙ¥”°4(€€€€€€€€€€€µ…ÁI•¥ÍÑÉä°4(€€€€€€€€€€€½¹™¥ÕÉ…Ñ¥½¹Ì°4(€€€€€€€€€€€½½É‘¥¹…Ñ½È°4(€€€€€€€€€€€Í½É•‰½…É‘Ì°4(€€€€€€€€€€€é½µ‰¥•¹¥¹”°4(€€€€€€€€€€€Ý•…Á½¹M•ÉÙ¥”°4(€€€€€€€€€€€•½¹½µ¥•Ì°4(€€€€€€€€€€€ÑÉ…¹Í…Ñ¥½¹Ì°4(€€€€€€€€€€€ÁÕÉ¡…Í•Ì°4(€€€€€€€€€€€É•Ý…É‘Ì°4(€€€€€€€€€€€Á½Ý•ÉUÁÌ°4(€€€€€€€€€€€Á…Á•ÉA½Ý•ÉUÁÌ°4(€€€€€€€€€€€Á½¥¹Ñ¥ÍÁ±…ä°4(€€€€€€€€€€€µ…ÁY¥ÍÕ…±¥é…Ñ¥½¹Ì°4(€€€€€€€€€€€É•ÍÕ±Ð€´ø©…Ù„¹ÕÑ¥°¹½¹ÕÉÉ•¹Ð¹½µÁ±•Ñ…‰±•ÕÑÕÉ”¹½µÁ±•Ñ•‘ÕÑÕÉ”¡¹Õ±°¤°4(€€€€€€€€€€€µ•ÍÍ…•Ì°4(€€€€€€€€€€€Á±Õ¥¸¹•Ñ1½•È ¤¤ì4(€€€…µ•I•™•É•¹”¹Í•Ð¡…µ•IÕ¹Ñ¥µ”¤ì4(€€€¹•Ü]•…Á½¹Õ¥5½‘Õ±” 4(€€€€€€€€€€€Õ¥I•¥ÍÑÉä°Õ¥Ñ¥½¹Ì°Õ¥½¹™¥ÕÉ…Ñ¥½¹Ì°Õ¥M•ÉÙ¥”°Ý•…Á½¹M•ÉÙ¥”°…µ•IÕ¹Ñ¥µ”¤4(€€€€€€€€¹É•¥ÍÑ•È ¤ì4(€€€Õ¥½¹™¥ÕÉ…Ñ¥½¹Ì4(€€€€€€€€¹¥¹¥Ñ¥…±¥é•Íå¹Œ ¤4(€€€€€€€€¹Ý¡•¹½µÁ±•Ñ” 4(€€€€€€€€€€€€¡¥¹½É•°™…¥±ÕÉ”¤€´øì4(€€€€€€€€€€€€€¥˜€¡™…¥±ÕÉ”€„ô¹Õ±°¤ì4(€€€€€€€€€€€€€€€Á±Õ¥¸4(€€€€€€€€€€€€€€€€€€€€¹•Ñ1½•È ¤4(€€€€€€€€€€€€€€€€€€€€¹Í•Ù•É” 4(€€€€€€€€€€€€€€€€€€€€€€€€‰Õ¥Ì¹åµ°É•©•Ñ•ì‰Õ¹‘±•‘•™…Õ±ÑÌÉ•µ…¥¸…Ñ¥Ù”è€ˆ4(€€€€€€€€€€€€€€€€€€€€€€€€€€€€¬™…¥±ÕÉ”¹•Ñ5•ÍÍ…” ¤¤ì4(€€€€€€€€€€€€€ô4(€€€€€€€€€€€ô¤ì4(4(€€€É•¥ÍÑ•ÉM•ÉÙ¥•Ì 4(€€€€€€€½¹™¥ÕÉ…Ñ¥½¹Ì°4(€€€€€€€µ•ÍÍ…•Ì°4(€€€€€€€Ñ•µÁ±…Ñ•Ì°4(€€€€€€€Ý½É±‘Ì°4(€€€€€€€É•¥ÍÑÉä°4(€€€€€€€¥¹ÍÑ…¹•Ì°4(€€€€€€€Í•ÍÍ¥½¹Ì°4(€€€€€€€Á±…å•ÉMÑ…Ñ•Ì°4(€€€€€€€Í½É•‰½…É‘Ì°4(€€€€€€€±½‰‰ä°4(€€€€€€€Ù¥Í¥‰¥±¥Ñä°4(€€€€€€€…Õ‘¥•¹•Ì°4(€€€€€€€ÁÉ•Ù¥•ÝM•ÉÙ¥”¤ì4(€€€É•¥ÍÑ•É1¥ÍÑ•¹•ÉÌ 4(€€€€€€€¹•ÜA±…å•É½¹Ñ•áÑ1¥ÍÑ•¹•È 4(€€€€€€€€€€€½½É‘¥¹…Ñ½È°Í•ÍÍ¥½¹Ì°½¹™¥ÕÉ…Ñ¥½¹Ì°…Õ‘¥•¹•Ì°µ•ÍÍ…•Ì°…µ•IÕ¹Ñ¥µ”¤°4(€€€€€€€¹•Ü%Í½±…Ñ•‘¡…Ñ1¥ÍÑ•¹•È¡Í•ÍÍ¥½¹Ì°Ù¥Í¥‰¥±¥ÑåA½±¥ä°½¹™¥ÕÉ…Ñ¥½¹Ì°µ•ÍÍ…•Ì¤°4(€€€€€€€¹•Ü½¹Ñ•áÑ•…Ñ¡1¥ÍÑ•¹•È¡Í•ÍÍ¥½¹Ì°…Õ‘¥•¹•Ì¤°4(€€€€€€€¹•Ü%¹ÍÑ…¹•]½É±‘AÉ½Ñ•Ñ¥½¹1¥ÍÑ•¹•È¡Ý½É±‘Ì°Í•ÍÍ¥½¹Ì°½¹™¥ÕÉ…Ñ¥½¹Ì°ÁÉ•Ù¥•ÝM•ÉÙ¥”¤°4(€€€€€€€¹•Ü5…ÁAÉ•Ù¥•Ý1¥ÍÑ•¹•È¡ÁÉ•Ù¥•ÝM•ÉÙ¥”¤°4(€€€€€€€¹•ÜÕ¥1¥ÍÑ•¹•È¡Õ¥M•ÉÙ¥”¤°4(€€€€€€€¹•ÜÕ¥¡…Ñ%¹ÁÕÑ1¥ÍÑ•¹•È¡Á±Õ¥¸°Õ¥M•ÉÙ¥”¤°4(€€€€€€€¹•Ü…µ•½µ‰…Ñ1¥ÍÑ•¹•È¡…µ•IÕ¹Ñ¥µ”°é½µ‰¥•¹¥¹”°µ•ÍÍ…•Ì¤°4(€€€€€€€¹•Üi½µ‰¥•AÉ½Ñ•Ñ¥½¹1¥ÍÑ•¹•È¡é½µ‰¥•¹¥¹”¤°4(€€€€€€€¹•Ü]•…Á½¹1¥ÍÑ•¹•È¡Ý•…Á½¹M•ÉÙ¥”¤°4(€€€€€€€¹•Ü‘¥Ñ½ÉA±…•µ•¹Ñ1¥ÍÑ•¹•È 4(€€€€€€€€€€€•‘¥Ñ½ÉM•ÉÙ¥”°4(€€€€€€€€€€€•‘¥Ñ½É%Ñ•µÌ°4(€€€€€€€€€€€Õ¥M•ÉÙ¥”°4(€€€€€€€€€€€µ…ÁY¥ÍÕ…±¥é…Ñ¥½¹Ì°4(€€€€€€€€€€€±½¬¹ÍåÍÑ•µUQ ¤°4(€€€€€€€€€€€µ…¥¹Q¡É•…¤°4(€€€€€€€¹•Ü‘¥Ñ½ÉM•ÍÍ¥½¹1¥ÍÑ•¹•È¡•‘¥Ñ½ÉM•ÉÙ¥”°•‘¥Ñ½É%Ñ•µÌ°µ…ÁY¥ÍÕ…±¥é…Ñ¥½¹Ì¤¤ì4(€€€É•¥ÍÑ•É½µµ…¹ 4(€€€€€€€½¹™¥ÕÉ…Ñ¥½¹Ì°4(€€€€€€€µ•ÍÍ…•Ì°4(€€€€€€€Ñ•µÁ±…Ñ•Ì°4(€€€€€€€ÁÉ•Ù¥•ÝM•ÉÙ¥”°4(€€€€€€€Õ¥½¹™¥ÕÉ…Ñ¥½¹Ì°4(€€€€€€€Õ¥M•ÉÙ¥”°4(€€€€€€€µ…¥¹Q¡É•…¤ì4(€€€É•¥ÍÑ•ÉA±…å•ÉÕ¥½µµ…¹¡Õ¥M•ÉÙ¥”¤ì4(€€€É•¥ÍÑ•É5…Á‘¥Ñ½É½µµ…¹¡µ…ÁY…±¥‘…Ñ½È°µ…¥¹Q¡É•…¤ì4(€€€É•¥ÍÑ•É…µ•½µµ…¹¡…µ•IÕ¹Ñ¥µ”¤ì4(€€€É•¥ÍÑ•Éi½µ‰¥•¹¥¹•½µµ…¹¡é½µ‰¥•¹¥¹”°é½µ‰¥••™¥¹¥Ñ¥½¹Ì¤ì4(€€€É•¥ÍÑ•É]•…Á½¹½µµ…¹¡Ý•…Á½¹M•ÉÙ¥”°Ý•…Á½¹•™¥¹¥Ñ¥½¹Ì¤ì4(€€€É•¥ÍÑ•É½¹½µå½µµ…¹¡•½¹½µ¥•Ì°ÑÉ…¹Í…Ñ¥½¹Ì°Á½Ý•ÉUÁÌ°Á…Á•ÉA½Ý•ÉUÁÌ¤ì4(4(€€€µ…¥¹Ñ•¹…¹•Q…Í¬€ô4(€€€€€€€Á±Õ¥¸4(€€€€€€€€€€€€¹•ÑM•ÉÙ•È ¤4(€€€€€€€€€€€€¹•ÑM¡•‘Õ±•È ¤4(€€€€€€€€€€€€¹ÉÕ¹Q…Í­Q¥µ•È¡Á±Õ¥¸°½½É‘¥¹…Ñ½Èèé•áÁ¥É•I•½¹¹•ÑI•Í•ÉÙ…Ñ¥½¹Ì°€ÈÁ0°€ÈÁ0¤ì4(€€€…µ•Q…Í¬€ôÁ±Õ¥¸¹•ÑM•ÉÙ•È ¤¹•ÑM¡•‘Õ±•È ¤¹ÉÕ¹Q…Í­Q¥µ•È¡Á±Õ¥¸°…µ•IÕ¹Ñ¥µ”èéÑ¥¬°€Å0°€Å0¤ì4(€€€™½È€¡½Éœ¹‰Õ­­¥Ð¹•¹Ñ¥Ñä¹A±…å•ÈÁ±…å•È€èÁ±Õ¥¸¹•ÑM•ÉÙ•È ¤¹•Ñ=¹±¥¹•A±…å•ÉÌ ¤¤ì4(€€€€€½½É‘¥¹…Ñ½È¹½¹¹•Ð¡Á±…å•È¤ì4(€€€ô4(€€€Á±Õ¥¸4(€€€€€€€€¹•ÑM•ÉÙ•È ¤4(€€€€€€€€¹•ÑM•ÉÙ¥•Í5…¹…•È ¤4(€€€€€€€€¹É•¥ÍÑ•È¡i½µ‰¥•Á¤¹±…ÍÌ°…Á¤°Á±Õ¥¸°M•ÉÙ¥•AÉ¥½É¥Ñä¹9½Éµ…°¤ì4(€€€Á±Õ¥¸¹•Ñ1½•È ¤¹¥¹™¼ ‰1½‰‰ä…¹¥Í½±…Ñ•µ¥¹ÍÑ…¹”ÉÕ¹Ñ¥µ”¥¹¥Ñ¥…±¥é•¸ˆ¤ì4(€ô4(4(€€¼¨¨I•±•…Í•ÌÑ…Í­Ì°Á±…å•ÉÌ°Ý½É±‘Ì°A$É•¥ÍÑÉ…Ñ¥½¸…¹•á•ÕÑ½ÈÑ¡É•…‘Ì¸€¨¼4(€ÁÕ‰±¥ŒÙ½¥ÍÑ½À ¤ì4(€€€¥˜€¡•‘¥Ñ½ÉM•ÉÙ¥”€„ô¹Õ±°¤ì4(€€€€€™½È€¡½Éœ¹‰Õ­­¥Ð¹•¹Ñ¥Ñä¹A±…å•ÈÁ±…å•È€èÁ±Õ¥¸¹•ÑM•ÉÙ•È ¤¹•Ñ=¹±¥¹•A±…å•ÉÌ ¤¤ì4(€€€€€€€•‘¥Ñ½ÉM•ÉÙ¥”¹±•…Ù”¡Á±…å•È¹•ÑU¹¥ÅÕ•% ¤¤ì4(€€€€€€€¥˜€¡•‘¥Ñ½É%Ñ•µÌ€„ô¹Õ±°¤ì4(€€€€€€€€€•‘¥Ñ½É%Ñ•µÌ¹É•µ½Ù”¡Á±…å•È¤ì4(€€€€€€€ô4(€€€€€ô4(€€€€€•‘¥Ñ½ÉM•ÉÙ¥”€ô¹Õ±°ì4(€€€€€•‘¥Ñ½É%Ñ•µÌ€ô¹Õ±°ì4(€€€€€¥˜€¡µ…ÁY¥ÍÕ…±¥é…Ñ¥½¹Ì€„ô¹Õ±°¤ì4(€€€€€€€µ…ÁY¥ÍÕ…±¥é…Ñ¥½¹Ì¹±•…É±° ¤ì4(€€€€€€€µ…ÁY¥ÍÕ…±¥é…Ñ¥½¹Ì€ô¹Õ±°ì4(€€€€€ô4(€€€ô4(€€€¥˜€¡Õ¥M•ÉÙ¥”€„ô¹Õ±°¤ì4(€€€€€Õ¥M•ÉÙ¥”¹Í¡ÕÑ‘½Ý¸ ¤ì4(€€€€€Õ¥M•ÉÙ¥”€ô¹Õ±°ì4(€€€ô4(€€€¥˜€¡µ…¥¹Ñ•¹…¹•Q…Í¬€„ô¹Õ±°¤ì4(€€€€€µ…¥¹Ñ•¹…¹•Q…Í¬¹…¹•° ¤ì4(€€€€€µ…¥¹Ñ•¹…¹•Q…Í¬€ô¹Õ±°ì4(€€€ô4(€€€¥˜€¡…µ•Q…Í¬€„ô¹Õ±°¤ì4(€€€€€…µ•Q…Í¬¹…¹•° ¤ì4(€€€€€…µ•Q…Í¬€ô¹Õ±°ì4(€€€ô4(€€€¥˜€¡…µ•IÕ¹Ñ¥µ”€„ô¹Õ±°¤ì4(€€€€€…µ•IÕ¹Ñ¥µ”¹Í¡ÕÑ‘½Ý¸ ¤ì4(€€€€€…µ•IÕ¹Ñ¥µ”€ô¹Õ±°ì4(€€€ô4(€€€¥˜€¡½½É‘¥¹…Ñ½È€„ô¹Õ±°¤ì4(€€€€€¥˜€¡ÁÉ•Ù¥•ÝM•ÉÙ¥”€„ô¹Õ±°¤ì4(€€€€€€€ÁÉ•Ù¥•ÝM•ÉÙ¥”¹±•…È ¤ì4(€€€€€€€ÁÉ•Ù¥•ÝM•ÉÙ¥”€ô¹Õ±°ì4(€€€€€ô4(€€€€€1¥ÍÐñMÑÉ¥¹œø™…¥±ÕÉ•Ì€ô½½É‘¥¹…Ñ½È¹Í¡ÕÑ‘½Ý¸ ¤ì4(€€€€€™…¥±ÕÉ•Ì¹™½É…  4(€€€€€€€€€Ý½É±€´øÁ±Õ¥¸¹•Ñ1½•È ¤¹Ý…É¹¥¹œ ‰]½É±½Õ±¹½Ð‰”Õ¹±½…‘•è€ˆ€¬Ý½É±¤¤ì4(€€€€€½½É‘¥¹…Ñ½È€ô¹Õ±°ì4(€€€ô4(€€€¥˜€¡…Á¤€„ô¹Õ±°¤ì4(€€€€€Á±Õ¥¸¹•ÑM•ÉÙ•È ¤¹•ÑM•ÉÙ¥•Í5…¹…•È ¤¹Õ¹É•¥ÍÑ•È¡i½µ‰¥•Á¤¹±…ÍÌ°…Á¤¤ì4(€€€€€…Á¤€ô¹Õ±°ì4(€€€ô4(€€€Í•ÉÙ¥•Ì¹±•…È ¤ì4(€€€¥˜€¡¥½á•ÕÑ½È€„ô¹Õ±°¤ì4(€€€€€¥½á•ÕÑ½È¹Í¡ÕÑ‘½Ý¸ ¤ì4(€€€€€ÑÉäì4(€€€€€€€¥˜€ …¥½á•ÕÑ½È¹…Ý…¥ÑQ•Éµ¥¹…Ñ¥½¸ È°Q¥µ•U¹¥Ð¹M=9L¤¤ì4(€€€€€€€€€¥½á•ÕÑ½È¹Í¡ÕÑ‘½Ý¹9½Ü ¤ì4(€€€€€€€ô4(€€€€€ô…Ñ €¡%¹Ñ•ÉÉÕÁÑ•‘á•ÁÑ¥½¸¥¹Ñ•ÉÉÕÁÑ•¤ì4(€€€€€€€¥½á•ÕÑ½È¹Í¡ÕÑ‘½Ý¹9½Ü ¤ì4(€€€€€€€Q¡É•…¹ÕÉÉ•¹ÑQ¡É•… ¤¹¥¹Ñ•ÉÉÕÁÐ ¤ì4(€€€€€ô4(€€€€€¥½á•ÕÑ½È€ô¹Õ±°ì4(€€€ô4(€ô4(4(€ÁÉ¥Ù…Ñ”Ù½¥É•¥ÍÑ•ÉM•ÉÙ¥•Ì 4(€€€€€½¹™¥ÕÉ…Ñ¥½¹5…¹…•È½¹™¥ÕÉ…Ñ¥½¹Ì°4(€€€€€5•ÍÍ…•M•ÉÙ¥”µ•ÍÍ…•Ì°4(€€€€€5…ÁQ•µÁ±…Ñ•…Ñ…±½œÑ•µÁ±…Ñ•Ì°4(€€€€€A…Á•É]½É±‘%¹ÍÑ…¹•M•ÉÙ¥”Ý½É±‘Ì°4(€€€€€…µ•%¹ÍÑ…¹•I•¥ÍÑÉäÉ•¥ÍÑÉä°4(€€€€€…µ•%¹ÍÑ…¹•M•ÉÙ¥”¥¹ÍÑ…¹•Ì°4(€€€€€A±…å•ÉM•ÍÍ¥½¹M•ÉÙ¥”Í•ÍÍ¥½¹Ì°4(€€€€€A…Á•ÉA±…å•ÉMÑ…Ñ•M•ÉÙ¥”Á±…å•ÉMÑ…Ñ•Ì°4(€€€€€½¹Ñ•áÑM½É•‰½…É‘M•ÉÙ¥”Í½É•‰½…É‘Ì°4(€€€€€1½‰‰åM•ÉÙ¥”±½‰‰ä°4(€€€€€Y¥Í¥‰¥±¥ÑåM•ÉÙ¥”Ù¥Í¥‰¥±¥Ñä°4(€€€€€A…Á•ÉÕ‘¥•¹•M•ÉÙ¥”…Õ‘¥•¹•Ì°4(€€€€€5…ÁAÉ•Ù¥•ÝM•ÉÙ¥”ÁÉ•Ù¥•ÝÌ¤ì4(€€€Í•ÉÙ¥•Ì¹É•¥ÍÑ•È¡½¹™¥ÕÉ…Ñ¥½¹5…¹…•È¹±…ÍÌ°½¹™¥ÕÉ…Ñ¥½¹Ì¤ì4(€€€Í•ÉÙ¥•Ì¹É•¥ÍÑ•È¡5•ÍÍ…•M•ÉÙ¥”¹±…ÍÌ°µ•ÍÍ…•Ì¤ì4(€€€Í•ÉÙ¥•Ì¹É•¥ÍÑ•È¡5…ÁQ•µÁ±…Ñ•…Ñ…±½œ¹±…ÍÌ°Ñ•µÁ±…Ñ•Ì¤ì4(€€€Í•ÉÙ¥•Ì¹É•¥ÍÑ•È¡A…Á•É]½É±‘%¹ÍÑ…¹•M•ÉÙ¥”¹±…ÍÌ°Ý½É±‘Ì¤ì4(€€€Í•ÉÙ¥•Ì¹É•¥ÍÑ•È¡…µ•%¹ÍÑ…¹•I•¥ÍÑÉä¹±…ÍÌ°É•¥ÍÑÉä¤ì4(€€€Í•ÉÙ¥•Ì¹É•¥ÍÑ•È¡…µ•%¹ÍÑ…¹•M•ÉÙ¥”¹±…ÍÌ°¥¹ÍÑ…¹•Ì¤ì4(€€€Í•ÉÙ¥•Ì¹É•¥ÍÑ•È¡A±…å•ÉM•ÍÍ¥½¹M•ÉÙ¥”¹±…ÍÌ°Í•ÍÍ¥½¹Ì¤ì4(€€€Í•ÉÙ¥•Ì¹É•¥ÍÑ•È¡A…Á•ÉA±…å•ÉMÑ…Ñ•M•ÉÙ¥”¹±…ÍÌ°Á±…å•ÉMÑ…Ñ•Ì¤ì4(€€€Í•ÉÙ¥•Ì¹É•¥ÍÑ•È¡½¹Ñ•áÑM½É•‰½…É‘M•ÉÙ¥”¹±…ÍÌ°Í½É•‰½…É‘Ì¤ì4(€€€Í•ÉÙ¥•Ì¹É•¥ÍÑ•È¡1½‰‰åM•ÉÙ¥”¹±…ÍÌ°±½‰‰ä¤ì4(€€€Í•ÉÙ¥•Ì¹É•¥ÍÑ•È¡Y¥Í¥‰¥±¥ÑåM•ÉÙ¥”¹±…ÍÌ°Ù¥Í¥‰¥±¥Ñä¤ì4(€€€Í•ÉÙ¥•Ì¹É•¥ÍÑ•È¡A…Á•ÉÕ‘¥•¹•M•ÉÙ¥”¹±…ÍÌ°…Õ‘¥•¹•Ì¤ì4(€€€Í•ÉÙ¥•Ì¹É•¥ÍÑ•È¡5…ÁAÉ•Ù¥•ÝM•ÉÙ¥”¹±…ÍÌ°ÁÉ•Ù¥•ÝÌ¤ì4(€€€Í•ÉÙ¥•Ì¹É•¥ÍÑ•È¡%¹ÍÑ…¹•½½É‘¥¹…Ñ½È¹±…ÍÌ°½½É‘¥¹…Ñ½È¤ì4(€€€Í•ÉÙ¥•Ì¹É•¥ÍÑ•È¡i½µ‰¥•Á¤¹±…ÍÌ°…Á¤¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”Ù½¥É•¥ÍÑ•É1¥ÍÑ•¹•ÉÌ¡1¥ÍÑ•¹•È¸¸¸±¥ÍÑ•¹•ÉÌ¤ì4(€€€™½È€¡1¥ÍÑ•¹•È±¥ÍÑ•¹•È€è±¥ÍÑ•¹•ÉÌ¤ì4(€€€€€Á±Õ¥¸¹•ÑM•ÉÙ•È ¤¹•ÑA±Õ¥¹5…¹…•È ¤¹É•¥ÍÑ•ÉÙ•¹ÑÌ¡±¥ÍÑ•¹•È°Á±Õ¥¸¤ì4(€€€ô4(€ô4(4(€ÁÉ¥Ù…Ñ”Ù½¥É•¥ÍÑ•É½µµ…¹ 4(€€€€€½¹™¥ÕÉ…Ñ¥½¹5…¹…•È½¹™¥ÕÉ…Ñ¥½¹Ì°4(€€€€€5•ÍÍ…•M•ÉÙ¥”µ•ÍÍ…•Ì°4(€€€€€5…ÁQ•µÁ±…Ñ•…Ñ…±½œÑ•µÁ±…Ñ•Ì°4(€€€€€5…ÁAÉ•Ù¥•ÝM•ÉÙ¥”ÁÉ•Ù¥•ÝÌ°4(€€€€€Õ¥½¹™¥ÕÉ…Ñ¥½¹M•ÉÙ¥”Õ¥½¹™¥ÕÉ…Ñ¥½¹Ì°4(€€€€€Õ¥M•ÉÙ¥”Õ¥M•ÉÙ¥”°4(€€€€€A…Á•É5…¥¹Q¡É•…‘á•ÕÑ½Èµ…¥¹Q¡É•…¤ì4(€€€A±Õ¥¹½µµ…¹½µµ…¹€ôÁ±Õ¥¸¹•Ñ½µµ…¹ ‰é½µ‰¥”ˆ¤ì4(€€€¥˜€¡½µµ…¹€ôô¹Õ±°¤ì4(€€€€€Ñ¡É½Ü¹•Ü%±±•…±MÑ…Ñ•á•ÁÑ¥½¸ ‰½µµ…¹é½µ‰¥”¥Ìµ¥ÍÍ¥¹œ™É½´Á±Õ¥¸¹åµ°ˆ¤ì4(€€€ô4(€€€i½µ‰¥•½µµ…¹•á•ÕÑ½È€ô4(€€€€€€€¹•Üi½µ‰¥•½µµ…¹ 4(€€€€€€€€€€€Á±Õ¥¸¹•ÑA±Õ¥¹5•Ñ„ ¤¹•ÑY•ÉÍ¥½¸ ¤°4(€€€€€€€€€€€…Á¤°4(€€€€€€€€€€€ÍÑ…Ñ”°4(€€€€€€€€€€€½¹™¥ÕÉ…Ñ¥½¹Ì°4(€€€€€€€€€€€µ•ÍÍ…•Ì°4(€€€€€€€€€€€½½É‘¥¹…Ñ½È°4(€€€€€€€€€€€Ñ•µÁ±…Ñ•Ì°4(€€€€€€€€€€€ÁÉ•Ù¥•ÝÌ°4(€€€€€€€€€€€Õ¥½¹™¥ÕÉ…Ñ¥½¹Ì°4(€€€€€€€€€€€Õ¥M•ÉÙ¥”°4(€€€€€€€€€€€…µ•IÕ¹Ñ¥µ”°4(€€€€€€€€€€€µ…¥¹Q¡É•…¤ì4(€€€½µµ…¹¹Í•Ñá•ÕÑ½È¡•á•ÕÑ½È¤ì4(€€€½µµ…¹¹Í•ÑQ…‰½µÁ±•Ñ•È¡•á•ÕÑ½È¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”Ù½¥É•¥ÍÑ•ÉA±…å•ÉÕ¥½µµ…¹¡Õ¥M•ÉÙ¥”Õ¥M•ÉÙ¥”¤ì4(€€€A±Õ¥¹½µµ…¹½µµ…¹€ôÁ±Õ¥¸¹•Ñ½µµ…¹ ‰é½µ‰¥•Ìˆ¤ì4(€€€¥˜€¡½µµ…¹€ôô¹Õ±°¤ì4(€€€€€Ñ¡É½Ü¹•Ü%±±•…±MÑ…Ñ•á•ÁÑ¥½¸ ‰½µµ…¹é½µ‰¥•Ì¥Ìµ¥ÍÍ¥¹œ™É½´Á±Õ¥¸¹åµ°ˆ¤ì4(€€€ô4(€€€½µµ…¹¹Í•Ñá•ÕÑ½È 4(€€€€€€€€¡Í•¹‘•È°¥¹½É•°±…‰•°°…ÉÕµ•¹ÑÌ¤€´øì4(€€€€€€€€€¥˜€ „¡Í•¹‘•È¥¹ÍÑ…¹•½˜½Éœ¹‰Õ­­¥Ð¹•¹Ñ¥Ñä¹A±…å•ÈÁ±…å•È¤¤ì4(€€€€€€€€€€€Í•¹‘•È¹Í•¹‘5•ÍÍ…” ‰•ÑÑ”½µµ…¹‘”‘½¥Ðƒ©ÑÉ”ÕÑ¥±¥Ï¥”•¸©•Ô¸ˆ¤ì4(€€€€€€€€€ô•±Í”¥˜€ …Á±…å•È¹¡…ÍA•Éµ¥ÍÍ¥½¸ ‰é½µ‰¥”¹Õ¤¹Á±…å•Èˆ¤¤ì4(€€€€€€€€€€€Á±…å•È¹Í•¹‘5•ÍÍ…” 4(€€€€€€€€€€€€€€€¹•Ð¹­å½É¤¹…‘Ù•¹ÑÕÉ”¹Ñ•áÐ¹µ¥¹¥µ•ÍÍ…”¹5¥¹¥5•ÍÍ…”¹µ¥¹¥5•ÍÍ…” ¤4(€€€€€€€€€€€€€€€€€€€€¹‘•Í•É¥…±¥é” ˆñÉ•ùY½ÕÌ¸…Ù•èÁ…Ì±„Á•Éµ¥ÍÍ¥½¸¸ˆ¤¤ì4(€€€€€€€€€ô•±Í”ì4(€€€€€€€€€€€Õ¥M•ÉÙ¥”¹½Á•¹!½µ”¡Á±…å•È°¹•Ü™È¹¡•¹•É¥„¹é½µ‰¥”¹Á±Õ¥¸¹Õ¤¹Õ¥% ‰Á±…å•Èµµ…¥¸ˆ¤¤ì4(€€€€€€€€€ô4(€€€€€€€€€É•ÑÕÉ¸ÑÉÕ”ì4(€€€€€€€ô¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”Ù½¥É•¥ÍÑ•É5…Á‘¥Ñ½É½µµ…¹ 4(€€€€€5…ÁY…±¥‘…Ñ½ÈÙ…±¥‘…Ñ½È°A…Á•É5…¥¹Q¡É•…‘á•ÕÑ½Èµ…¥¹Q¡É•…¤ì4(€€€A±Õ¥¹½µµ…¹½µµ…¹€ôÁ±Õ¥¸¹•Ñ½µµ…¹ ‰éµ…Àˆ¤ì4(€€€¥˜€¡½µµ…¹€ôô¹Õ±°¤ì4(€€€€€Ñ¡É½Ü¹•Ü%±±•…±MÑ…Ñ•á•ÁÑ¥½¸ ‰½µµ…¹éµ…À¥Ìµ¥ÍÍ¥¹œ™É½´Á±Õ¥¸¹åµ°ˆ¤ì4(€€€ô4(€€€i5…Á½µµ…¹•á•ÕÑ½È€ô4(€€€€€€€¹•Üi5…Á½µµ…¹ 4(€€€€€€€€€€€•‘¥Ñ½ÉM•ÉÙ¥”°4(€€€€€€€€€€€Ù…±¥‘…Ñ½È°4(€€€€€€€€€€€•‘¥Ñ½É%Ñ•µÌ°4(€€€€€€€€€€€Õ¥M•ÉÙ¥”°4(€€€€€€€€€€€½½É‘¥¹…Ñ½È°4(€€€€€€€€€€€ÁÉ•Ù¥•ÝM•ÉÙ¥”°4(€€€€€€€€€€€…µ•IÕ¹Ñ¥µ”°4(€€€€€€€€€€€µ…ÁY¥ÍÕ…±¥é…Ñ¥½¹Ì°4(€€€€€€€€€€€µ…¥¹Q¡É•…¤ì4(€€€½µµ…¹¹Í•Ñá•ÕÑ½È¡•á•ÕÑ½È¤ì4(€€€½µµ…¹¹Í•ÑQ…‰½µÁ±•Ñ•È¡•á•ÕÑ½È¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”Ù½¥É•¥ÍÑ•É…µ•½µµ…¹¡A…Á•É…µ•IÕ¹Ñ¥µ”…µ•Ì¤ì4(€€€A±Õ¥¹½µµ…¹½µµ…¹€ôÁ±Õ¥¸¹•Ñ½µµ…¹ ‰é…µ”ˆ¤ì4(€€€¥˜€¡½µµ…¹€ôô¹Õ±°¤ì4(€€€€€Ñ¡É½Ü¹•Ü%±±•…±MÑ…Ñ•á•ÁÑ¥½¸ ‰½µµ…¹é…µ”¥Ìµ¥ÍÍ¥¹œ™É½´Á±Õ¥¸¹åµ°ˆ¤ì4(€€€ô4(€€€i…µ•½µµ…¹•á•ÕÑ½È€ô¹•Üi…µ•½µµ…¹¡…µ•Ì°½½É‘¥¹…Ñ½È¤ì4(€€€½µµ…¹¹Í•Ñá•ÕÑ½È¡•á•ÕÑ½È¤ì4(€€€½µµ…¹¹Í•ÑQ…‰½µÁ±•Ñ•È¡•á•ÕÑ½È¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”Ù½¥É•¥ÍÑ•Éi½µ‰¥•¹¥¹•½µµ…¹ 4(€€€€€A…Á•Éi½µ‰¥•¹¥¹”•¹¥¹”°i½µ‰¥••™¥¹¥Ñ¥½¹1½…‘•È‘•™¥¹¥Ñ¥½¹Ì¤ì4(€€€A±Õ¥¹½µµ…¹½µµ…¹€ôÁ±Õ¥¸¹•Ñ½µµ…¹ ‰éé½µ‰¥”ˆ¤ì4(€€€¥˜€¡½µµ…¹€ôô¹Õ±°¤ì4(€€€€€Ñ¡É½Ü¹•Ü%±±•…±MÑ…Ñ•á•ÁÑ¥½¸ ‰½µµ…¹éé½µ‰¥”¥Ìµ¥ÍÍ¥¹œ™É½´Á±Õ¥¸¹åµ°ˆ¤ì4(€€€ô4(€€€ii½µ‰¥•½µµ…¹•á•ÕÑ½È€ô4(€€€€€€€¹•Üii½µ‰¥•½µµ…¹¡•¹¥¹”°‘•™¥¹¥Ñ¥½¹Ì°½½É‘¥¹…Ñ½È°…µ•IÕ¹Ñ¥µ”°Á±Õ¥¸¤ì4(€€€½µµ…¹¹Í•Ñá•ÕÑ½È¡•á•ÕÑ½È¤ì4(€€€½µµ…¹¹Í•ÑQ…‰½µÁ±•Ñ•È¡•á•ÕÑ½È¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”Ù½¥É•¥ÍÑ•É]•…Á½¹½µµ…¹ 4(€€€€€A…Á•É]•…Á½¹M•ÉÙ¥”Ý•…Á½¹Ì°]•…Á½¹•™¥¹¥Ñ¥½¹1½…‘•È‘•™¥¹¥Ñ¥½¹Ì¤ì4(€€€A±Õ¥¹½µµ…¹½µµ…¹€ôÁ±Õ¥¸¹•Ñ½µµ…¹ ‰éÝ•…Á½¸ˆ¤ì4(€€€¥˜€¡½µµ…¹€ôô¹Õ±°¤ì4(€€€€€Ñ¡É½Ü¹•Ü%±±•…±MÑ…Ñ•á•ÁÑ¥½¸ ‰½µµ…¹éÝ•…Á½¸¥Ìµ¥ÍÍ¥¹œ™É½´Á±Õ¥¸¹åµ°ˆ¤ì4(€€€ô4(€€€i]•…Á½¹½µµ…¹•á•ÕÑ½È€ô4(€€€€€€€¹•Üi]•…Á½¹½µµ…¹¡Á±Õ¥¸°Ý•…Á½¹Ì°‘•™¥¹¥Ñ¥½¹Ì°…µ•IÕ¹Ñ¥µ”°Õ¥M•ÉÙ¥”¤ì4(€€€½µµ…¹¹Í•Ñá•ÕÑ½È¡•á•ÕÑ½È¤ì4(€€€½µµ…¹¹Í•ÑQ…‰½µÁ±•Ñ•È¡•á•ÕÑ½È¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”Ù½¥É•¥ÍÑ•É½¹½µå½µµ…¹ 4(€€€€€½¹½µåM•ÉÙ¥”•½¹½µ¥•Ì°4(€€€€€QÉ…¹Í…Ñ¥½¹M•ÉÙ¥”ÑÉ…¹Í…Ñ¥½¹Ì°4(€€€€€A½Ý•ÉUÁM•ÉÙ¥”Á½Ý•ÉUÁÌ°4(€€€€€A…Á•ÉA½Ý•ÉUÁM•ÉÙ¥”Á…Á•ÉA½Ý•ÉUÁÌ¤ì4(€€€A±Õ¥¹½µµ…¹½µµ…¹€ôÁ±Õ¥¸¹•Ñ½µµ…¹ ‰é•½¹½µäˆ¤ì4(€€€¥˜€¡½µµ…¹€ôô¹Õ±°¤ì4(€€€€€Ñ¡É½Ü¹•Ü%±±•…±MÑ…Ñ•á•ÁÑ¥½¸ ‰½µµ…¹é•½¹½µä¥Ìµ¥ÍÍ¥¹œ™É½´Á±Õ¥¸¹åµ°ˆ¤ì4(€€€ô4(€€€i½¹½µå½µµ…¹•á•ÕÑ½È€ô4(€€€€€€€¹•Üi½¹½µå½µµ…¹¡…µ•IÕ¹Ñ¥µ”°•½¹½µ¥•Ì°ÑÉ…¹Í…Ñ¥½¹Ì°Á½Ý•ÉUÁÌ°Á…Á•ÉA½Ý•ÉUÁÌ¤ì4(€€€½µµ…¹¹Í•Ñá•ÕÑ½È¡•á•ÕÑ½È¤ì4(€€€½µµ…¹¹Í•ÑQ…‰½µÁ±•Ñ•È¡•á•ÕÑ½È¤ì4(€ô4)ô4(