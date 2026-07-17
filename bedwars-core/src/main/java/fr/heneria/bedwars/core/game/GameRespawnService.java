package fr.heneria.bedwars.core.game;

import fr.heneria.bedwars.api.game.GameState;
import fr.heneria.bedwars.core.game.event.GameEventBus;
import fr.heneria.bedwars.core.game.event.PlayerGameRespawnEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.function.IntSupplier;

/** Central-ticker respawn coordinator; it creates no scheduler task. */
public final class GameRespawnService {
  private final GameInstanceManager games;
  private final GameEventBus events;
  private final Clock clock;
  private final IntSupplier protectionSeconds;

  public GameRespawnService(
      GameInstanceManager games, GameEventBus events, Clock clock, IntSupplier protectionSeconds) {
    this.games = Objects.requireNonNull(games, "games");
    this.events = Objects.requireNonNull(events, "events");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.protectionSeconds = Objects.requireNonNull(protectionSeconds, "protectionSeconds");
  }

  public void tick() {
    Instant now = clock.instant();
    for (GameInstance game : games.all()) {
      if (game.state() != GameState.PLAYING) continue;
      for (RuntimePlayer player : game.runtimePlayers()) {
        if (!player.respawning() || player.respawnAt().filter(due -> due.isAfter(now)).isPresent())
          continue;
        player.completeRespawn(now.plusSeconds(Math.max(0, protectionSeconds.getAsInt())));
        events.publish(new PlayerGameRespawnEvent(game.id(), player.playerId(), now));
      }
    }
  }
}
