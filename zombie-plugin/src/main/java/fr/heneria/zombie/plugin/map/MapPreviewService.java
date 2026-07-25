package fr.heneria.zombie.plugin.map;

import fr.heneria.zombie.core.instance.WorldInstanceHandle;
import fr.heneria.zombie.plugin.lobby.LobbyService;
import fr.heneria.zombie.plugin.world.PaperWorldInstanceService;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Owns isolated, non-game world copies used by administrators to inspect uploaded maps.
 *
 * <p>A preview never enters the game-instance registry. Its source template is never loaded or
 * modified directly.
 */
public final class MapPreviewService {

  private final MapTemplateCatalog templates;
  private final PaperWorldInstanceService worlds;
  private final LobbyService lobby;
  private final Executor mainThread;
  private final Map<UUID, PreviewSession> active = new ConcurrentHashMap<>();
  private final Set<UUID> transitions = ConcurrentHashMap.newKeySet();

  /**
   * Creates the preview service.
   *
   * @param templates uploaded map catalog
   * @param worlds isolated world adapter
   * @param lobby lobby return service
   * @param mainThread Paper main-thread executor
   */
  public MapPreviewService(
      MapTemplateCatalog templates,
      PaperWorldInstanceService worlds,
      LobbyService lobby,
      Executor mainThread) {
    this.templates = Objects.requireNonNull(templates, "templates");
    this.worlds = Objects.requireNonNull(worlds, "worlds");
    this.lobby = Objects.requireNonNull(lobby, "lobby");
    this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
  }

  /**
   * Opens a private copy and teleports the administrator to the vanilla world spawn.
   *
   * @param player administrator
   * @param mapId uploaded folder identifier
   * @return future opened identifier
   */
  public CompletableFuture<String> open(Player player, String mapId) {
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(mapId, "mapId");
    UUID playerId = player.getUniqueId();
    if (!transitions.add(playerId)) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("A map preview transition is already running"));
    }
    return templates
        .find(mapId)
        .thenCompose(
            definition ->
                definition.isPresent()
                    ? closeExisting(playerId, player, true)
                    : CompletableFuture.failedFuture(
                        new IllegalArgumentException("Unknown or invalid map " + mapId)))
        .thenCompose(ignored -> worlds.prepare(UUID.randomUUID(), mapId))
        .thenCompose(handle -> enterPreparedWorld(player, mapId, handle))
        .whenComplete((ignored, failure) -> transitions.remove(playerId));
  }

  /**
   * Leaves the player's current preview, returns them to the lobby and deletes the copy.
   *
   * @param player player
   * @return future indicating whether a preview existed
   */
  public CompletableFuture<Boolean> leave(Player player) {
    Objects.requireNonNull(player, "player");
    UUID playerId = player.getUniqueId();
    if (!transitions.add(playerId)) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("A map preview transition is already running"));
    }
    return closeExisting(playerId, player, true)
        .whenComplete((ignored, failure) -> transitions.remove(playerId));
  }

  /**
   * Releases an offline player's preview.
   *
   * @param playerId player identifier
   */
  public void disconnect(UUID playerId) {
    if (!transitions.add(playerId)) {
      return;
    }
    closeExisting(playerId, null, false)
        .whenComplete((ignored, failure) -> transitions.remove(playerId));
  }

  /** Clears in-memory ownership during shutdown; the world adapter safely unloads all copies. */
  public void clear() {
    active.clear();
    transitions.clear();
  }

  /**
   * Returns whether any preview is active or transitioning.
   *
   * @return preview activity
   */
  public boolean hasActivePreviews() {
    return !active.isEmpty() || !transitions.isEmpty();
  }

  /**
   * Checks whether a player owns the named preview copy.
   *
   * @param playerId player identifier
   * @param worldName runtime world name
   * @return ownership
   */
  public boolean canEnter(UUID playerId, String worldName) {
    PreviewSession session = active.get(playerId);
    return session != null && session.handle().worldName().equals(worldName);
  }

  private CompletableFuture<String> enterPreparedWorld(
      Player player, String mapId, WorldInstanceHandle handle) {
    PreviewSession session = new PreviewSession(mapId, handle);
    active.put(player.getUniqueId(), session);
    return CompletableFuture.supplyAsync(
            () -> {
              if (!player.isOnline()) {
                return false;
              }
              World world = Bukkit.getWorld(handle.worldName());
              return world != null && player.teleport(world.getSpawnLocation());
            },
            mainThread)
        .thenCompose(
            teleported -> {
              if (!teleported) {
                return CompletableFuture.failedFuture(
                    new IllegalStateException("Could not teleport to preview world " + mapId));
              }
              return CompletableFuture.completedFuture(mapId);
            })
        .exceptionallyCompose(
            failure -> {
              active.remove(player.getUniqueId(), session);
              Throwable cause =
                  failure instanceof CompletionException && failure.getCause() != null
                      ? failure.getCause()
                      : failure;
              return worlds
                  .destroyTemporary(handle)
                  .thenCompose(ignored -> CompletableFuture.failedFuture(cause));
            });
  }

  private CompletableFuture<Boolean> closeExisting(
      UUID playerId, Player player, boolean returnToLobby) {
    PreviewSession session = active.remove(playerId);
    if (session == null) {
      return CompletableFuture.completedFuture(false);
    }
    CompletableFuture<Void> returned =
        returnToLobby && player != null
            ? CompletableFuture.runAsync(() -> lobby.sendToLobby(player), mainThread)
            : CompletableFuture.completedFuture(null);
    return returned
        .handle((ignored, returnFailure) -> returnFailure)
        .thenCompose(
            returnFailure ->
                worlds
                    .destroyTemporary(session.handle())
                    .thenApply(
                        cleaned -> {
                          if (!cleaned) {
                            throw new IllegalStateException(
                                "Could not unload preview world " + session.handle().worldName());
                          }
                          if (returnFailure != null) {
                            throw new CompletionException(returnFailure);
                          }
                          return true;
                        }));
  }

  private record PreviewSession(String mapId, WorldInstanceHandle handle) {
    private PreviewSession {
      Objects.requireNonNull(mapId, "mapId");
      Objects.requireNonNull(handle, "handle");
    }
  }
}
