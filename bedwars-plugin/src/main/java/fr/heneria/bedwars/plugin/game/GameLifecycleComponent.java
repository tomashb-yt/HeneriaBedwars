package fr.heneria.bedwars.plugin.game;

import fr.heneria.bedwars.core.game.GameInstanceManager;
import fr.heneria.bedwars.core.game.event.GameEventBus;
import fr.heneria.bedwars.core.lifecycle.LifecycleComponent;
import fr.heneria.bedwars.core.logging.ProjectLogger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Registers runtime cleanup hooks and coordinates orphan recovery. */
public final class GameLifecycleComponent implements LifecycleComponent, Listener {
  private final JavaPlugin plugin;
  private final GameInstanceManager games;
  private final BukkitRuntimeWorldService worlds;
  private final GameEventBus events;
  private final ProjectLogger logger;

  public GameLifecycleComponent(
      JavaPlugin plugin,
      GameInstanceManager games,
      BukkitRuntimeWorldService worlds,
      GameEventBus events,
      ProjectLogger logger) {
    this.plugin = plugin;
    this.games = games;
    this.worlds = worlds;
    this.events = events;
    this.logger = logger;
  }

  @Override
  public String name() {
    return "game-instances";
  }

  @Override
  public void start() {
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
    worlds.prepare();
    logger.info("[Games] Runtime storage cleanup scheduled before first instance creation.");
  }

  @Override
  public void stop() {
    HandlerList.unregisterAll(this);
    games.destroyAll("plugin-stop");
    events.clear();
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    games.disconnect(event.getPlayer().getUniqueId());
  }

  @EventHandler
  public void onKick(PlayerKickEvent event) {
    games.disconnect(event.getPlayer().getUniqueId());
  }
}
