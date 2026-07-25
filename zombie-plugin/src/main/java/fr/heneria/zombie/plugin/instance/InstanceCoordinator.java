package fr.heneria.zombie.plugin.instance;

import fr.heneria.zombie.core.instance.GameInstanceOptions;
import fr.heneria.zombie.core.instance.GameInstanceService;
import fr.heneria.zombie.core.instance.GameInstanceSnapshot;
import fr.heneria.zombie.core.instance.InstanceJoinResult;
import fr.heneria.zombie.core.map.MapTemplateDefinition;
import fr.heneria.zombie.core.session.ExpiredReservation;
import fr.heneria.zombie.core.session.PlayerContext;
import fr.heneria.zombie.core.session.PlayerSessionService;
import fr.heneria.zombie.core.session.PlayerSessionSnapshot;
import fr.heneria.zombie.core.session.ReconnectDecision;
import fr.heneria.zombie.core.session.SessionAssignmentResult;
import fr.heneria.zombie.plugin.display.ContextScoreboardService;
import fr.heneria.zombie.plugin.isolation.VisibilityService;
import fr.heneria.zombie.plugin.lobby.LobbyService;
import fr.heneria.zombie.plugin.map.MapTemplateCatalog;
import fr.heneria.zombie.plugin.player.PaperPlayerStateService;
import fr.heneria.zombie.plugin.world.PaperWorldInstanceService;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/** Coordinates cross-service player and instance transactions on the Paper boundary. */
public final class InstanceCoordinator {

  private final GameInstanceService instances;
  private final PlayerSessionService sessions;
  private final MapTemplateCatalog templates;
  private final PaperWorldInstanceService worlds;
  private final LobbyService lobby;
  private final PaperPlayerStateService playerStates;
  private final ContextScoreboardService scoreboards;
  private final VisibilityService visibility;
  private final Executor mainThread;

  /**
   * Creates the coordinator.
   *
   * @param instances instance application service
   * @param sessions player sessions
   * @param templates map catalog
   * @param worlds world service
   * @param lobby lobby service
   * @param playerStates player state adapter
   * @param scoreboards scoreboard adapter
   * @param visibility visibility adapter
   * @param mainThread Paper executor
   */
  public InstanceCoordinator(
      GameInstanceService instances,
      PlayerSessionService sessions,
      MapTemplateCatalog templates,
      PaperWorldInstanceService worlds,
      LobbyService lobby,
      PaperPlayerStateService playerStates,
      ContextScoreboardService scoreboards,
      VisibilityService visibility,
      Executor mainThread) {
    this.instances = Objects.requireNonNull(instances, "instances");
    this.sessions = Objects.requireNonNull(sessions, "sessions");
    this.templates = Objects.requireNonNull(templates, "templates");
    this.worlds = Objects.requireNonNull(worlds, "worlds");
    this.lobby = Objects.requireNonNull(lobby, "lobby");
    this.playerStates = Objects.requireNonNull(playerStates, "playerStates");
    this.scoreboards = Objects.requireNonNull(scoreboards, "scoreboards");
    this.visibility = Objects.requireNonNull(visibility, "visibility");
    this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
  }

  /**
   * Creates an instance from a validated template.
   *
   * @param mapId template identifier
   * @param owner optional owner
   * @return future created snapshot
   */
  public CompletableFuture<GameInstanceSnapshot> create(String mapId, Optional<UUID> owner) {
    return templates
        .find(mapId)
        .thenCompose(
            template ->
                template
                    .map(
                        value ->
                            instances.createInstance(
                                mapId,
                                new GameInstanceOptions(
                                    value.maximumPlayers(),
                                    owner,
                                    owner.isPresent()
                                        ? fr.heneria.zombie.core.instance.InstanceAccess.PRIVATE
                                        : fr.heneria.zombie.core.instance.InstanceAccess.PUBLIC)))
                    .orElseGet(
                        () ->
                            CompletableFuture.failedFuture(
                                new IllegalArgumentException("Unknown or invalid map " + mapId))));
  }

  /**
   * Joins a player to one prepared instance.
   *
   * @param player player
   * @param instanceId instance identifier
   * @return future result
   */
  public CompletableFuture<PlayerInstanceResult> join(Player player, UUID instanceId) {
    Optional<GameInstanceSnapshot> found = instances.findInstance(instanceId);
    if (found.isEmpty()) {
      return CompletableFuture.completedFuture(PlayerInstanceResult.INSTANCE_NOT_FOUND);
    }
    GameInstanceSnapshot instance = found.get();
    return templates
        .find(instance.mapId())
        .thenApplyAsync(
            template ->
                template
                    .map(value -> joinOnMain(player, instance, value))
                    .orElse(PlayerInstanceResult.TEMPLATE_INVALID),
            mainThread);
  }

  /**
   * Sends a player to the lobby and releases instance membership.
   *
   * @param player player
   * @return whether an instance membership existed
   */
  public boolean leave(Player player) {
    Optional<UUID> previous =
        sessions.findSession(player.getUniqueId()).flatMap(PlayerSessionSnapshot::instanceId);
    previous.ifPresent(instanceId -> instances.leaveInstance(player.getUniqueId(), instanceId));
    lobby.sendToLobby(player);
    visibility.refreshAll();
    return previous.isPresent();
  }

  /**
   * Evicts all members, closes an instance and removes its scoreboard.
   *
   * @param instanceId instance identifier
   * @return future cleanup result
   */
  public CompletableFuture<Boolean> stop(UUID instanceId) {
    Optional<GameInstanceSnapshot> found = instances.findInstance(instanceId);
    if (found.isEmpty()) {
      return CompletableFuture.completedFuture(false);
    }
    for (UUID playerId : found.get().players()) {
      Player online = Bukkit.getPlayer(playerId);
      instances.leaveInstance(playerId, instanceId);
      if (online != null) {
        lobby.sendToLobby(online);
      } else {
        sessions.sendToLobby(playerId);
      }
    }
    visibility.refreshAll();
    return instances
        .closeInstance(instanceId)
        .thenApply(
            cleaned -> {
              if (cleaned) {
                scoreboards.removeInstance(instanceId);
              }
              return cleaned;
            });
  }

  /**
   * Handles a login or reconnection.
   *
   * @param player player
   */
  public CompletableFuture<PlayerContext> connect(Player player) {
    ReconnectDecision decision = sessions.connect(player.getUniqueId());
    if (decision == ReconnectDecision.RETURN_TO_INSTANCE) {
      Optional<UUID> instanceId =
          sessions.findSession(player.getUniqueId()).flatMap(PlayerSessionSnapshot::instanceId);
      if (instanceId.isPresent()) {
        return join(player, instanceId.get())
            .handleAsync(
                (result, failure) -> {
                  if (failure == null && result == PlayerInstanceResult.SUCCESS) {
                    return PlayerContext.INSTANCE;
                  }
                  instances.leaveInstance(player.getUniqueId(), instanceId.get());
                  lobby.admit(player);
                  visibility.refreshAll();
                  return PlayerContext.LOBBY;
                },
                mainThread);
      }
    }
    lobby.admit(player);
    visibility.refreshAll();
    return CompletableFuture.completedFuture(PlayerContext.LOBBY);
  }

  /**
   * Retains or releases a disconnecting membership according to policy.
   *
   * @param playerId player identifier
   * @param reserveSlot configured reservation policy
   */
  public void disconnect(UUID playerId, boolean reserveSlot) {
    Optional<UUID> instanceId =
        sessions.findSession(playerId).flatMap(PlayerSessionSnapshot::instanceId);
    PlayerSessionSnapshot result = sessions.disconnect(playerId);
    if (instanceId.isPresent() && (!reserveSlot || result.context() == PlayerContext.LOBBY)) {
      instances.leaveInstance(playerId, instanceId.get());
    }
    visibility.refreshAll();
  }

  /** Releases expired reconnect reservations. */
  public void expireReconnectReservations() {
    for (ExpiredReservation reservation : sessions.expireReconnectReservations()) {
      instances.leaveInstance(reservation.playerId(), reservation.instanceId());
    }
  }

  /**
   * Returns active instance snapshots.
   *
   * @return snapshots
   */
  public Collection<GameInstanceSnapshot> activeInstances() {
    return instances.getActiveInstances();
  }

  /**
   * Safely moves online players to the lobby and preserves instance folders on shutdown.
   *
   * @return worlds that Paper could not unload
   */
  public java.util.List<String> shutdown() {
    instances.markAllInterrupted();
    for (Player player : Bukkit.getOnlinePlayers()) {
      leave(player);
      visibility.restore(player);
    }
    java.util.List<String> failures = worlds.unloadAllPreservingFiles();
    sessions.clear();
    scoreboards.clear();
    playerStates.clear();
    return failures;
  }

  private PlayerInstanceResult joinOnMain(
      Player player, GameInstanceSnapshot instance, MapTemplateDefinition template) {
    if (!player.isOnline()) {
      return PlayerInstanceResult.PLAYER_OFFLINE;
    }
    SessionAssignmentResult assignment =
        sessions.assignInstance(player.getUniqueId(), instance.id());
    if (assignment == SessionAssignmentResult.OTHER_INSTANCE) {
      return PlayerInstanceResult.ALREADY_IN_OTHER_INSTANCE;
    }
    InstanceJoinResult joined = instances.joinInstance(player.getUniqueId(), instance.id());
    if (joined == InstanceJoinResult.FULL) {
      if (assignment == SessionAssignmentResult.ASSIGNED) {
        sessions.sendToLobby(player.getUniqueId());
      }
      return PlayerInstanceResult.INSTANCE_FULL;
    }
    if (joined == InstanceJoinResult.ACCESS_DENIED) {
      if (assignment == SessionAssignmentResult.ASSIGNED) {
        sessions.sendToLobby(player.getUniqueId());
      }
      return PlayerInstanceResult.ACCESS_DENIED;
    }
    if (joined != InstanceJoinResult.JOINED && joined != InstanceJoinResult.ALREADY_JOINED) {
      if (assignment == SessionAssignmentResult.ASSIGNED) {
        sessions.sendToLobby(player.getUniqueId());
      }
      return PlayerInstanceResult.INSTANCE_UNAVAILABLE;
    }
    World world = instance.worldName().map(Bukkit::getWorld).orElse(null);
    if (world == null) {
      rollbackJoin(player.getUniqueId(), instance.id());
      return PlayerInstanceResult.INSTANCE_UNAVAILABLE;
    }
    playerStates.captureLobby(player);
    MapTemplateDefinition.MapSpawn spawn = template.spawn();
    boolean teleported =
        playerStates.applyInstance(
            player,
            new Location(world, spawn.x(), spawn.y(), spawn.z(), spawn.yaw(), spawn.pitch()));
    if (!teleported) {
      rollbackJoin(player.getUniqueId(), instance.id());
      lobby.sendToLobby(player);
      return PlayerInstanceResult.TELEPORT_FAILED;
    }
    scoreboards.applyInstance(player, instance.id());
    visibility.refreshAll();
    return PlayerInstanceResult.SUCCESS;
  }

  private void rollbackJoin(UUID playerId, UUID instanceId) {
    instances.leaveInstance(playerId, instanceId);
    sessions.sendToLobby(playerId);
  }
}
