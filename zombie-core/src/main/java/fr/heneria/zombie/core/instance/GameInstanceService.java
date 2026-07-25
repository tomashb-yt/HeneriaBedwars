package fr.heneria.zombie.core.instance;

import java.time.Clock;
import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Application service coordinating instance allocation, world preparation and cleanup. */
public final class GameInstanceService {

  private final GameInstanceRegistry registry;
  private final WorldInstanceGateway worlds;
  private final IntSupplier maximumConcurrentGames;
  private final Supplier<Boolean> preserveFailedWorlds;
  private final Supplier<Duration> creationTimeout;
  private final Supplier<UUID> identifiers;
  private final Clock clock;
  private final Consumer<String> diagnostics;
  private final Object capacityLock = new Object();

  /**
   * Creates the service.
   *
   * @param registry live registry
   * @param worlds world gateway
   * @param maximumConcurrentGames configured capacity supplier
   * @param preserveFailedWorlds failure preservation supplier
   * @param creationTimeout preparation timeout supplier
   * @param identifiers identifier source
   * @param clock time source
   * @param diagnostics lifecycle diagnostic sink
   */
  public GameInstanceService(
      GameInstanceRegistry registry,
      WorldInstanceGateway worlds,
      IntSupplier maximumConcurrentGames,
      Supplier<Boolean> preserveFailedWorlds,
      Supplier<Duration> creationTimeout,
      Supplier<UUID> identifiers,
      Clock clock,
      Consumer<String> diagnostics) {
    this.registry = Objects.requireNonNull(registry, "registry");
    this.worlds = Objects.requireNonNull(worlds, "worlds");
    this.maximumConcurrentGames =
        Objects.requireNonNull(maximumConcurrentGames, "maximumConcurrentGames");
    this.preserveFailedWorlds =
        Objects.requireNonNull(preserveFailedWorlds, "preserveFailedWorlds");
    this.creationTimeout = Objects.requireNonNull(creationTimeout, "creationTimeout");
    this.identifiers = Objects.requireNonNull(identifiers, "identifiers");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
  }

  /**
   * Creates and prepares an isolated instance.
   *
   * @param mapId template identifier
   * @param options creation options
   * @return future prepared snapshot
   */
  public CompletableFuture<GameInstanceSnapshot> createInstance(
      String mapId, GameInstanceOptions options) {
    GameInstance instance;
    synchronized (capacityLock) {
      int maximum = maximumConcurrentGames.getAsInt();
      if (maximum != -1 && registry.size() >= maximum) {
        return CompletableFuture.failedFuture(
            new InstanceCreationException("Maximum concurrent games reached"));
      }
      instance = new GameInstance(identifiers.get(), mapId, options, clock.instant());
      registry.register(instance);
    }

    return worlds
        .prepare(instance.id(), mapId)
        .orTimeout(creationTimeout.get().toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
        .thenApply(
            handle -> {
              instance.markPrepared(handle);
              return instance.snapshot();
            })
        .exceptionallyCompose(
            failure -> {
              Throwable cause = unwrap(failure);
              diagnostics.accept(
                  "Instance "
                      + instance.id()
                      + " failed during creation: "
                      + (cause.getMessage() == null
                          ? cause.getClass().getName()
                          : cause.getMessage()));
              instance.markError(
                  cause.getMessage() == null ? cause.getClass().getName() : cause.getMessage());
              return cleanupFailedCreation(instance)
                  .handle(
                      (ignored, cleanupFailure) -> {
                        registry.remove(instance);
                        InstanceCreationException result =
                            new InstanceCreationException(
                                "Could not create instance for map " + mapId, cause);
                        if (cleanupFailure != null) {
                          result.addSuppressed(unwrap(cleanupFailure));
                        }
                        throw new CompletionException(result);
                      });
            });
  }

  /**
   * Finds an instance.
   *
   * @param instanceId identifier
   * @return optional aggregate snapshot
   */
  public Optional<GameInstanceSnapshot> findInstance(UUID instanceId) {
    return registry.find(instanceId).map(GameInstance::snapshot);
  }

  /**
   * Returns every live instance.
   *
   * @return immutable snapshots
   */
  public Collection<GameInstanceSnapshot> getActiveInstances() {
    return registry.snapshots();
  }

  /**
   * Adds a player after cross-registry uniqueness has been checked by the session service.
   *
   * @param playerId player identifier
   * @param instanceId instance identifier
   * @return membership result
   */
  public InstanceJoinResult joinInstance(UUID playerId, UUID instanceId) {
    return registry
        .find(instanceId)
        .map(instance -> instance.addPlayer(playerId))
        .orElse(InstanceJoinResult.NOT_JOINABLE);
  }

  /**
   * Removes a player from their registered instance.
   *
   * @param playerId player identifier
   * @param instanceId expected instance identifier
   * @return whether membership existed
   */
  public boolean leaveInstance(UUID playerId, UUID instanceId) {
    return registry.find(instanceId).map(instance -> instance.removePlayer(playerId)).orElse(false);
  }

  /**
   * Closes an empty instance and safely cleans its world.
   *
   * @param instanceId identifier
   * @return future cleanup result
   */
  public CompletableFuture<Boolean> closeInstance(UUID instanceId) {
    Optional<GameInstance> found = registry.find(instanceId);
    if (found.isEmpty()) {
      return CompletableFuture.completedFuture(false);
    }
    GameInstance instance = found.get();
    GameInstanceSnapshot snapshot = instance.snapshot();
    if (!snapshot.players().isEmpty()) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("Cannot close an instance while players remain"));
    }
    try {
      if (snapshot.state() == GameInstanceState.ERROR) {
        instance.transitionTo(GameInstanceState.CLEANING);
      } else {
        instance.transitionTo(GameInstanceState.ENDING);
        instance.transitionTo(GameInstanceState.CLEANING);
      }
    } catch (InvalidInstanceTransitionException invalid) {
      diagnostics.accept(
          "Instance " + instanceId + " rejected lifecycle transition: " + invalid.getMessage());
      return CompletableFuture.failedFuture(invalid);
    }
    return instance
        .world()
        .map(
            handle ->
                worlds
                    .destroy(handle, preserveFailedWorlds.get())
                    .thenApply(
                        cleaned -> {
                          if (cleaned) {
                            instance.transitionTo(GameInstanceState.CLOSED);
                            registry.remove(instance);
                          } else {
                            instance.markError("World cleanup was not confirmed");
                          }
                          return cleaned;
                        }))
        .orElseGet(
            () -> {
              instance.transitionTo(GameInstanceState.CLOSED);
              registry.remove(instance);
              return CompletableFuture.completedFuture(true);
            });
  }

  /**
   * Marks every live instance as interrupted before a server shutdown.
   *
   * <p>World files are deliberately preserved by the platform adapter because cleanup certainty
   * cannot be established while the server is stopping.
   */
  public void markAllInterrupted() {
    for (GameInstanceSnapshot snapshot : registry.snapshots()) {
      registry
          .find(snapshot.id())
          .ifPresent(
              instance -> {
                instance.markError("Server shutdown interrupted the instance");
                diagnostics.accept("Instance " + snapshot.id() + " interrupted by server shutdown");
              });
    }
  }

  private CompletableFuture<Boolean> cleanupFailedCreation(GameInstance instance) {
    Optional<WorldInstanceHandle> world = instance.world();
    if (world.isEmpty()) {
      return CompletableFuture.completedFuture(true);
    }
    return worlds.destroy(world.get(), preserveFailedWorlds.get());
  }

  private static Throwable unwrap(Throwable failure) {
    return failure instanceof CompletionException && failure.getCause() != null
        ? failure.getCause()
        : failure;
  }
}
