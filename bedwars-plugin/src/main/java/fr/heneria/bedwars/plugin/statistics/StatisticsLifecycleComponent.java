package fr.heneria.bedwars.plugin.statistics;

import fr.heneria.bedwars.core.game.GameInstanceManager;
import fr.heneria.bedwars.core.game.event.GameEventBus;
import fr.heneria.bedwars.core.game.event.GameVictoryEvent;
import fr.heneria.bedwars.core.lifecycle.LifecycleComponent;
import fr.heneria.bedwars.core.logging.ProjectLogger;
import fr.heneria.bedwars.core.statistics.MatchRecordResult;
import fr.heneria.bedwars.core.statistics.StatisticsService;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Captures victorious runtime matches before their temporary instance is recycled. */
public final class StatisticsLifecycleComponent implements LifecycleComponent, Listener {
  private final JavaPlugin plugin;
  private final StatisticsService statistics;
  private final GameInstanceManager games;
  private final GameEventBus events;
  private final ProjectLogger logger;
  private AutoCloseable subscription;

  public StatisticsLifecycleComponent(
      JavaPlugin plugin,
      StatisticsService statistics,
      GameInstanceManager games,
      GameEventBus events,
      ProjectLogger logger) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.statistics = Objects.requireNonNull(statistics, "statistics");
    this.games = Objects.requireNonNull(games, "games");
    this.events = Objects.requireNonNull(events, "events");
    this.logger = Objects.requireNonNull(logger, "logger");
  }

  @Override
  public String name() {
    return "statistics";
  }

  @Override
  public void start() {
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
    Bukkit.getOnlinePlayers().forEach(this::remember);
    statistics
        .initialize()
        .whenComplete(
            (ignored, failure) -> {
              if (failure == null) logger.info("[Statistics] Storage initialized.");
              else logger.error("[Statistics] Storage initialization failed", unwrap(failure));
            });
    subscription =
        events.subscribe(
            event -> {
              if (!(event instanceof GameVictoryEvent victory)) return;
              games
                  .find(victory.gameId())
                  .ifPresent(
                      game ->
                          statistics
                              .record(game, victory.teamId(), victory.occurredAt())
                              .whenComplete(
                                  (result, failure) -> {
                                    if (failure != null) {
                                      logger.error(
                                          "[Statistics] Match persistence failed for "
                                              + victory.gameId(),
                                          unwrap(failure));
                                    } else if (result == MatchRecordResult.RECORDED) {
                                      logger.info(
                                          "[Statistics] Recorded completed match "
                                              + victory.gameId());
                                    }
                                  }));
            });
  }

  @Override
  public void stop() throws Exception {
    HandlerList.unregisterAll(this);
    if (subscription != null) subscription.close();
    statistics.close();
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    remember(event.getPlayer());
  }

  private void remember(Player player) {
    statistics
        .rememberPlayer(player.getUniqueId(), player.getName())
        .whenComplete(
            (ignored, failure) -> {
              if (failure != null)
                logger.error(
                    "[Statistics] Unable to remember player " + player.getUniqueId(),
                    unwrap(failure));
            });
  }

  private static Throwable unwrap(Throwable failure) {
    return failure.getCause() == null ? failure : failure.getCause();
  }
}
