package fr.heneria.zombie.core.instance;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Thread-safe aggregate owning the lifecycle and membership of one game instance. */
public final class GameInstance {

  private final UUID id;
  private final String mapId;
  private final GameInstanceOptions options;
  private final Instant createdAt;
  private final Set<UUID> players = new LinkedHashSet<>();
  private GameInstanceState state = GameInstanceState.CREATING;
  private WorldInstanceHandle world;
  private String lastError;

  /**
   * Creates a preparing instance.
   *
   * @param id unique identifier
   * @param mapId stable map identifier
   * @param options creation options
   * @param createdAt creation instant
   */
  public GameInstance(UUID id, String mapId, GameInstanceOptions options, Instant createdAt) {
    this.id = Objects.requireNonNull(id, "id");
    this.mapId = validateMapId(mapId);
    this.options = Objects.requireNonNull(options, "options");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
  }

  /**
   * Applies a valid lifecycle transition.
   *
   * @param target target state
   * @throws InvalidInstanceTransitionException when the transition is invalid
   */
  public synchronized void transitionTo(GameInstanceState target) {
    Objects.requireNonNull(target, "target");
    if (!state.canTransitionTo(target)) {
      throw new InvalidInstanceTransitionException(state, target);
    }
    state = target;
  }

  /**
   * Completes world preparation and enters the waiting state.
   *
   * @param preparedWorld prepared world
   */
  public synchronized void markPrepared(WorldInstanceHandle preparedWorld) {
    if (state != GameInstanceState.CREATING) {
      throw new InvalidInstanceTransitionException(state, GameInstanceState.WAITING);
    }
    world = Objects.requireNonNull(preparedWorld, "preparedWorld");
    state = GameInstanceState.WAITING;
  }

  /**
   * Marks the instance as failed when possible.
   *
   * @param error diagnostic message
   */
  public synchronized void markError(String error) {
    lastError = Objects.requireNonNull(error, "error");
    if (state != GameInstanceState.ERROR && state.canTransitionTo(GameInstanceState.ERROR)) {
      state = GameInstanceState.ERROR;
    }
  }

  /**
   * Adds one player to an accessible instance.
   *
   * @param playerId player identifier
   * @return join result
   */
  public synchronized InstanceJoinResult addPlayer(UUID playerId) {
    Objects.requireNonNull(playerId, "playerId");
    if (state != GameInstanceState.WAITING
        && state != GameInstanceState.STARTING
        && state != GameInstanceState.RUNNING) {
      return InstanceJoinResult.NOT_JOINABLE;
    }
    if (players.contains(playerId)) {
      return InstanceJoinResult.ALREADY_JOINED;
    }
    if (options.access() == InstanceAccess.PRIVATE
        && options.owner().filter(playerId::equals).isEmpty()) {
      return InstanceJoinResult.ACCESS_DENIED;
    }
    if (players.size() >= options.maximumPlayers()) {
      return InstanceJoinResult.FULL;
    }
    players.add(playerId);
    return InstanceJoinResult.JOINED;
  }

  /**
   * Removes one member.
   *
   * @param playerId player identifier
   * @return whether the member was present
   */
  public synchronized boolean removePlayer(UUID playerId) {
    return players.remove(Objects.requireNonNull(playerId, "playerId"));
  }

  /**
   * Returns whether a player belongs to this instance.
   *
   * @param playerId player identifier
   * @return membership
   */
  public synchronized boolean contains(UUID playerId) {
    return players.contains(playerId);
  }

  /**
   * Returns the prepared world.
   *
   * @return optional world
   */
  public synchronized Optional<WorldInstanceHandle> world() {
    return Optional.ofNullable(world);
  }

  /**
   * Returns an immutable snapshot.
   *
   * @return current snapshot
   */
  public synchronized GameInstanceSnapshot snapshot() {
    return new GameInstanceSnapshot(
        id,
        mapId,
        Optional.ofNullable(world).map(WorldInstanceHandle::worldName),
        state,
        players,
        options.maximumPlayers(),
        createdAt,
        options.owner(),
        options.access(),
        Optional.ofNullable(lastError));
  }

  /**
   * Returns the identifier.
   *
   * @return instance identifier
   */
  public UUID id() {
    return id;
  }

  private static String validateMapId(String mapId) {
    Objects.requireNonNull(mapId, "mapId");
    if (!mapId.matches("[a-z0-9][a-z0-9_-]{0,63}")) {
      throw new IllegalArgumentException("Invalid map id: " + mapId);
    }
    return mapId;
  }
}
