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
import fr.heneria.zombie.plugin.game.DownedPlayerListener;
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
    AtomicReference<fr.heneria.zombie.core.editor.MapPublicationService> publicationReference =
        new AtomicReference<>();
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
            mainThread,
            mapId -> {
              fr.heneria.zombie.core.editor.MapPublicationService service =
                  publicationReference.get();
              if (service == null) {
                return java.util.Optional.empty();
              }
              return service
                  .publication(mapId)
                  .activeVersion()
                  .map(
                      version ->
                          plugin
                              .getDataFolder()
                              .toPath()
                              .resolve("maps")
                              .resolve(mapId)
                              .resolve("world-versions")
                              .resolve("v" + version)
                              .toAbsolutePath()
                              .normalize())
                  .filter(java.nio.file.Files::isDirectory);
            });
    previewService = new MapPreviewService(templates, worlds, lobby, mainThread);
    api = new PaperZombieApi(state, templates::count, registry::size);
    GuiRegistry guiRegistry = new GuiRegistry();
    GuiActionRegistry guiActions = new GuiActionRegistry();
    YamlMapPersistence mapPersistence =
        new YamlMapPersistence(
            plugin.getDataFolder().toPath(),
            templates.rootDirectory(),
            plugin.getServer().getWorldContainer().toPath(),
            ioExecutor);
    MapRegistry mapRegistry = new MapRegistry();
    editorService = new MapEditorService(mapRegistry, mapPersistence, Clock.systemUTC());
    MapValidator mapValidator = new MapValidator();
    fr.heneria.zombie.core.editor.MapPublicationService mapPublications =
        new fr.heneria.zombie.core.editor.MapPublicationService(
            mapRegistry,
            mapValidator,
            new fr.heneria.zombie.plugin.editor.YamlMapPublicationPersistence(
                plugin.getDataFolder().toPath(), templates.rootDirectory(), ioExecutor),
            Clock.systemUTC());
    publicationReference.set(mapPublications);
    fr.heneria.zombie.plugin.editor.MapWorldPublicationService mapWorldPublications =
        new fr.heneria.zombie.plugin.editor.MapWorldPublicationService(
            plugin.getServer().getWorldContainer().toPath(), templates, ioExecutor);
    editorItems = new EditorItemService(plugin);
    mapVisualizations = new MapVisualizationService(plugin);
    editorService
        .initialize()
        .thenCompose(ignored -> mapPublications.initialize())
        .whenComplete(
            (count, failure) -> {
              if (failure == null) {
                plugin
                    .getLogger()
                    .info(
                        "Loaded "
                            + mapRegistry.all().size()
                            + " editable map definition(s) and "
                            + count
                            + " publication history file(s).");
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
              known.addAll(fr.heneria.zombie.plugin.gui.MapMenuModule.ACTION_IDS);
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
            prevÛ®5¶‰ËkºwµçHYˆ
[[YHOH[	‰ˆ™X\ÛÛ‹˜ÛÛ\]\Ô›İ[™Ûİ

JHÃBˆ[[YK›ÛXšYT™[[İ™Y
Bˆ›ÛXšYK™[]RY

KÚ[\’Y™]Ø\™™]Ø\™™X\ÛÛ‹X]ØØ][ÛŠNÃBˆCBˆJNÃBˆ\\”İÙ\•\Ù\šXÙH\\”İÙ\•\ÈCBˆ™]È\\”İÙ\•\Ù\šXÙJBˆİÙ\•\ËBˆİÙ\•\›ÜËBˆ›ÛXšYQ[™Ú[™KBˆØ[YRYOˆÃBˆ\\‘Ø[YT[[YH[[YHHØ[YT™Y™\™[˜ÙK™Ù]

NÃBˆ™]\›ˆ[[YHOH[È\İ›ÙŠ
Hˆ[[YKœ^Y\’YÊØ[YRY
NÃBˆJNÃBˆÙX\Û‘Yš[š][Û“ØY\ˆÙX\Û‘Yš[š][ÛœÈH™]ÈÙX\Û‘Yš[š][Û“ØY\ŠYÚ[‹[Ñ^Xİ]ÜŠNÃBˆÙX\Û‘Yš[š][ÛœÃBˆš[š]X[^™P\Ş[˜Ê
CBˆÚ[ÛÛ\]JBˆ
ØYY˜Z[\™JHOˆÃBˆYˆ
˜Z[\™HOH[
HÃBˆYÚ[ƒBˆ™Ù]ÙÙÙ\Š
CBˆš[™›ÊBˆ“ØYYƒBˆ
ÈØYYœÚ^™J
CBˆ
ÈˆÙX\ÛˆYš[š][ÛŠÊHœ›ÛHƒBˆ
ÈÙX\Û‘Yš[š][ÛœË™\™XİÜJ
JNÃBˆH[ÙHÃBˆYÚ[ƒBˆ™Ù]ÙÙÙ\Š
CBˆœÙ]™\™JBˆ‘^\›˜[ÙX\ÛˆYš[š][ÛœÈ™Z™XİYÈ\İ˜[YÛ˜\Úİ™[XZ[œÈXİ]™NˆƒBˆ
È˜Z[\™K™Ù]Y\ÜØYÙJ
JNÃBˆCBˆJNÃBˆ\\•ÙX\Û”Ù\šXÙHÙX\Û”Ù\šXÙHCBˆ™]È\\•ÙX\Û”Ù\šXÙJBˆYÚ[‹BˆÙX\Û‘Yš[š][ÛœËBˆ›ÛXšYQ[™Ú[™KBˆ^Y\’YOˆÃBˆ\\‘Ø[YT[[YH[[YHHØ[YT™Y™\™[˜ÙK™Ù]

NÃBˆ™]\›ˆ[[YHOH[È˜]˜K][“Ü[Û˜[™[\J
Hˆ[[YK™Ø[YQ›ÜŠ^Y\’Y
NÃBˆKBˆØ[YRYOˆÃBˆ\\‘Ø[YT[[YH[[YHHØ[YT™Y™\™[˜ÙK™Ù]

NÃBˆ™]\›ˆ[[YHOH[È˜]˜K][“Ü[Û˜[™[\J
Hˆ[[YK›X\›ÜŠØ[YRY
NÃBˆKBˆ\˜Ú\Ù\ËBˆ™]Ø\™ËBˆØ[YRYOƒBˆİÙ\•\Ë˜Xİ]™JØ[YRYœ‹š[™\šXK›ÛXšYK˜ÛÜ™KœİÙ\\”İÙ\•\\K’S”ÕWÒÒS
KBˆ^Y\’YOˆÃBˆ\\‘Ø[YT[[YH[[YHHØ[YT™Y™\™[˜ÙK™Ù]

NÃBˆ™]\›ˆ[[YHOH[	‰ˆ[[YK˜Ø[Xİ
^Y\’Y
NÃBˆKBˆ
Ø[YRY^Y\’Y\YY[XYÙKXYÚİ
HOˆÃBˆ\\‘Ø[YT[[YH[[YHHØ[YT™Y™\™[˜ÙK™Ù]

NÃBˆYˆ
[[YHOH[
HÃBˆ[[YKÙX\Û’]
Ø[YRY^Y\’Y\YY[XYÙKXYÚİ
NÃBˆCBˆJNÃBˆ\\”İÙ\•\ËÙX\ÛœÊÙX\Û”Ù\šXÙJNÃBˆØ[YT[[YHCBˆ™]È\\‘Ø[YT[[YJBˆØ[YTÙ\šXÙKBˆX\™YÚ\İKBˆX\X›XØ][ÛœËBˆÛÛ™šYİ\˜][ÛœËBˆÛÛÜ™[˜]Ü‹BˆØÛÜ™X›Ø\™ËBˆ›ÛXšYQ[™Ú[™KBˆÙX\Û”Ù\šXÙKBˆXÛÛ›ÛZY\ËBˆ˜[œØXİ[ÛœËBˆ\˜Ú\Ù\ËBˆ™]Ø\™ËBˆİÙ\•\ËBˆ\\”İÙ\•\ËBˆÚ[\Ü^KBˆX\š\İX[^˜][ÛœËBˆ™\İ[Oˆ˜]˜K][˜ÛÛ˜İ\œ™[ÛÛ\]X›Q]\™K˜ÛÛ\]Y]\™J[
KBˆY\ÜØYÙ\ËBˆYÚ[‹™Ù]ÙÙÙ\Š
JNÃBˆØ[YT™Y™\™[˜ÙKœÙ]
Ø[YT[[YJNÃBˆ™]Èœ‹š[™\šXK›ÛXšYKœYÚ[‹™İZK“X\Y[S[Ù[JBˆİZT™YÚ\İKBˆİZPXİ[ÛœËBˆİZPÛÛ™šYİ\˜][ÛœËBˆİZTÙ\šXÙKBˆY]Ü”Ù\šXÙKBˆX\˜[Y]Ü‹BˆX\X›XØ][ÛœËBˆ[\]\ËBˆX\ÛÜ›X›XØ][ÛœËBˆY]Ü’][\ËBˆX\š\İX[^˜][ÛœËBˆÛÛÜ™[˜]Ü‹BˆØ[YT[[YKBˆXZ[•™XYBˆÛØÚËœŞ\İ[UUÊ
JCBˆœ™YÚ\İ\Š
NÃBˆ™]ÈÙX\Û‘İZS[Ù[JBˆİZT™YÚ\İKİZPXİ[ÛœËİZPÛÛ™šYİ\˜][ÛœËİZTÙ\šXÙKÙX\Û”Ù\šXÙKØ[YT[[YJCBˆœ™YÚ\İ\Š
NÃBˆİZPÛÛ™šYİ\˜][ÛœÃBˆš[š]X[^™P\Ş[˜Ê
CBˆÚ[ÛÛ\]JBˆ
YÛ›Ü™Y˜Z[\™JHOˆÃBˆYˆ
˜Z[\™HOH[
HÃBˆYÚ[ƒBˆ™Ù]ÙÙÙ\Š
CBˆœÙ]™\™JBˆ™İZ\Ë[[™Z™XİYÈ[™YY˜][È™[XZ[ˆXİ]™NˆƒBˆ
È˜Z[\™K™Ù]Y\ÜØYÙJ
JNÃBˆCBˆJNÃBƒBˆ™YÚ\İ\”Ù\šXÙ\ÊBˆÛÛ™šYİ\˜][ÛœËBˆY\ÜØYÙ\ËBˆ[\]\ËBˆÛÜ›ËBˆ™YÚ\İKBˆ[œİ[˜Ù\ËBˆÙ\ÜÚ[ÛœËBˆ^Y\”İ]\ËBˆØÛÜ™X›Ø\™ËBˆØ˜KBˆš\ÚXš[]KBˆ]YY[˜Ù\ËBˆ™]šY]ÔÙ\šXÙJNÃBˆ™YÚ\İ\“\İ[™\œÊBˆ™]È^Y\ÛÛ^\İ[™\ŠBˆÛÛÜ™[˜]Ü‹Ù\ÜÚ[ÛœËÛÛ™šYİ\˜][ÛœË]YY[˜Ù\ËY\ÜØYÙ\ËØ[YT[[YJKBˆ™]È\ÛÛ]YÚ]\İ[™\ŠÙ\ÜÚ[ÛœËš\ÚXš[]TÛXŞKÛÛ™šYİ\˜][ÛœËY\ÜØYÙ\ÊKBˆ™]ÈÛÛ^X]\İ[™\ŠÙ\ÜÚ[ÛœË]YY[˜Ù\ÊKBˆ™]È[œİ[˜ÙUÛÜ››İXİ[Û“\İ[™\ŠÛÜ›ËÙ\ÜÚ[ÛœËÛÛ™šYİ\˜][ÛœË™]šY]ÔÙ\šXÙJKBˆ™]ÈX\™]šY]Ó\İ[™\Š™]šY]ÔÙ\šXÙJKBˆ™]ÈİZS\İ[™\ŠİZTÙ\šXÙJKBˆ™]ÈİZPÚ][œ]\İ[™\ŠYÚ[‹İZTÙ\šXÙJKBˆ™]ÈİÛ™Y^Y\“\İ[™\ŠØ[YT[[YJKBˆ™]ÈØ[YPÛÛX˜]\İ[™\ŠØ[YT[[YK›ÛXšYQ[™Ú[™KY\ÜØYÙ\ÊKBˆ™]È›ÛXšYT›İXİ[Û“\İ[™\Š›ÛXšYQ[™Ú[™JKBˆ™]ÈÙX\Û“\İ[™\ŠÙX\Û”Ù\šXÙJKBˆ™]ÈY]Ü”XÙ[Y[\İ[™\ŠBˆY]Ü”Ù\šXÙKBˆY]Ü’][\ËBˆİZTÙ\šXÙKBˆX\š\İX[^˜][ÛœËBˆÛØÚËœŞ\İ[UUÊ
KBˆXZ[•™XY
KBˆ™]ÈY]Ü”Ù\ÜÚ[Û“\İ[™\ŠY]Ü”Ù\šXÙKY]Ü’][\ËX\š\İX[^˜][ÛœÊJNÃBˆ™YÚ\İ\ÛÛ[X[™
BˆÛÛ™šYİ\˜][ÛœËBˆY\ÜØYÙ\ËBˆ[\]\ËBˆ™]šY]ÔÙ\šXÙKBˆİZPÛÛ™šYİ\˜][ÛœËBˆİZTÙ\šXÙKBˆXZ[•™XY
NÃBˆ™YÚ\İ\”^Y\‘İZPÛÛ[X[™
İZTÙ\šXÙJNÃBˆ™YÚ\İ\“X\Y]ÜÛÛ[X[™
X\˜[Y]Ü‹XZ[•™XY
NÃBˆ™YÚ\İ\‘Ø[YPÛÛ[X[™
Ø[YT[[YJNÃBˆ™YÚ\İ\–›ÛXšYQ[™Ú[™PÛÛ[X[™
›ÛXšYQ[™Ú[™K›ÛXšYQYš[š][ÛœÊNÃBˆ™YÚ\İ\•ÙX\ÛÛÛ[X[™
ÙX\Û”Ù\šXÙKÙX\Û‘Yš[š][ÛœÊNÃBˆ™YÚ\İ\‘XÛÛ›Û^PÛÛ[X[™
XÛÛ›ÛZY\Ë˜[œØXİ[ÛœËİÙ\•\Ë\\”İÙ\•\ÊNÃBƒBˆXZ[[˜[˜ÙU\ÚÈCBˆYÚ[ƒBˆ™Ù]Ù\™\Š
CBˆ™Ù]ØÚY[\Š
CBˆœ[•\ÚÕ[Y\ŠYÚ[‹ÛÛÜ™[˜]Ü™^\™T™XÛÛ›™Xİ™\Ù\˜][ÛœËŒŒ
NÃBˆØ[YU\ÚÈHYÚ[‹™Ù]Ù\™\Š
K™Ù]ØÚY[\Š
Kœ[•\ÚÕ[Y\ŠYÚ[‹Ø[YT[[YNXÚËSS
NÃBˆ›Üˆ
Ü™Ë˜ZÚÚ]™[]K”^Y\ˆ^Y\ˆˆYÚ[‹™Ù]Ù\™\Š
K™Ù]Û›[™T^Y\œÊ
JHÃBˆÛÛÜ™[˜]Ü‹˜ÛÛ›™Xİ
^Y\ŠNÃBˆCBˆYÚ[ƒBˆ™Ù]Ù\™\Š
CBˆ™Ù]Ù\šXÙ\ÓX[˜YÙ\Š
CBˆœ™YÚ\İ\Š›ÛXšYP\K˜Û\ÜË\KYÚ[‹Ù\šXÙTš[Üš]K“›Ü›X[
NÃBˆYÚ[‹™Ù]ÙÙÙ\Š
Kš[™›Ê“Ø˜H[™\ÛÛ]YZ[œİ[˜ÙH[[YH[š]X[^™YˆŠNÃBˆCBƒBˆÊŠˆ™[X\Ù\È\ÚÜË^Y\œËÛÜ›ËTH™YÚ\İ˜][Ûˆ[™^Xİ]Üˆ™XYËˆ
‹ÃBˆX›XÈ›ÚYİÜ

HÃBˆYˆ
Y]Ü”Ù\šXÙHOH[
HÃBˆ›Üˆ
Ü™Ë˜ZÚÚ]™[]K”^Y\ˆ^Y\ˆˆYÚ[‹™Ù]Ù\™\Š
K™Ù]Û›[™T^Y\œÊ
JHÃBˆY]Ü”Ù\šXÙK›X]™J^Y\‹™Ù][š\]YRY

JNÃBˆYˆ
Y]Ü’][\ÈOH[
HÃBˆY]Ü’][\Ëœ™[[İ™J^Y\ŠNÃBˆCBˆCBˆY]Ü”Ù\šXÙHH[ÃBˆY]Ü’][\ÈH[ÃBˆYˆ
X\š\İX[^˜][ÛœÈOH[
HÃBˆX\š\İX[^˜][ÛœË˜ÛX\[

NÃBˆX\š\İX[^˜][ÛœÈH[ÃBˆCBˆCBˆYˆ
İZTÙ\šXÙHOH[
HÃBˆİZTÙ\šXÙKœÚ]İÛŠ
NÃBˆİZTÙ\šXÙHH[ÃBˆCBˆYˆ
XZ[[˜[˜ÙU\ÚÈOH[
HÃBˆXZ[[˜[˜ÙU\ÚË˜Ø[˜Ù[

NÃBˆXZ[[˜[˜ÙU\ÚÈH[ÃBˆCBˆYˆ
Ø[YU\ÚÈOH[
HÃBˆØ[YU\ÚË˜Ø[˜Ù[

NÃBˆØ[YU\ÚÈH[ÃBˆCBˆYˆ
Ø[YT[[YHOH[
HÃBˆØ[YT[[YKœÚ]İÛŠ
NÃBˆØ[YT[[YHH[ÃBˆCBˆYˆ
ÛÛÜ™[˜]ÜˆOH[
HÃBˆYˆ
™]šY]ÔÙ\šXÙHOH[
HÃBˆ™]šY]ÔÙ\šXÙK˜ÛX\Š
NÃBˆ™]šY]ÔÙ\šXÙHH[ÃBˆCBˆ\İİš[™Ïˆ˜Z[\™\ÈHÛÛÜ™[˜]Ü‹œÚ]İÛŠ
NÃBˆ˜Z[\™\Ë™›Ü‘XXÚ
BˆÛÜ›OˆYÚ[‹™Ù]ÙÙÙ\Š
KØ\›š[™Ê•ÛÜ›Ûİ[›İ™H[›ØYYˆˆ
ÈÛÜ›
JNÃBˆÛÛÜ™[˜]ÜˆH[ÃBˆCBˆYˆ
\HOH[
HÃBˆYÚ[‹™Ù]Ù\™\Š
K™Ù]Ù\šXÙ\ÓX[˜YÙ\Š
K[œ™YÚ\İ\Š›ÛXšYP\K˜Û\ÜË\JNÃBˆ\HH[ÃBˆCBˆÙ\šXÙ\Ë˜ÛX\Š
NÃBˆYˆ
[Ñ^Xİ]ÜˆOH[
HÃBˆ[Ñ^Xİ]Ü‹œÚ]İÛŠ
NÃBˆHÃBˆYˆ
Z[Ñ^Xİ]Ü‹˜]ØZ]\›Z[˜][ÛŠ‹[YU[š]”ÑPÓÓ‘ÊJHÃBˆ[Ñ^Xİ]Ü‹œÚ]İÛ“›İÊ
NÃBˆCBˆHØ]Ú
[\œ\Y^Ù\[Ûˆ[\œ\Y
HÃBˆ[Ñ^Xİ]Ü‹œÚ]İÛ“›İÊ
NÃBˆ™XY˜İ\œ™[™XY

Kš[\œ\

NÃBˆCBˆ[Ñ^Xİ]ÜˆH[ÃBˆCBˆCBƒBˆš]˜]H›ÚY™YÚ\İ\”Ù\šXÙ\ÊBˆÛÛ™šYİ\˜][Û“X[˜YÙ\ˆÛÛ™šYİ\˜][ÛœËBˆY\ÜØYÙTÙ\šXÙHY\ÜØYÙ\ËBˆX\[\]PØ][ÙÈ[\]\ËBˆ\\•ÛÜ›[œİ[˜ÙTÙ\šXÙHÛÜ›ËBˆØ[YR[œİ[˜ÙT™YÚ\İH™YÚ\İKBˆØ[YR[œİ[˜ÙTÙ\šXÙH[œİ[˜Ù\ËBˆ^Y\”Ù\ÜÚ[Û”Ù\šXÙHÙ\ÜÚ[ÛœËBˆ\\”^Y\”İ]TÙ\šXÙH^Y\”İ]\ËBˆÛÛ^ØÛÜ™X›Ø\™Ù\šXÙHØÛÜ™X›Ø\™ËBˆØ˜TÙ\šXÙHØ˜KBˆš\ÚXš[]TÙ\šXÙHš\ÚXš[]KBˆ\\]YY[˜ÙTÙ\šXÙH]YY[˜Ù\ËBˆX\™]šY]ÔÙ\šXÙH™]šY]ÜÊHÃBˆÙ\šXÙ\Ëœ™YÚ\İ\ŠÛÛ™šYİ\˜][Û“X[˜YÙ\‹˜Û\ÜËÛÛ™šYİ\˜][ÛœÊNÃBˆÙ\šXÙ\Ëœ™YÚ\İ\ŠY\ÜØYÙTÙ\šXÙK˜Û\ÜËY\ÜØYÙ\ÊNÃBˆÙ\šXÙ\Ëœ™YÚ\İ\ŠX\[\]PØ][ÙË˜Û\ÜË[\]\ÊNÃBˆÙ\šXÙ\Ëœ™YÚ\İ\Š\\•ÛÜ›[œİ[˜ÙTÙ\šXÙK˜Û\ÜËÛÜ›ÊNÃBˆÙ\šXÙ\Ëœ™YÚ\İ\ŠØ[YR[œİ[˜ÙT™YÚ\İK˜Û\ÜË™YÚ\İJNÃBˆÙ\šXÙ\Ëœ™YÚ\İ\ŠØ[YR[œİ[˜ÙTÙ\šXÙK˜Û\ÜË[œİ[˜Ù\ÊNÃBˆÙ\šXÙ\Ëœ™YÚ\İ\Š^Y\”Ù\ÜÚ[Û”Ù\šXÙK˜Û\ÜËÙ\ÜÚ[ÛœÊNÃBˆÙ\šXÙ\Ëœ™YÚ\İ\Š\\”^Y\”İ]TÙ\šXÙK˜Û\ÜË^Y\”İ]\ÊNÃBˆÙ\šXÙ\Ëœ™YÚ\İ\ŠÛÛ^ØÛÜ™X›Ø\™Ù\šXÙK˜Û\ÜËØÛÜ™X›Ø\™ÊNÃBˆÙ\šXÙ\Ëœ™YÚ\İ\ŠØ˜TÙ\šXÙK˜Û\ÜËØ˜JNÃBˆÙ\šXÙ\Ëœ™YÚ\İ\Šš\ÚXš[]TÙ\šXÙK˜Û\ÜËš\ÚXš[]JNÃBˆÙ\šXÙ\Ëœ™YÚ\İ\Š\\]YY[˜ÙTÙ\šXÙK˜Û\ÜË]YY[˜Ù\ÊNÃBˆÙ\šXÙ\Ëœ™YÚ\İ\ŠX\™]šY]ÔÙ\šXÙK˜Û\ÜË™]šY]ÜÊNÃBˆÙ\šXÙ\Ëœ™YÚ\İ\Š[œİ[˜ÙPÛÛÜ™[˜]Ü‹˜Û\ÜËÛÛÜ™[˜]ÜŠNÃBˆÙ\šXÙ\Ëœ™YÚ\İ\Š›ÛXšYP\K˜Û\ÜË\JNÃBˆCBƒBˆš]˜]H›ÚY™YÚ\İ\“\İ[™\œÊ\İ[™\‹‹‹ˆ\İ[™\œÊHÃBˆ›Üˆ
\İ[™\ˆ\İ[™\ˆˆ\İ[™\œÊHÃBˆYÚ[‹™Ù]Ù\™\Š
K™Ù]YÚ[“X[˜YÙ\Š
Kœ™YÚ\İ\‘]™[Ê\İ[™\‹YÚ[ŠNÃBˆCBˆCBƒBˆš]˜]H›ÚY™YÚ\İ\ÛÛ[X[™
BˆÛÛ™šYİ\˜][Û“X[˜YÙ\ˆÛÛ™šYİ\˜][ÛœËBˆY\ÜØYÙTÙ\šXÙHY\ÜØYÙ\ËBˆX\[\]PØ][ÙÈ[\]\ËBˆX\™]šY]ÔÙ\šXÙH™]šY]ÜËBˆİZPÛÛ™šYİ\˜][Û”Ù\šXÙHİZPÛÛ™šYİ\˜][ÛœËBˆİZTÙ\šXÙHİZTÙ\šXÙKBˆ\\“XZ[•™XY^Xİ]ÜˆXZ[•™XY
HÃBˆYÚ[ÛÛ[X[™ÛÛ[X[™HYÚ[‹™Ù]ÛÛ[X[™
›ÛXšYHŠNÃBˆYˆ
ÛÛ[X[™OH[
HÃBˆ›İÈ™]È[YØ[İ]Q^Ù\[ÛŠÛÛ[X[™›ÛXšYH\ÈZ\ÜÚ[™Èœ›ÛHYÚ[‹[[ŠNÃBˆCBˆ›ÛXšYPÛÛ[X[™^Xİ]ÜˆCBˆ™]È›ÛXšYPÛÛ[X[™
BˆYÚ[‹™Ù]YÚ[“Y]J
K™Ù]™\œÚ[ÛŠ
KBˆ\KBˆİ]KBˆÛÛ™šYİ\˜][ÛœËBˆY\ÜØYÙ\ËBˆÛÛÜ™[˜]Ü‹Bˆ[\]\ËBˆ™]šY]ÜËBˆİZPÛÛ™šYİ\˜][ÛœËBˆİZTÙ\šXÙKBˆØ[YT[[YKBˆXZ[•™XY
NÃBˆÛÛ[X[™œÙ]^Xİ]ÜŠ^Xİ]ÜŠNÃBˆÛÛ[X[™œÙ]XÛÛ\]\Š^Xİ]ÜŠNÃBˆCBƒBˆš]˜]H›ÚY™YÚ\İ\”^Y\‘İZPÛÛ[X[™
İZTÙ\šXÙHİZTÙ\šXÙJHÃBˆYÚ[ÛÛ[X[™ÛÛ[X[™HYÚ[‹™Ù]ÛÛ[X[™
›ÛXšY\ÈŠNÃBˆYˆ
ÛÛ[X[™OH[
HÃBˆ›İÈ™]È[YØ[İ]Q^Ù\[ÛŠÛÛ[X[™›ÛXšY\È\ÈZ\ÜÚ[™Èœ›ÛHYÚ[‹[[ŠNÃBˆCBˆÛÛ[X[™œÙ]^Xİ]ÜŠBˆ
Ù[™\‹YÛ›Ü™YX™[\™İ[Y[ÊHOˆÃBˆYˆ
JÙ[™\ˆ[œİ[˜Ù[ÙˆÜ™Ë˜ZÚÚ]™[]K”^Y\ˆ^Y\ŠJHÃBˆÙ[™\‹œÙ[™Y\ÜØYÙJÙ]HÛÛ[X[™HÚ]0ê™H][\ğêYH[ˆ™]KˆŠNÃBˆH[ÙHYˆ
\^Y\‹š\Ô\›Z\ÜÚ[ÛŠ›ÛXšY\Ë›Y[Kœ^Y\ˆŠJHÃBˆ^Y\‹œÙ[™Y\ÜØYÙJBˆ™]šŞ[ÜšK˜Y™[\™K^›Z[š[Y\ÜØYÙK“Z[šSY\ÜØYÙK›Z[šSY\ÜØYÙJ
CBˆ™\Ù\šX[^™J™Y•›İ\È‰Ø]™^ˆ\ÈH\›Z\ÜÚ[Û‹ˆŠJNÃBˆH[ÙHÃBˆ›ÛÛX[ˆYZ[ˆCBˆ\™İ[Y[Ë›[™İˆBˆ	‰ˆ\™İ[Y[ÖÌK™\]X[ÒYÛ›Ü™PØ\ÙJ˜YZ[ˆŠCBˆ	‰ˆ^Y\‹š\Ô\›Z\ÜÚ[ÛŠ›ÛXšY\Ë›Y[K˜YZ[ˆŠNÃBˆİZTÙ\šXÙK›Ü[’ÛYJBˆ^Y\‹Bˆ™]Èœ‹š[™\šXK›ÛXšYKœYÚ[‹™İZK‘İZRY
YZ[ˆÈ˜YZ[‹[XZ[ˆˆˆœ^Y\‹[XZ[ˆŠJNÃBˆCBˆ™]\›ˆYNÃBˆJNÃBˆCBƒBˆš]˜]H›ÚY™YÚ\İ\“X\Y]ÜÛÛ[X[™
BˆX\˜[Y]Üˆ˜[Y]Ü‹\\“XZ[•™XY^Xİ]ÜˆXZ[•™XY
HÃBˆYÚ[ÛÛ[X[™ÛÛ[X[™HYÚ[‹™Ù]ÛÛ[X[™
›X\ŠNÃBˆYˆ
ÛÛ[X[™OH[
HÃBˆ›İÈ™]È[YØ[İ]Q^Ù\[ÛŠÛÛ[X[™›X\\ÈZ\ÜÚ[™Èœ›ÛHYÚ[‹[[ŠNÃBˆCBˆ“X\ÛÛ[X[™^Xİ]ÜˆCBˆ™]È“X\ÛÛ[X[™
BˆY]Ü”Ù\šXÙKBˆ˜[Y]Ü‹BˆY]Ü’][\ËBˆİZTÙ\šXÙKBˆÛÛÜ™[˜]Ü‹Bˆ™]šY]ÔÙ\šXÙKBˆØ[YT[[YKBˆX\š\İX[^˜][ÛœËBˆXZ[•™XY
NÃBˆÛÛ[X[™œÙ]^Xİ]ÜŠ^Xİ]ÜŠNÃBˆÛÛ[X[™œÙ]XÛÛ\]\Š^Xİ]ÜŠNÃBˆCBƒBˆš]˜]H›ÚY™YÚ\İ\‘Ø[YPÛÛ[X[™
\\‘Ø[YT[[YHØ[Y\ÊHÃBˆYÚ[ÛÛ[X[™ÛÛ[X[™HYÚ[‹™Ù]ÛÛ[X[™
™Ø[YHŠNÃBˆYˆ
ÛÛ[X[™OH[
HÃBˆ›İÈ™]È[YØ[İ]Q^Ù\[ÛŠÛÛ[X[™™Ø[YH\ÈZ\ÜÚ[™Èœ›ÛHYÚ[‹[[ŠNÃBˆCBˆ‘Ø[YPÛÛ[X[™^Xİ]ÜˆH™]È‘Ø[YPÛÛ[X[™
Ø[Y\ËÛÛÜ™[˜]ÜŠNÃBˆÛÛ[X[™œÙ]^Xİ]ÜŠ^Xİ]ÜŠNÃBˆÛÛ[X[™œÙ]XÛÛ\]\Š^Xİ]ÜŠNÃBˆCBƒBˆš]˜]H›ÚY™YÚ\İ\–›ÛXšYQ[™Ú[™PÛÛ[X[™
Bˆ\\–›ÛXšYQ[™Ú[™H[™Ú[™K›ÛXšYQYš[š][Û“ØY\ˆYš[š][ÛœÊHÃBˆYÚ[ÛÛ[X[™ÛÛ[X[™HYÚ[‹™Ù]ÛÛ[X[™
›ÛXšYHŠNÃBˆYˆ
ÛÛ[X[™OH[
HÃBˆ›İÈ™]È[YØ[İ]Q^Ù\[ÛŠÛÛ[X[™›ÛXšYH\ÈZ\ÜÚ[™Èœ›ÛHYÚ[‹[[ŠNÃBˆCBˆ–›ÛXšYPÛÛ[X[™^Xİ]ÜˆCBˆ™]È–›ÛXšYPÛÛ[X[™
[™Ú[™KYš[š][ÛœËÛÛÜ™[˜]Ü‹Ø[YT[[YKYÚ[ŠNÃBˆÛÛ[X[™œÙ]^Xİ]ÜŠ^Xİ]ÜŠNÃBˆÛÛ[X[™œÙ]XÛÛ\]\Š^Xİ]ÜŠNÃBˆCBƒBˆš]˜]H›ÚY™YÚ\İ\•ÙX\ÛÛÛ[X[™
Bˆ\\•ÙX\Û”Ù\šXÙHÙX\ÛœËÙX\Û‘Yš[š][Û“ØY\ˆYš[š][ÛœÊHÃBˆYÚ[ÛÛ[X[™ÛÛ[X[™HYÚ[‹™Ù]ÛÛ[X[™
ÙX\ÛˆŠNÃBˆYˆ
ÛÛ[X[™OH[
HÃBˆ›İÈ™]È[YØ[İ]Q^Ù\[ÛŠÛÛ[X[™ÙX\Ûˆ\ÈZ\ÜÚ[™Èœ›ÛHYÚ[‹[[ŠNÃBˆCBˆ•ÙX\ÛÛÛ[X[™^Xİ]ÜˆCBˆ™]È•ÙX\ÛÛÛ[X[™
YÚ[‹ÙX\ÛœËYš[š][ÛœËØ[YT[[YKİZTÙ\šXÙJNÃBˆÛÛ[X[™œÙ]^Xİ]ÜŠ^Xİ]ÜŠNÃBˆÛÛ[X[™œÙ]XÛÛ\]\Š^Xİ]ÜŠNÃBˆCBƒBˆš]˜]H›ÚY™YÚ\İ\‘XÛÛ›Û^PÛÛ[X[™
BˆXÛÛ›Û^TÙ\šXÙHXÛÛ›ÛZY\ËBˆ˜[œØXİ[Û”Ù\šXÙH˜[œØXİ[ÛœËBˆİÙ\•\Ù\šXÙHİÙ\•\ËBˆ\\”İÙ\•\Ù\šXÙH\\”İÙ\•\ÊHÃBˆYÚ[ÛÛ[X[™ÛÛ[X[™HYÚ[‹™Ù]ÛÛ[X[™
™XÛÛ›Û^HŠNÃBˆYˆ
ÛÛ[X[™OH[
HÃBˆ›İÈ™]È[YØ[İ]Q^Ù\[ÛŠÛÛ[X[™™XÛÛ›Û^H\ÈZ\ÜÚ[™Èœ›ÛHYÚ[‹[[ŠNÃBˆCBˆ‘XÛÛ›Û^PÛÛ[X[™^Xİ]ÜˆCBˆ™]È‘XÛÛ›Û^PÛÛ[X[™
Ø[YT[[YKXÛÛ›ÛZY\Ë˜[œØXİ[ÛœËİÙ\•\Ë\\”İÙ\•\ÊNÃBˆÛÛ[X[™œÙ]^Xİ]ÜŠ^Xİ]ÜŠNÃBˆÛÛ[X[™œÙ]XÛÛ\]\Š^Xİ]ÜŠNÃBˆCBŸCB