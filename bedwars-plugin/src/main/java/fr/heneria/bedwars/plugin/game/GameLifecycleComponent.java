package fr.heneria.bedwars.plugin.game;

import fr.heneria.bedwars.core.game.GameInstanceManager;
import fr.heneria.bedwars.core.game.event.GameEventBus;
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
  private final BukkitRuntimePlayerGateway players;
  private final ConfigurationService configurations;
  private final ProjectLogger logger;
  private AutoCloseable eventSubscription;
  private BukkitTask ticker;
  private long ticks;

  public GameLifecycleComponent(
      JavaPlugin plugin,
      GameInstanceManager games,
      BukkitRuntimeWorldService worlds,
      GameEventBus events,
      GameLobbyService lobby,
      BukkitGameDisplayService displays,
      GameWaitingListener waitingListener,
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
    eventSubscription =
        events.subscribe(
            event -> {
              if (Bukkit.isPrimaryThread()) displays.handle(event);
              else plugin.getServer().getScheduler().runTask(plugin, () -> displays.handle(event));
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
    lobby.shutdown();
    displays.clear();
    games.destroyAll("plugin-stop");
    closeSubscription();
    events.clear();
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    lobby.disconnect(event.getPlayer().getUniqueId());
    waitingListener.forget(event.getPlayer().getUniqueId());
  }

  @EventHandler
  public void onKick(PlayerKickEvent event) {
    lobby.disconnect(event.getPlayer().getUniqueId());
    waitingListener.forget(event.getPlayer().getUniqueId());
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    players.sanitizeJoin(
        event.getPlayer(), games.byPlayer(event.getPlayer().getUniqueId()).isPresent());
  }

  private void tick() {
    ticks++;
    if (ticks % 20 == 0) lobby.tick();
    int refresh = configurations.snapshot().game().scoreboardRefreshTicks();
    if (ticks % refresh == 0)
      games.all().stream()
          .filter(
              game ->
                  game.state() == fr.heneria.bedwars.api.game.GameState.WAITING
                      || game.state() == fr.heneria.bedwars.api.game.GameState.STARTING)
          .forEach(displays::refresh);
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
