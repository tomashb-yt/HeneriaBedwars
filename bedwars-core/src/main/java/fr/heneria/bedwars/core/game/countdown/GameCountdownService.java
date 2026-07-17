package fr.heneria.bedwars.core.game.countdown;

import fr.heneria.bedwars.api.game.GameState;
import fr.heneria.bedwars.core.config.GameSettings;
import fr.heneria.bedwars.core.game.GameId;
import fr.heneria.bedwars.core.game.GameInstance;
import fr.heneria.bedwars.core.game.GameInstanceManager;
import fr.heneria.bedwars.core.game.event.GameCountdownAccelerateEvent;
import fr.heneria.bedwars.core.game.event.GameCountdownCancelEvent;
import fr.heneria.bedwars.core.game.event.GameCountdownStartEvent;
import fr.heneria.bedwars.core.game.event.GameCountdownTickEvent;
import fr.heneria.bedwars.core.game.event.GameEventBus;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Pure countdown coordinator driven by a single external one-second ticker.
 *
 * <p>All state transitions pass through {@link GameInstanceManager}. This service never schedules
 * platform tasks and never starts BedWars mechanics when {@code PLAYING} is reached.
 */
public final class GameCountdownService {
  private final GameInstanceManager games;
  private final GameEventBus events;
  private final Supplier<GameSettings> settings;
  private final Clock clock;
  private final Map<GameId, GameCountdown> countdowns = new LinkedHashMap<>();

  public GameCountdownService(
      GameInstanceManager games,
      GameEventBus events,
      Supplier<GameSettings> settings,
      Clock clock) {
    this.games = games;
    this.events = events;
    this.settings = settings;
    this.clock = clock;
  }

  /** Starts a normal countdown. The configured minimum must already be reached. */
  public synchronized CountdownOperationResult start(GameId gameId) {
    return start(gameId, false);
  }

  /** Starts a countdown, optionally ignoring the minimum for administrative testing. */
  public synchronized CountdownOperationResult start(GameId gameId, boolean forced) {
    GameInstance game = games.find(gameId).orElse(null);
    if (game == null)
      return CountdownOperationResult.failure(
          CountdownOperationCode.GAME_NOT_FOUND, gameId.toString());
    if (game.state() != GameState.WAITING)
      return CountdownOperationResult.failure(
          game.state() == GameState.STARTING
              ? CountdownOperationCode.COUNTDOWN_ALREADY_RUNNING
              : CountdownOperationCode.INVALID_STATE,
          game.state().name());
    int minimum = game.arena().definition().minimumPlayers();
    if (!forced && game.playerIds().size() < minimum)
      return CountdownOperationResult.failure(
          CountdownOperationCode.MINIMUM_PLAYERS_NOT_REACHED, Integer.toString(minimum));
    if (countdowns.containsKey(gameId))
      return CountdownOperationResult.failure(
          CountdownOperationCode.COUNTDOWN_ALREADY_RUNNING, gameId.toString());
    int duration = settings.get().normalCountdownSeconds();
    var transitioned = games.transition(gameId, GameState.STARTING);
    if (!transitioned.successful())
      return CountdownOperationResult.failure(
          CountdownOperationCode.INVALID_STATE, transitioned.detail());
    GameCountdown countdown = new GameCountdown(gameId, duration, minimum, forced, now());
    countdowns.put(gameId, countdown);
    game.timer("countdown-seconds", duration, now());
    events.publish(new GameCountdownStartEvent(gameId, duration, forced, now()));
    return CountdownOperationResult.success(countdown.snapshot());
  }

  /** Immediately passes through STARTING to PLAYING for explicit development-only force starts. */
  public synchronized CountdownOperationResult forcePlaying(GameId gameId) {
    if (!settings.get().forcedStartEnabled())
      return CountdownOperationResult.failure(
          CountdownOperationCode.FORCED_START_DISABLED, gameId.toString());
    GameInstance game = games.find(gameId).orElse(null);
    if (game == null)
      return CountdownOperationResult.failure(
          CountdownOperationCode.GAME_NOT_FOUND, gameId.toString());
    if (game.state() == GameState.WAITING) {
      CountdownOperationResult started = start(gameId, true);
      if (!started.successful()) return started;
    }
    if (game.state() != GameState.STARTING)
      return CountdownOperationResult.failure(
          CountdownOperationCode.INVALID_STATE, game.state().name());
    GameCountdown countdown = countdowns.remove(gameId);
    if (countdown != null) countdown.cancel();
    game.timer("countdown-seconds", 0, now());
    var transitioned = games.transition(gameId, GameState.PLAYING);
    return transitioned.successful()
        ? CountdownOperationResult.success(countdown == null ? null : countdown.snapshot())
        : CountdownOperationResult.failure(
            CountdownOperationCode.INTERNAL_ERROR, transitioned.detail());
  }

  /** Cancels one active countdown and returns the game to WAITING. */
  public synchronized CountdownOperationResult cancel(GameId gameId, String reason) {
    GameCountdown countdown = countdowns.remove(gameId);
    if (countdown == null)
      return CountdownOperationResult.failure(
          CountdownOperationCode.COUNTDOWN_NOT_RUNNING, gameId.toString());
    CountdownSnapshot snapshot = countdown.cancel();
    games.find(gameId).ifPresent(game -> game.timer("countdown-seconds", 0, now()));
    var transitioned = games.transition(gameId, GameState.WAITING);
    if (!transitioned.successful())
      return CountdownOperationResult.failure(
          CountdownOperationCode.INVALID_STATE, transitioned.detail());
    events.publish(
        new GameCountdownCancelEvent(gameId, reason == null ? "cancelled" : reason, now()));
    return CountdownOperationResult.success(snapshot);
  }

  /**
   * Re-evaluates automatic start, cancellation and full-game acceleration after membership changes.
   */
  public synchronized void playerCountChanged(GameInstance game) {
    GameSettings current = settings.get();
    int players = game.playerIds().size();
    int minimum = game.arena().definition().minimumPlayers();
    int maximum = game.arena().definition().maximumPlayers();
    if (game.state() == GameState.WAITING && current.countdownEnabled() && players >= minimum) {
      start(game.id());
      return;
    }
    if (game.state() != GameState.STARTING) return;
    if (players < minimum && current.cancelBelowMinimum()) {
      cancel(game.id(), "minimum-players");
      return;
    }
    GameCountdown countdown = countdowns.get(game.id());
    if (countdown != null
        && players >= maximum
        && countdown.accelerateTo(current.fullGameCountdownSeconds())) {
      CountdownSnapshot snapshot = countdown.snapshot();
      game.timer("countdown-seconds", snapshot.remainingSeconds(), now());
      events.publish(
          new GameCountdownAccelerateEvent(game.id(), snapshot.remainingSeconds(), now()));
    }
  }

  /** Advances every live countdown exactly once. Expected caller cadence: one second. */
  public synchronized void tick() {
    for (GameId gameId : List.copyOf(countdowns.keySet())) {
      GameCountdown countdown = countdowns.get(gameId);
      GameInstance game = games.find(gameId).orElse(null);
      if (countdown == null) continue;
      if (game == null || game.state() != GameState.STARTING) {
        countdowns.remove(gameId);
        continue;
      }
      if (game.playerIds().size() < game.arena().definition().minimumPlayers()
          && settings.get().cancelBelowMinimum()) {
        cancel(gameId, "minimum-players");
        continue;
      }
      CountdownSnapshot snapshot = countdown.tick();
      game.timer("countdown-seconds", snapshot.remainingSeconds(), now());
      events.publish(
          new GameCountdownTickEvent(
              gameId, snapshot.remainingSeconds(), snapshot.initialDuration(), now()));
      if (snapshot.status() == GameCountdownStatus.COMPLETED) {
        countdowns.remove(gameId);
        games.transition(gameId, GameState.PLAYING);
      }
    }
  }

  public synchronized Optional<CountdownSnapshot> snapshot(GameId gameId) {
    return Optional.ofNullable(countdowns.get(gameId)).map(GameCountdown::snapshot);
  }

  public synchronized Map<GameId, CountdownSnapshot> snapshots() {
    Map<GameId, CountdownSnapshot> values = new LinkedHashMap<>();
    countdowns.forEach((id, countdown) -> values.put(id, countdown.snapshot()));
    return Map.copyOf(values);
  }

  public synchronized void cancelAll(String reason) {
    for (GameId id : List.copyOf(countdowns.keySet())) cancel(id, reason);
  }

  private Instant now() {
    return clock.instant();
  }
}
