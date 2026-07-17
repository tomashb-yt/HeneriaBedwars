package fr.heneria.bedwars.core.game;

import fr.heneria.bedwars.core.game.event.BedDestroyedEvent;
import fr.heneria.bedwars.core.game.event.GameEventBus;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Atomic bed-destruction use case, independent from Bukkit block events. */
public final class GameBedService {
  private final GameInstanceManager games;
  private final GameEventBus events;
  private final Clock clock;

  public GameBedService(GameInstanceManager games, GameEventBus events, Clock clock) {
    this.games = Objects.requireNonNull(games, "games");
    this.events = Objects.requireNonNull(events, "events");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public BedDestroyResult destroy(UUID playerId, RuntimeBlockPosition position) {
    GameInstance game = games.byPlayer(playerId).orElse(null);
    if (game == null) return BedDestroyResult.of(BedDestroyCode.NOT_IN_GAME);
    Instant now = clock.instant();
    BedDestroyResult result = game.destroyBed(playerId, position, now);
    if (result.successful()) {
      RuntimeBed bed = result.bed().orElseThrow();
      events.publish(new BedDestroyedEvent(game.id(), bed.teamId(), playerId, now));
    }
    return result;
  }
}
