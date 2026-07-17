package fr.heneria.bedwars.core.game;

import fr.heneria.bedwars.api.game.GameSnapshot;
import fr.heneria.bedwars.api.game.GameState;
import fr.heneria.bedwars.core.arena.ArenaDefinition;
import fr.heneria.bedwars.core.arena.ArenaService;
import fr.heneria.bedwars.core.game.event.GameCreateEvent;
import fr.heneria.bedwars.core.game.event.GameDestroyEvent;
import fr.heneria.bedwars.core.game.event.GameEndEvent;
import fr.heneria.bedwars.core.game.event.GameEventBus;
import fr.heneria.bedwars.core.game.event.GameStartEvent;
import fr.heneria.bedwars.core.game.event.GameWaitingEvent;
import fr.heneria.bedwars.core.game.event.PlayerGameJoinEvent;
import fr.heneria.bedwars.core.game.event.PlayerGameLeaveEvent;
import fr.heneria.bedwars.core.map.MapTemplate;
import fr.heneria.bedwars.core.map.MapTemplateService;
import fr.heneria.bedwars.core.map.MapType;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Authoritative in-memory registry and lifecycle coordinator for all live games.
 *
 * <p>Indexes are reserved before asynchronous cloning begins, preventing two creations for one
 * arena. Failed creations are rolled back to {@code DESTROYED} and removed atomically.
 */
public final class GameInstanceManager {
  private final Function<String, Optional<ArenaDefinition>> arenaFinder;
  private final Predicate<ArenaDefinition> validArena;
  private final Function<String, Optional<MapTemplate>> mapFinder;
  private final RuntimeWorldService worlds;
  private final RuntimePlayerGateway players;
  private final GameEventBus events;
  private final Clock clock;
  private final Supplier<UUID> ids;
  private final GameLocationMapper locations = new GameLocationMapper();
  private final Map<GameId, GameInstance> instances = new LinkedHashMap<>();
  private final Map<String, GameId> byArena = new LinkedHashMap<>();
  private final Map<UUID, GameId> byPlayer = new LinkedHashMap<>();
  private final Set<GameId> destroying = new HashSet<>();

  public GameInstanceManager(
      ArenaService arenas,
      MapTemplateService maps,
      RuntimeWorldService worlds,
      RuntimePlayerGateway players,
      GameEventBus events,
      Clock clock) {
    this(
        arenas::find,
        arena -> arena.enabled() && arenas.validate(arena).valid(),
        maps::find,
        worlds,
        players,
        events,
        clock,
        UUID::randomUUID);
  }

  GameInstanceManager(
      Function<String, Optional<ArenaDefinition>> arenaFinder,
      Predicate<ArenaDefinition> validArena,
      Function<String, Optional<MapTemplate>> mapFinder,
      RuntimeWorldService worlds,
      RuntimePlayerGateway players,
      GameEventBus events,
      Clock clock,
      Supplier<UUID> ids) {
    this.arenaFinder = arenaFinder;
    this.validArena = validArena;
    this.mapFinder = mapFinder;
    this.worlds = worlds;
    this.players = players;
    this.events = events;
    this.clock = clock;
    this.ids = ids;
  }

  public CompletionStage<GameOperationResult> create(String arenaId) {
    ArenaDefinition arena = arenaFinder.apply(arenaId).orElse(null);
    if (arena == null)
      return completed(GameOperationResult.failure(GameOperationCode.ARENA_NOT_FOUND, arenaId));
    if (!validArena.test(arena))
      return completed(GameOperationResult.failure(GameOperationCode.ARENA_INVALID, arenaId));
    String mapId = arena.template().orElse(null);
    if (mapId == null)
      return completed(GameOperationResult.failure(GameOperationCode.MAP_NOT_FOUND, arenaId));
    MapTemplate template = mapFinder.apply(mapId).orElse(null);
    if (template == null)
      return completed(GameOperationResult.failure(GameOperationCode.MAP_NOT_FOUND, mapId));
    if (template.type() != MapType.BEDWARS || !template.operational())
      return completed(GameOperationResult.failure(GameOperationCode.MAP_INVALID, mapId));

    GameInstance instance;
    synchronized (this) {
      String normalizedArena = arena.id().value();
      if (byArena.containsKey(normalizedArena))
        return completed(
            GameOperationResult.failure(GameOperationCode.ARENA_OCCUPIED, normalizedArena));
      GameId id = new GameId(ids.get());
      instance = new GameInstance(new RuntimeArena(id, arena, template), now());
      instances.put(id, instance);
      byArena.put(normalizedArena, id);
    }
    events.publish(new GameCreateEvent(instance.id(), arena.id().value(), now()));
    CompletableFuture<GameOperationResult> result = new CompletableFuture<>();
    worlds
        .create(instance.arena())
        .whenComplete(
            (world, failure) -> {
              if (failure != null) {
                rollbackCreation(instance);
                result.complete(
                    GameOperationResult.failure(
                        GameOperationCode.WORLD_CREATION_FAILED, rootMessage(failure)));
                return;
              }
              try {
                instance.attachWorld(world, now());
                instance.transition(GameState.WAITING, now());
                events.publish(new GameWaitingEvent(instance.id(), world.worldName(), now()));
                result.complete(GameOperationResult.success(instance));
              } catch (RuntimeException exception) {
                rollbackCreation(instance);
                worlds.destroy(world);
                result.complete(
                    GameOperationResult.failure(
                        GameOperationCode.INTERNAL_ERROR, exception.getMessage()));
              }
            });
    return result;
  }

  public CompletionStage<GameOperationResult> join(GameId gameId, UUID playerId) {
    GameInstance instance = find(gameId).orElse(null);
    WaitingPlayerContext context =
        instance == null
            ? new WaitingPlayerContext("ADVENTURE", 8, 4, Map.of())
            : new WaitingPlayerContext(
                "ADVENTURE",
                8,
                4,
                Map.of(
                    "game_id", instance.id().shortId(),
                    "arena_id", instance.arena().definition().id().value(),
                    "arena_name", instance.arena().definition().displayName(),
                    "players", instance.playerIds().size() + 1,
                    "minimum_players", instance.arena().definition().minimumPlayers(),
                    "maximum_players", instance.arena().definition().maximumPlayers(),
                    "countdown", 0,
                    "state", instance.state().name()));
    return join(gameId, playerId, context);
  }

  public CompletionStage<GameOperationResult> join(
      GameId gameId, UUID playerId, WaitingPlayerContext context) {
    GameInstance instance;
    RuntimeWorldHandle world;
    synchronized (this) {
      instance = instances.get(gameId);
      if (instance == null)
        return completed(
            GameOperationResult.failure(GameOperationCode.NOT_FOUND, gameId.toString()));
      if (byPlayer.containsKey(playerId))
        return completed(
            GameOperationResult.failure(GameOperationCode.PLAYER_OCCUPIED, playerId.toString()));
      if (instance.state() != GameState.WAITING && instance.state() != GameState.STARTING)
        return completed(
            GameOperationResult.failure(
                GameOperationCode.INVALID_STATE, instance, instance.state().name()));
      if (instance.playerIds().size() >= instance.arena().definition().maximumPlayers())
        return completed(
            GameOperationResult.failure(GameOperationCode.GAME_FULL, instance, gameId.toString()));
      world = instance.world().orElse(null);
      if (world == null)
        return completed(
            GameOperationResult.failure(
                GameOperationCode.INVALID_STATE, instance, "world-missing"));
      instance.addPlayer(playerId, now());
      byPlayer.put(playerId, gameId);
    }
    RuntimeLocation destination = locations.waiting(instance);
    return players
        .enter(playerId, world, destination, context)
        .handle(
            (teleported, failure) -> {
              if (failure != null || !Boolean.TRUE.equals(teleported)) {
                synchronized (this) {
                  instance.removePlayer(playerId, now());
                  byPlayer.remove(playerId);
                }
                return GameOperationResult.failure(
                    GameOperationCode.TELEPORT_FAILED,
                    instance,
                    failure == null ? playerId.toString() : rootMessage(failure));
              }
              events.publish(new PlayerGameJoinEvent(gameId, playerId, now()));
              return GameOperationResult.success(instance);
            });
  }

  public CompletionStage<GameOperationResult> leave(UUID playerId) {
    GameInstance instance;
    synchronized (this) {
      GameId gameId = byPlayer.remove(playerId);
      if (gameId == null)
        return completed(
            GameOperationResult.failure(GameOperationCode.NOT_FOUND, playerId.toString()));
      instance = instances.get(gameId);
      if (instance != null) instance.removePlayer(playerId, now());
    }
    if (instance == null)
      return completed(
          GameOperationResult.failure(GameOperationCode.NOT_FOUND, playerId.toString()));
    return players
        .leave(playerId)
        .handle(
            (restored, failure) -> {
              events.publish(new PlayerGameLeaveEvent(instance.id(), playerId, now()));
              if (failure != null || !Boolean.TRUE.equals(restored))
                return GameOperationResult.failure(
                    GameOperationCode.RESTORE_FAILED,
                    instance,
                    failure == null ? playerId.toString() : rootMessage(failure));
              return GameOperationResult.success(instance);
            });
  }

  /** Removes a disconnected player without attempting a platform teleport. */
  public GameOperationResult disconnect(UUID playerId) {
    GameInstance instance;
    synchronized (this) {
      GameId gameId = byPlayer.remove(playerId);
      if (gameId == null)
        return GameOperationResult.failure(GameOperationCode.NOT_FOUND, playerId.toString());
      instance = instances.get(gameId);
      if (instance == null)
        return GameOperationResult.failure(GameOperationCode.NOT_FOUND, playerId.toString());
      instance.removePlayer(playerId, now());
    }
    players.disconnect(playerId);
    events.publish(new PlayerGameLeaveEvent(instance.id(), playerId, now()));
    return GameOperationResult.success(instance);
  }

  public GameOperationResult transition(GameId id, GameState target) {
    GameInstance instance = find(id).orElse(null);
    if (instance == null)
      return GameOperationResult.failure(GameOperationCode.NOT_FOUND, id.toString());
    try {
      instance.transition(target, now());
      if (target == GameState.PLAYING) events.publish(new GameStartEvent(id, now()));
      if (target == GameState.ENDING)
        events.publish(new GameEndEvent(id, "state-transition", now()));
      return GameOperationResult.success(instance);
    } catch (GameTransitionException exception) {
      return GameOperationResult.failure(
          GameOperationCode.INVALID_STATE, instance, exception.getMessage());
    }
  }

  public CompletionStage<GameOperationResult> destroy(GameId id, String reason) {
    GameInstance instance = find(id).orElse(null);
    if (instance == null)
      return completed(GameOperationResult.failure(GameOperationCode.NOT_FOUND, id.toString()));
    synchronized (this) {
      if (!destroying.add(id))
        return completed(
            GameOperationResult.failure(
                GameOperationCode.OPERATION_IN_PROGRESS, instance, id.toString()));
      try {
        if (instance.state() == GameState.CREATING) {
          instance.transition(GameState.RESETTING, now());
        } else if (instance.state() != GameState.ENDING
            && instance.state() != GameState.RESETTING) {
          instance.transition(GameState.ENDING, now());
          events.publish(new GameEndEvent(id, reason == null ? "destroy" : reason, now()));
        }
        if (instance.state() == GameState.ENDING) instance.transition(GameState.RESETTING, now());
      } catch (GameTransitionException exception) {
        destroying.remove(id);
        return completed(
            GameOperationResult.failure(
                GameOperationCode.INVALID_STATE, instance, exception.getMessage()));
      }
      for (UUID playerId : instance.playerIds()) {
        byPlayer.remove(playerId);
        instance.removePlayer(playerId, now());
        players.leave(playerId);
        events.publish(new PlayerGameLeaveEvent(id, playerId, now()));
      }
    }
    RuntimeWorldHandle world = instance.world().orElse(null);
    CompletionStage<Void> destruction =
        world == null ? CompletableFuture.completedFuture(null) : worlds.destroy(world);
    return destruction.handle(
        (ignored, failure) -> {
          if (failure != null) {
            synchronized (this) {
              destroying.remove(id);
            }
            return GameOperationResult.failure(
                GameOperationCode.WORLD_DESTRUCTION_FAILED, instance, rootMessage(failure));
          }
          synchronized (this) {
            if (instance.state() == GameState.RESETTING)
              instance.transition(GameState.DESTROYED, now());
            instances.remove(id);
            byArena.remove(instance.arena().definition().id().value());
            destroying.remove(id);
          }
          events.publish(new GameDestroyEvent(id, now()));
          return GameOperationResult.success(instance);
        });
  }

  public synchronized Optional<GameInstance> find(GameId id) {
    return Optional.ofNullable(instances.get(id));
  }

  public synchronized Optional<GameInstance> byPlayer(UUID playerId) {
    return Optional.ofNullable(byPlayer.get(playerId)).map(instances::get);
  }

  public synchronized Optional<GameInstance> byArena(String arenaId) {
    if (arenaId == null) return Optional.empty();
    return Optional.ofNullable(byArena.get(arenaId.toLowerCase(java.util.Locale.ROOT)))
        .map(instances::get);
  }

  /** Resolves a full UUID or a unique UUID prefix without guessing on ambiguity. */
  public synchronized GameLookupResult lookup(String query) {
    if (query == null || query.isBlank()) return GameLookupResult.missing();
    String normalized = query.trim().toLowerCase(java.util.Locale.ROOT);
    try {
      GameInstance exact = instances.get(GameId.parse(normalized));
      if (exact != null) return GameLookupResult.found(exact);
    } catch (IllegalArgumentException ignored) {
      // A short prefix is an expected input.
    }
    List<GameInstance> matches =
        instances.values().stream()
            .filter(game -> game.id().toString().startsWith(normalized))
            .toList();
    if (matches.isEmpty()) return GameLookupResult.missing();
    if (matches.size() > 1) return GameLookupResult.ambiguous();
    return GameLookupResult.found(matches.get(0));
  }

  public synchronized List<GameInstance> all() {
    return instances.values().stream()
        .sorted(Comparator.comparing(instance -> instance.id().value()))
        .toList();
  }

  public List<GameSnapshot> snapshots() {
    Instant snapshotTime = now();
    return all().stream().map(instance -> instance.snapshot(snapshotTime)).toList();
  }

  public synchronized int size() {
    return instances.size();
  }

  /** Starts destruction of every live instance; used during deterministic plugin shutdown. */
  public CompletionStage<Void> destroyAll(String reason) {
    List<GameId> ids = all().stream().map(GameInstance::id).toList();
    CompletableFuture<?>[] operations =
        ids.stream()
            .map(id -> destroy(id, reason).toCompletableFuture())
            .toArray(CompletableFuture[]::new);
    return CompletableFuture.allOf(operations);
  }

  private void rollbackCreation(GameInstance instance) {
    synchronized (this) {
      if (instance.state() == GameState.CREATING) instance.transition(GameState.RESETTING, now());
      if (instance.state() == GameState.RESETTING) instance.transition(GameState.DESTROYED, now());
      instances.remove(instance.id());
      byArena.remove(instance.arena().definition().id().value());
    }
    events.publish(new GameDestroyEvent(instance.id(), now()));
  }

  private Instant now() {
    return clock.instant();
  }

  private static CompletionStage<GameOperationResult> completed(GameOperationResult result) {
    return CompletableFuture.completedFuture(result);
  }

  private static String rootMessage(Throwable failure) {
    Throwable current = failure;
    while (current.getCause() != null) current = current.getCause();
    return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
  }
}
