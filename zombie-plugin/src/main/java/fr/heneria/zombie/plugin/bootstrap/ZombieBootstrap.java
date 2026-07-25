package fr.heneria.zombie.plugin.bootstrap;

import fr.heneria.zombie.api.PluginState;
import fr.heneria.zombie.api.ZombieApi;
import fr.heneria.zombie.core.bootstrap.ServiceRegistry;
import fr.heneria.zombie.core.config.ConfigurationIssue;
import fr.heneria.zombie.core.config.ZombieSettingsValidator;
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
import fr.heneria.zombie.plugin.instance.InstanceCoordinator;
import fr.heneria.zombie.plugin.isolation.PaperAudienceService;
import fr.heneria.zombie.plugin.isolation.VisibilityService;
import fr.heneria.zombie.plugin.listener.ContextDeathListener;
import fr.heneria.zombie.plugin.listener.InstanceWorldProtectionListener;
import fr.heneria.zombie.plugin.listener.IsolatedChatListener;
import fr.heneria.zombie.plugin.listener.PlayerContextListener;
import fr.heneria.zombie.plugin.lobby.LobbyService;
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
  private BukkitTask maintenanceTask;

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
    lobby.initialize();
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

    api = new PaperZombieApi(state, templates::count, registry::size);
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
        audiences);
    registerListeners(
        new PlayerContextListener(coordinator, sessions, configurations, audiences, messages),
        new IsolatedChatListener(sessions, visibilityPolicy, configurations, messages),
        new ContextDeathListener(sessions, audiences),
        new InstanceWorldProtectionListener(worlds, sessions, configurations));
    registerCommand(configurations, messages, mainThread);

    maintenanceTask =
        plugin
            .getServer()
            .getScheduler()
            .runTaskTimer(plugin, coordinator::expireReconnectReservations, 20L, 20L);
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
    if (maintenanceTask != null) {
      maintenanceTask.cancel();
      maintenanceTask = null;
    }
    if (coordinator != null) {
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
      PaperAudienceService audiences) {
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
            mainThread);
    command.setExecutor(executor);
    command.setTabCompleter(executor);
  }
}
