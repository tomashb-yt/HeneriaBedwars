package fr.heneria.bedwars.plugin.statistics;

import fr.heneria.bedwars.core.game.GameInstanceManager;
import fr.heneria.bedwars.core.game.event.GameEventBus;
import fr.heneria.bedwars.core.game.event.GameVictoryEvent;
import fr.heneria.bedwars.core.lifecycle.LifecycleComponent;
import fr.heneria.bedwars.core.logging.ProjectLogger;
import fr.heneria.bedwars.core.statistics.MatchRecordResult;
import fr.heneria.bedwars.core.statistics.StatisticsService;
import java.util.Objects;

/** Captures victorious runtime matches before their temporary instance is recycled. */
public final class StatisticsLifecycleComponent implements LifecycleComponent {
  private final StatisticsService statistics;
  private final GameInstanceManager games;
  private final GameEventBus events;
  private final ProjectLogger logger;
  private AutoCloseable subscription;

  public StatisticsLifecycleComponent(
      StatisticsService statistics,
      GameInstanceManager games,
      GameEventBus events,
      ProjectLogger logger) {
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
    if (subscription != null) subscription.close();
    statistics.close();
  }

  private static Throwable unwrap(Throwable failure) {
    return failure.getCause() == null ? failure : failure.getCause();
  }
}
