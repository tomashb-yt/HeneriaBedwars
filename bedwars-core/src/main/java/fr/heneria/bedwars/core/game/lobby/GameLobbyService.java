package fr.heneria.bedwars.core.game.lobby;

import fr.heneria.bedwars.api.game.GameState;
import fr.heneria.bedwars.core.config.GameSettings;
import fr.heneria.bedwars.core.game.GameId;
import fr.heneria.bedwars.core.game.GameInstance;
import fr.heneria.bedwars.core.game.GameInstanceManager;
import fr.heneria.bedwars.core.game.GameOperationCode;
import fr.heneria.bedwars.core.game.GameOperationResult;
import fr.heneria.bedwars.core.game.WaitingPlayerContext;
import fr.heneria.bedwars.core.game.countdown.CountdownOperationResult;
import fr.heneria.bedwars.core.game.countdown.GameCountdownService;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * Authoritative pre-game use-case service for joins, leaves, countdowns and empty cleanup.
 *
 * <p>The service is thread-safe, stores no Bukkit object and is ticked once per second by the
 * platform. Empty-instance deadlines and countdowns therefore require no per-player or per-instance
 * scheduler task.
 */
public final class GameLobbyService {
  private final GameInstanceManager games;
  private final GameCountdownService countdowns;
  private final Supplier<GameSettings> settings;
  private final Clock clock;
  private final Map<GameId, Instant> emptyDeadlines = new LinkedHashMap<>();
  private final Set<GameId> emptyDestructions = new HashSet<>();
  private volatile boolean acceptingJoins = true;

  public GameLobbyService(
      GameInstanceManager games,
      GameCountdownService countdowns,
      Supplier<GameSettings> settings,
      Clock clock) {
    this.games = games;
    this.countdowns = countdowns;
    this.settings = settings;
    this.clock = clock;
  }

  /** Performs a complete join and only keeps membership after a successful platform teleport. */
  public CompletionStage<GameJoinResult> join(GameId gameId, UUID playerId) {
    if (!acceptingJoins)
      return java.util.concurrent.CompletableFuture.completedFuture(
          GameJoinResult.failure(GameJoinCode.RUNTIME_STOPPING, null, gameId.toString()));
    GameInstance game = games.find(gameId).orElse(null);
    if (game == null)
      return java.util.concurrent.CompletableFuture.completedFuture(
          GameJoinResult.failure(GameJoinCode.GAME_NOT_FOUND, null, gameId.toString()));
    if (game.state() == GameState.STARTING && !settings.get().allowJoinDuringCountdown())
      return java.util.concurrent.CompletableFuture.completedFuture(
          GameJoinResult.failure(GameJoinCode.GAME_NOT_JOINABLE, game, game.state().name()));
    WaitingPlayerContext context = waitingContext(game);
    return games
        .join(gameId, playerId, context)
        .thenApply(
            result -> {
              if (!result.successful()) return mapJoin(result);
              synchronized (this) {
                emptyDeadlines.remove(gameId);
                emptyDestructions.remove(gameId);
              }
              countdowns.playerCountChanged(game);
              return GameJoinResult.success(game);
            });
  }

  /** Removes a player, restores their in-memory snapshot and re-evaluates the countdown. */
  public CompletionStage<GameLeaveResult> leave(UUID playerId) {
    GameInstance game = games.byPlayer(playerId).orElse(null);
    if (game == null)
      return java.util.concurrent.CompletableFuture.completedFuture(
          GameLeaveResult.failure(GameLeaveCode.PLAYER_NOT_IN_GAME, null, playerId.toString()));
    return games
        .leave(playerId)
        .thenApply(
            result -> {
              if (!result.successful())
                return GameLeaveResult.failure(
                    result.code() == GameOperationCode.RESTORE_FAILED
                        ? GameLeaveCode.RESTORE_FAILED
                        : GameLeaveCode.INTERNAL_ERROR,
                    game,
                    result.detail());
              afterDeparture(game);
              return GameLeaveResult.success(game);
            });
  }

  /** Treats a pre-game disconnect as a definitive departure without an offline teleport. */
  public GameLeaveResult disconnect(UUID playerId) {
    GameInstance game = games.byPlayer(playerId).orElse(null);
    if (game == null)
      return GameLeaveResult.failure(GameLeaveCode.PLAYER_NOT_IN_GAME, null, playerId.toString());
    GameOperationResult result = games.disconnect(playerId);
    if (!result.successful())
      return GameLeaveResult.failure(GameLeaveCode.INTERNAL_ERROR, game, result.detail());
    afterDeparture(game);
    return GameLeaveResult.success(game);
  }

  public CountdownOperationResult start(GameId gameId, boolean forcePlaying) {
    return forcePlaying ? countdowns.forcePlaying(gameId) : countdowns.start(gameId);
  }

  public CountdownOperationResult cancelCountdown(GameId gameId, String reason) {
    return countdowns.cancel(gameId, reason);
  }

  /** Cancels all pre-game state and delegates deterministic world cleanup to the manager. */
  public CompletionStage<GameOperationResult> stopGame(GameId gameId, String reason) {
    synchronized (this) {
      emptyDeadlines.remove(gameId);
      emptyDestructions.remove(gameId);
    }
    if (countdowns.snapshot(gameId).isPresent()) countdowns.cancel(gameId, "game-stop");
    return games.destroy(gameId, reason);
  }

  /** Advances countdowns and all empty-instance deadlines from one central platform task. */
  public void tick() {
    countdowns.tick();
    GameSettings current = settings.get();
    if (!current.destroyEmptyInstance()) return;
    for (GameInstance game : games.all()) {
      if (game.state() != GameState.WAITING || !game.playerIds().isEmpty()) {
        synchronized (this) {
          emptyDeadlines.remove(game.id());
        }
        continue;
      }
      Instant deadline;
      synchronized (this) {
        deadline =
            emptyDeadlines.computeIfAbsent(
                game.id(), id -> now().plusSeconds(current.emptyDestroyDelaySeconds()));
        if (now().isBefore(deadline) || !emptyDestructions.add(game.id())) continue;
      }
      stopGame(game.id(), "empty-instance")
          .whenComplete(
              (result, failure) -> {
                synchronized (this) {
                  emptyDeadlines.remove(game.id());
                  emptyDestructions.remove(game.id());
                }
              });
    }
  }

  /** Blocks new joins and clears all scheduler-owned runtime state before plugin shutdown. */
  public synchronized void shutdown() {
    acceptingJoins = false;
    countdowns.cancelAll("plugin-stop");
    emptyDeadlines.clear();
    emptyDestructions.clear();
  }

  public synchronized Map<GameId, Instant> emptyDeadlines() {
    return Map.copyOf(emptyDeadlines);
  }

  public boolean acceptingJoins() {
    return acceptingJoins;
  }

  private void afterDeparture(GameInstance game) {
    countdowns.playerCountChanged(game);
    if (game.state() == GameState.WAITING
        && game.playerIds().isEmpty()
        && settings.get().destroyEmptyInstance()) {
      synchronized (this) {
        emptyDeadlines.put(game.id(), now().plusSeconds(settings.get().emptyDestroyDelaySeconds()));
      }
    }
  }

  public WaitingPlayerContext waitingContext(GameInstance game) {
    GameSettings current = settings.get();
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("game_id", game.id().shortId());
    values.put("full_game_id", game.id().toString());
    values.put("arena_id", game.arena().definition().id().value());
    values.put("arena_name", game.arena().definition().displayName());
    values.put("players", game.playerIds().size() + 1);
    values.put("minimum_players", game.arena().definition().minimumPlayers());
    values.put("maximum_players", game.arena().definition().maximumPlayers());
    values.put(
        "countdown", countdowns.snapshot(game.id()).map(c -> c.remainingSeconds()).orElse(0));
    values.put("state", game.state().name());
    return new WaitingPlayerContext(
        current.waitingGameMode(), current.leaveItemSlot(), current.infoItemSlot(), values);
  }

  private static GameJoinResult mapJoin(GameOperationResult result) {
    GameJoinCode code =
        switch (result.code()) {
          case NOT_FOUND -> GameJoinCode.GAME_NOT_FOUND;
          case PLAYER_OCCUPIED -> GameJoinCode.PLAYER_ALREADY_IN_GAME;
          case GAME_FULL -> GameJoinCode.GAME_FULL;
          case INVALID_STATE ->
              result.detail().equals("world-missing")
                  ? GameJoinCode.WORLD_NOT_READY
                  : GameJoinCode.GAME_NOT_JOINABLE;
          case TELEPORT_FAILED -> GameJoinCode.TELEPORT_FAILED;
          default -> GameJoinCode.INTERNAL_ERROR;
        };
    return GameJoinResult.failure(code, result.instance().orElse(null), result.detail());
  }

  private Instant now() {
    return clock.instant();
  }
}
