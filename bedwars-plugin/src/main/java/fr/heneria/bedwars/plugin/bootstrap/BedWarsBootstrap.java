package fr.heneria.bedwars.plugin.bootstrap;

import fr.heneria.bedwars.api.HeneriaBedWarsApi;
import fr.heneria.bedwars.api.PluginStatus;
import fr.heneria.bedwars.api.game.ArenaGameApi;
import fr.heneria.bedwars.api.game.GameApi;
import fr.heneria.bedwars.api.game.PlayerGameApi;
import fr.heneria.bedwars.core.arena.ArenaService;
import fr.heneria.bedwars.core.game.GameInstanceManager;
import fr.heneria.bedwars.core.game.countdown.GameCountdownService;
import fr.heneria.bedwars.core.game.lobby.GameLobbyService;
import fr.heneria.bedwars.core.gui.TextInputService;
import fr.heneria.bedwars.core.lifecycle.LifecycleComponent;
import fr.heneria.bedwars.core.lifecycle.LifecycleManager;
import fr.heneria.bedwars.core.logging.ProjectLogger;
import fr.heneria.bedwars.core.map.MapTemplateService;
import fr.heneria.bedwars.core.service.ServiceRegistry;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import fr.heneria.bedwars.plugin.gui.BukkitGuiService;
import fr.heneria.bedwars.plugin.gui.GuiService;
import fr.heneria.bedwars.plugin.item.ItemService;
import java.util.List;
import java.util.Objects;

/** Default composition root for the Ticket 001 foundation. */
public final class BedWarsBootstrap implements PluginBootstrap, HeneriaBedWarsApi {
  private final String version;
  private final ProjectLogger logger;
  private final ServiceRegistry services = new ServiceRegistry();
  private final LifecycleManager lifecycle;
  private final GameInstanceManager games;
  private final GameApi gameApi;
  private final PlayerGameApi playerApi;
  private final ArenaGameApi arenaApi;
  private PluginStatus status = PluginStatus.STOPPED;

  public BedWarsBootstrap(
      String version,
      ConfigurationService configuration,
      ArenaService arenaService,
      MapTemplateService mapService,
      GameInstanceManager games,
      GameCountdownService countdowns,
      GameLobbyService lobby,
      LifecycleComponent mapLifecycle,
      LifecycleComponent arenaLifecycle,
      LifecycleComponent gameLifecycle,
      TextInputService textInputService,
      LifecycleComponent textInputLifecycle,
      ItemService itemService,
      BukkitGuiService guiService,
      ProjectLogger logger) {
    this.version = Objects.requireNonNull(version, "version");
    this.logger = Objects.requireNonNull(logger, "logger");
    this.games = Objects.requireNonNull(games, "games");
    this.gameApi = new PublicGameApi();
    this.playerApi = new PublicPlayerApi();
    this.arenaApi = new PublicArenaApi();
    services.register(ConfigurationService.class, configuration);
    services.register(ArenaService.class, arenaService);
    services.register(MapTemplateService.class, mapService);
    services.register(GameInstanceManager.class, games);
    services.register(GameCountdownService.class, countdowns);
    services.register(GameLobbyService.class, lobby);
    services.register(TextInputService.class, textInputService);
    services.register(ItemService.class, itemService);
    services.register(GuiService.class, guiService);
    services.register(HeneriaBedWarsApi.class, this);
    lifecycle =
        new LifecycleManager(
            List.of(
                new FoundationComponent(),
                mapLifecycle,
                arenaLifecycle,
                gameLifecycle,
                textInputLifecycle,
                guiService),
            logger);
  }

  @Override
  public void start() throws Exception {
    status = PluginStatus.STARTING;
    try {
      lifecycle.startAll();
      status = PluginStatus.RUNNING;
    } catch (Exception exception) {
      status = PluginStatus.FAILED;
      throw exception;
    }
  }

  @Override
  public void stop() throws Exception {
    status = PluginStatus.STOPPING;
    try {
      lifecycle.stopAll();
      status = PluginStatus.STOPPED;
    } catch (Exception exception) {
      status = PluginStatus.FAILED;
      throw exception;
    }
  }

  @Override
  public String version() {
    return version;
  }

  @Override
  public PluginStatus status() {
    return status;
  }

  @Override
  public GameApi games() {
    return gameApi;
  }

  @Override
  public PlayerGameApi players() {
    return playerApi;
  }

  @Override
  public ArenaGameApi arenas() {
    return arenaApi;
  }

  @Override
  public int serviceCount() {
    return services.size();
  }

  private final class FoundationComponent implements LifecycleComponent {
    @Override
    public String name() {
      return "foundation";
    }

    @Override
    public void start() {
      logger.info("Core foundation initialized with " + services.size() + " services.");
    }

    @Override
    public void stop() {
      logger.info("Core foundation stopped.");
    }
  }

  private final class PublicGameApi implements GameApi {
    @Override
    public java.util.List<fr.heneria.bedwars.api.game.GameSnapshot> all() {
      return games.snapshots();
    }

    @Override
    public java.util.Optional<fr.heneria.bedwars.api.game.GameSnapshot> find(
        java.util.UUID gameId) {
      return games
          .find(new fr.heneria.bedwars.core.game.GameId(gameId))
          .map(game -> game.snapshot(java.time.Instant.now()));
    }

    @Override
    public java.util.Optional<fr.heneria.bedwars.api.game.GameSnapshot> byPlayer(
        java.util.UUID playerId) {
      return games.byPlayer(playerId).map(game -> game.snapshot(java.time.Instant.now()));
    }

    @Override
    public java.util.Optional<fr.heneria.bedwars.api.game.GameSnapshot> byArena(String arenaId) {
      return games.byArena(arenaId).map(game -> game.snapshot(java.time.Instant.now()));
    }
  }

  private final class PublicPlayerApi implements PlayerGameApi {
    @Override
    public java.util.Optional<fr.heneria.bedwars.api.game.GameSnapshot> game(
        java.util.UUID playerId) {
      return gameApi.byPlayer(playerId);
    }

    @Override
    public java.util.Optional<fr.heneria.bedwars.api.game.RuntimePlayerSnapshot> runtime(
        java.util.UUID playerId) {
      return game(playerId)
          .flatMap(
              snapshot ->
                  snapshot.players().stream()
                      .filter(player -> player.playerId().equals(playerId))
                      .findFirst());
    }
  }

  private final class PublicArenaApi implements ArenaGameApi {
    @Override
    public java.util.Optional<fr.heneria.bedwars.api.game.GameSnapshot> game(String arenaId) {
      return gameApi.byArena(arenaId);
    }

    @Override
    public boolean occupied(String arenaId) {
      return games.byArena(arenaId).isPresent();
    }
  }
}
