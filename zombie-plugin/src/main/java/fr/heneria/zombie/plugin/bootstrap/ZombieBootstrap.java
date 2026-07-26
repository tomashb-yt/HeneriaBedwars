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
            guiConfigurations,
            guiService,
            editorService,
            mapValidator,
            editorItems,
            mainThread,
            Clock.systemUTC())
        .register();
    guiConfigurations
        .initializeAsync()
        .whenComplete(
            (ignored, failure) -> {
              if (failure != null) {
                plugin
                    .getLogger()
                    .severe(
                        "guis.yml rejected; bundled defaults remain active: "
                            + failure.getMessage());
              }
            });

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
    ZombieDefinitionLoader zombieDefinitions = new ZombieDefinitionLoader(plugin, ioExecutor);
    zombieDefinitions
        .initializeAsync()
        .whenComplete(
            (loaded, failure) -> {
              if (failure == null) {
                plugin
                    .getLogger()
                    .info(
                        "Loaded "
                            + loaded.size()
                            + " zombie type definition(s) from "
                            + zombieDefinitions.directory());
              } else {
                plugin
                    .getLogger()
                    .severe(
                        "External zombie definitions rejected; last valid snapshot remains active: "
                            + failure.getMessage());
              }
            });
    AtomicReference<PaperGameRuntime> gameReference = new AtomicReference<>();
    PaperZombieEngine zombieEngine =
        new PaperZombieEngine(
            plugin,
            zombieDefinitions,
            gameId -> {
              PaperGameRuntime runtime = gameReference.get();
              return runtime == null ? List.of() : runtime.playerIds(gameId);
            },
            (gameId, playerId) -> {
              PaperGameRuntime runtime = gameReference.get();
              return runtime != null && runtime.isTargetable(gameId, playerId);
            },
            (source, target, amount) -> {
              PaperGameRuntime runtime = gameReference.get();
              if (runtime != null) {
                runtime.damagePlayer(source.gameId(), target, amount);
              }
            },
            (zombie, reason, killerId, reward) -> {
              PaperGameRuntime runtime = gameReference.get();
              if (runtime != null && reason.completesRoundSlot()) {
                runtime.zombieRemoved(zombie.entityId(), killerId, reward);
              }
            });
    gameRuntime =
        new PaperGameRuntime(
            gameService,
            mapRegistry,
            configurations,
            coordinator,
            scoreboards,
            zombieEngine,
            result -> java.util.concurrent.CompletableFuture.completedFuture(null),
            messages,
            plugin.getLogger());
    gameReference.set(gameRuntime);

    registerServices(
        configurations,
        messages,
        templates,
        worlds,
        registry,
        instances,
        sessions,
        playerStates,
        scoreboards,
        lobby,
        visibility,
        audiences,
        previewService);
    registerListeners(
        new PlayerContextListener(
            coordinator, sessions, configurations, audiences, messages, gameRuntime),
        new IsolatedChatListener(sessions, visibilityPolicy, configurations, messages),
        new ContextDeathListener(sessions, audiences),
        new InstanceWorldProtectionListener(worlds, sessions, configurations, previewService),
        new MapPreviewListener(previewService),
        new GuiListener(guiService),
        new GuiChatInputListener(plugin, guiService),
        new GameCombatListener(gameRuntime, zombieEngine, messages),
        new ZombieProtectionListener(zombieEngine),
        new EditorPlacementListener(
            editorService, editorItems, guiService, Clock.systemUTC(), mainThread),
        new EditorSessionListener(editorService, editorItems));
    registerCommand(
        configurations,
        messages,
        templates,
        previewService,
        guiConfigurations,
        guiService,
        mainThread);
    registerPlayerGuiCommand(guiService);
    registerMapEditorCommand(mapValidator, mainThread);
    registerGameCommand(gameRuntime);
    registerZombieEngineCommand(zombieEngine, zombieDefinitions);

    maintenanceTask =
        plugin
            .getServer()
            .getScheduler()
            .runTaskTimer(plugin, coordinator::expireReconnectReservations, 20L, 20L);
    gameTask = plugin.getServer().getScheduler().runTaskTimer(plugin, gameRuntime::tick, 1L, 1L);
    for (org.bukkit.entity.Player player : plugin.getServer().getOnlinePlayers()) {
      coordinator.connect(player);
    }
    plugin
        .getServer()
        .getServicesManager()
        .register(ZombieApi.class, api, plugin, ServicePriority.Normal);
    plugin.getLogger().info("Lobby and isolated-instance runtime initialized.");
  }

  /** Releases tasks, players, worlds, API registration and executor threads. */
  public void stop() {
    if (editorService != null) {
      for (org.bukkit.entity.Player player : plugin.getServer().getOnlinePlayers()) {
        editorService.leave(player.getUniqueId());
        if (editorItems != null) {
          editorItems.remove(player);
        }
      }
      editorService = null;
      editorItems = null;
    }
    if (guiService != null) {
      guiService.shutdown();
      guiService = null;
    }
    if (maintenanceTask != null) {
      maintenanceTask.cancel();
      maintenanceTask = null;
    }
    if (gameTask != null) {
      gameTask.cancel();
      gameTask = null;
    }
    if (gameRuntime != null) {
      gameRuntime.shutdown();
      gameRuntime = null;
    }
    if (coordinator != null) {
      if (previewService != null) {
        previewService.clear();
        previewService = null;
      }
      List<String> failures = coordinator.shutdown();
      failures.forEach(
          world -> plugin.getLogger().warning("World could not be unloaded: " + world));
      coordinator = null;
    }
    if (api != null) {
      plugin.getServer().getServicesManager().unregister(ZombieApi.class, api);
      api = null;
    }
    services.clear();
    if (ioExecutor != null) {
      ioExecutor.shutdown();
      try {
        if (!ioExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
          ioExecutor.shutdownNow();
        }
      } catch (InterruptedException interrupted) {
        ioExecutor.shutdownNow();
        Thread.currentThread().interrupt();
      }
      ioExecutor = null;
    }
  }

  private void registerServices(
      ConfigurationManager configurations,
      MessageService messages,
      MapTemplateCatalog templates,
      PaperWorldInstanceService worlds,
      GameInstanceRegistry registry,
      GameInstanceService instances,
      PlayerSessionService sessions,
      PaperPlayerStateService playerStates,
      ContextScoreboardService scoreboards,
      LobbyService lobby,
      VisibilityService visibility,
      PaperAudienceService audiences,
      MapPreviewService previews) {
    services.register(ConfigurationManager.class, configurations);
    services.register(MessageService.class, messages);
    services.register(MapTemplateCatalog.class, templates);
    services.register(PaperWorldInstanceService.class, worlds);
    services.register(GameInstanceRegistry.class, registry);
    services.register(GameInstanceService.class, instances);
    services.register(PlayerSessionService.class, sessions);
    services.register(PaperPlayerStateService.class, playerStates);
    services.register(ContextScoreboardService.class, scoreboards);
    services.register(LobbyService.class, lobby);
    services.register(VisibilityService.class, visibility);
    services.register(PaperAudienceService.class, audiences);
    services.register(MapPreviewService.class, previews);
    services.register(InstanceCoordinator.class, coordinator);
    services.register(ZombieApi.class, api);
  }

  private void registerListeners(Listener... listeners) {
    for (Listener listener : listeners) {
      plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }
  }

  private void registerCommand(
      ConfigurationManager configurations,
      MessageService messages,
      MapTemplateCatalog templates,
      MapPreviewService previews,
      GuiConfigurationService guiConfigurations,
      GuiService guiService,
      PaperMainThreadExecutor mainThread) {
    PluginCommand command = plugin.getCommand("zombie");
    if (command == null) {
      throw new IllegalStateException("Command zombie is missing from plugin.yml");
    }
    ZombieCommand executor =
        new ZombieCommand(
            plugin.getPluginMeta().getVersion(),
            api,
            state,
            configurations,
            messages,
            coordinator,
            templates,
            previews,
            guiConfigurations,
            guiService,
            gameRuntime,
            mainThread);
    command.setExecutor(executor);
    command.setTabCompleter(executor);
  }

  private void registerPlayerGuiCommand(GuiService guiService) {
    PluginCommand command = plugin.getCommand("zombies");
    if (command == null) {
      throw new IllegalStateException("Command zombies is missing from plugin.yml");
    }
    command.setExecutor(
        (sender, ignored, label, arguments) -> {
          if (!(sender instanceof org.bukkit.entity.Player player)) {
            sender.sendMessage("Cette commande doit être utilisée en jeu.");
          } else if (!player.hasPermission("zombie.gui.player")) {
            player.sendMessage(
                net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                    .deserialize("<red>Vous n'avez pas la permission."));
          } else {
            guiService.openHome(player, new fr.heneria.zombie.plugin.gui.GuiId("player-main"));
          }
          return true;
        });
  }

  private void registerMapEditorCommand(
      MapValidator validator, PaperMainThreadExecutor mainThread) {
    PluginCommand command = plugin.getCommand("zmap");
    if (command == null) {
      throw new IllegalStateException("Command zmap is missing from plugin.yml");
    }
    ZMapCommand executor =
        new ZMapCommand(
            editorService,
            validator,
            editorItems,
            guiService,
            coordinator,
            previewService,
            gameRuntime,
            mainThread);
    command.setExecutor(executor);
    command.setTabCompleter(executor);
  }

  private void registerGameCommand(PaperGameRuntime games) {
    PluginCommand command = plugin.getCommand("zgame");
    if (command == null) {
      throw new IllegalStateException("Command zgame is missing from plugin.yml");
    }
    ZGameCommand executor = new ZGameCommand(games, coordinator);
    command.setExecutor(executor);
    command.setTabCompleter(executor);
  }

  private void registerZombieEngineCommand(
      PaperZombieEngine engine, ZombieDefinitionLoader definitions) {
    PluginCommand command = plugin.getCommand("zzombie");
    if (command == null) {
      throw new IllegalStateException("Command zzombie is missing from plugin.yml");
    }
    ZZombieCommand executor =
        new ZZombieCommand(engine, definitions, coordinator, gameRuntime, plugin);
    command.setExecutor(executor);
    command.setTabCompleter(executor);
  }
}
