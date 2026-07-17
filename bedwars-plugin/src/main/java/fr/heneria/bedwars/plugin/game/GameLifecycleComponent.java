package fr.heneria.bedwars.plugin.game;

import fr.heneria.bedwars.core.game.GameDeathService;
import fr.heneria.bedwars.core.game.GameId;
import fr.heneria.bedwars.core.game.GameInstanceManager;
import fr.heneria.bedwars.core.game.GameRespawnService;
import fr.heneria.bedwars.core.game.event.GameEventBus;
import fr.heneria.bedwars.core.game.event.GameVictoryEvent;
import fr.heneria.bedwars.core.game.event.GameWaitingEvent;
import fr.heneria.bedwars.core.game.lobby.GameLobbyService;
import fr.heneria.bedwars.core.lifecycle.LifecycleComponent;
import fr.heneria.bedwars.core.logging.ProjectLogger;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Registers runtime cleanup hooks and coordinates orphan recovery. */
public final class GameLifecycleComponent implements LifecycleComponent, Listener {
  private final JavaPlugin plugin;
  private final GameInstanceManager games;
  private final BukkitRuntimeWorldService worlds;
  private final GameEventBus events;
  private final GameLobbyService lobby;
  private final BukkitGameDisplayService displays;
  private final GameWaitingListener waitingListener;
  private final BukkitGamePlayListener playListener;
  private final BukkitGameBedRegistry beds;
  private final GameDeathService deaths;
  private final GameRespawnService respawns;
  private final BukkitRuntimePlayerGateway players;
  private final ConfigurationService configurations;
  private final ProjectLogger logger;
  private AutoCloseable eventSubscription;
  private BukkitTask ticker;
  private long ticks;
  private final java.util.Map<GameId, java.time.Instant> endingDeadlines =
      new java.util.LinkedHashMap<>();
  private final java.util.Set<GameId> endingCleanups = new java.util.HashSet<>();

  public GameLifecycleComponent(
      JavaPlugin plugin,
      GameInstanceManager games,
      BukkitRuntimeWorldService worlds,
      GameEventBus events,
      GameLobbyService lobby,
      BukkitGameDisplayService displays,
      GameWaitingListener waitingListener,
      BukkitGamePlayListener playListener,
      BukkitGameBedRegistry beds,
      GameDeathService deaths,
      GameRespawnService respawns,
      BukkitRuntimePlayerGateway players,
      ConfigurationService configurations,
      ProjectLogger logger) {
    this.plugin = plugin;
    this.games = games;
    this.worlds = worlds;
    this.events = events;
    this.lobby = lobby;
    this.displays = displays;
    this.waitingListener = waitingListener;
    this.playListener = playListener;
    this.beds = beds;
    this.deaths = deaths;
    this.respawns = respawns;
    this.players = players;
    this.configurations = configurations;
    this.logger = logger;
  }

  @Override
  public String name() {
    return "game-instances";
  }

  @Override
  public void start() {
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
    plugin.getServer().getPluginManager().registerEvents(waitingListener, plugin);
    plugin.getServer().getPluginManager().registerEvents(playListener, plugin);
    eventSubscription =
        events.subscribe(
            event -> {
              Runnable action =
                  () -> {
                    if (event instanceof GameWaitingEvent waiting)
                      games.find(waiting.gameId()).ifPresent(beds::initialize);
                    if (event instanceof GameVictoryEvent victory)
                      endingDeadlines.put(
                          victory.gameId(),
                          java.time.Instant.now()
                              .plusSeconds(
                                  configurations.snapshot().game().endingDurationSeconds()));
                    displays.handle(event);
                  };
              if (Bukkit.isPrimaryThread()) action.run();
              else plugin.getServer().getScheduler().runTask(plugin, action);
            });
    ticker = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    worlds.prepare();
    logger.info("[Games] Runtime storage cleanup scheduled before first instance creation.");
  }

  @Override
  public void stop() {
    if (ticker != null) ticker.cancel();
    HandlerList.unregisterAll(this);
    HandlerList.unregisterAll(waitingListener);
    HandlerList.unregisterAll(playListener);
    lobby.shutdown();
    displays.clear();
    games.destroyAll("plugin-stop");
    closeSubscription();
    events.clear();
    endingDeadlines.clear();
    endingCleanups.clear();
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    disconnect(event.getPlayer().getUniqueId());
    waitingListener.forget(event.getPlayer().getUniqueId());
    playListener.forget(event.getPlayer().getUniqueId());
  }

  @EventHandler
  public void onKick(PlayerKickEvent event) {
    disconnect(event.getPlayer().getUniqueId());
    waitingListener.forget(event.getPlayer().getUniqueId());
    playListener.forget(event.getPlayer().getUniqueId());
  }

  private void disconnect(java.util.UUID playerId) {
    var game = games.byPlayer(playerId).orElse(null);
    deaths.disconnect(playerId);
    lobby.disconnect(playerId);
    if (game != null && game.playerIds().isEmpty()) lobby.stopGame(game.id(), "empty-playing-game");
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    players.sanitizeJoin(
        event.getPlayer(), games.byPlayer(event.getPlayer().getUniqueId()).isPresent());
  }

  private void tick() {
    ticks++;
    if (ticks % 20 == 0) {
      lobby.tick();
      respawns.tick();
      finishEndedGames();
    }
    int refresh = configurations.snapshot().game().scoreboardRefreshTicks();
    if (ticks % refresh == 0)
      games.all().stream()
          .filter(
              game ->
                  game.state() == fr.heneria.bedwars.api.game.GameState.WAITING
                      || game.state() == fr.heneria.bedwars.api.game.GameState.STARTING
                      || game.state() == fr.heneria.bedwars.api.game.GameState.PLAYING
                      || game.state() == fr.heneria.bedwars.api.game.GameState.ENDING)
          .forEach(displays::refresh);
  }

  private void finishEndedGames() {
    java.time.Instant now = java.time.Instant.now();
    for (var entry : java.util.Map.copyOf(endingDeadlines).entrySet()) {
      if (now.isBefore(entry.getValue()) || !endingCleanups.add(entry.getKey())) continue;
      lobby
          .stopGame(entry.getKey(), "last-team-alive")
          .whenComplete(
              (result, failure) -> {
                endingDeadlines.remove(entry.getKey());
                endingCleanups.remove(entry.getKey());
              });
    }
  }

  private void closeSubscription() {
    if (eventSubscription == null) return;
    try {
      eventSubscription.close();
    } catch (Exception exception) {
      logger.error("[Games] Unable to close the internal event subscription", exception);
    }
    eventSubscription = null;
  }
}
