package fr.heneria.zombie.plugin.lobby;

import fr.heneria.zombie.core.config.ZombieSettings.LocationOptions;
import fr.heneria.zombie.core.session.PlayerSessionService;
import fr.heneria.zombie.plugin.config.ConfigurationManager;
import fr.heneria.zombie.plugin.display.ContextScoreboardService;
import fr.heneria.zombie.plugin.player.PaperPlayerStateService;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

/** Applies the safe default lobby context to managed players. */
public final class LobbyService {

  private final ConfigurationManager configurations;
  private final PlayerSessionService sessions;
  private final PaperPlayerStateService playerStates;
  private final ContextScoreboardService scoreboards;

  /**
   * Creates the lobby service.
   *
   * @param configurations active settings
   * @param sessions session source of truth
   * @param playerStates state adapter
   * @param scoreboards scoreboard adapter
   */
  public LobbyService(
      ConfigurationManager configurations,
      PlayerSessionService sessions,
      PaperPlayerStateService playerStates,
      ContextScoreboardService scoreboards) {
    this.configurations = Objects.requireNonNull(configurations, "configurations");
    this.sessions = Objects.requireNonNull(sessions, "sessions");
    this.playerStates = Objects.requireNonNull(playerStates, "playerStates");
    this.scoreboards = Objects.requireNonNull(scoreboards, "scoreboards");
  }

  /**
   * Loads or creates the configured lobby world.
   *
   * @throws IllegalStateException when Paper cannot create it
   */
  public void initialize() {
    String worldName = configurations.current().settings().lobby().world();
    if (Bukkit.getWorld(worldName) == null
        && Bukkit.createWorld(new WorldCreator(worldName)) == null) {
      throw new IllegalStateException("Could not load lobby world " + worldName);
    }
  }

  /**
   * Starts managing and sends a player to the lobby.
   *
   * @param player player
   */
  public void admit(Player player) {
    playerStates.beginManagement(player);
    sessions.sendToLobby(player.getUniqueId());
    playerStates.applyLobby(player, spawn());
    scoreboards.applyLobby(player);
  }

  /**
   * Leaves an instance and restores the captured lobby state.
   *
   * @param player player
   * @return previous instance
   */
  public Optional<UUID> sendToLobby(Player player) {
    Optional<UUID> previous = sessions.sendToLobby(player.getUniqueId());
    playerStates.restoreLobby(player, spawn());
    scoreboards.applyLobby(player);
    return previous;
  }

  /**
   * Returns the configured lobby spawn.
   *
   * @return live location
   */
  public Location spawn() {
    LocationOptions configured = configurations.current().settings().lobby().spawn();
    World world =
        Optional.ofNullable(Bukkit.getWorld(configured.world()))
            .orElseGet(
                () ->
                    Optional.ofNullable(
                            Bukkit.getWorld(
                                configurations.current().settings().server().fallbackWorld()))
                        .orElseThrow(
                            () -> new IllegalStateException("No safe lobby world loaded")));
    return new Location(
        world,
        configured.x(),
        configured.y(),
        configured.z(),
        configured.yaw(),
        configured.pitch());
  }
}
