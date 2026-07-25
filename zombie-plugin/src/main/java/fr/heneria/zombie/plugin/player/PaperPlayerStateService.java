package fr.heneria.zombie.plugin.player;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Captures and restores original and lobby states without sharing mutable inventories. */
public final class PaperPlayerStateService {

  private final Map<UUID, PlayerStateSnapshot> originalStates = new ConcurrentHashMap<>();
  private final Map<UUID, PlayerStateSnapshot> lobbyStates = new ConcurrentHashMap<>();

  /**
   * Captures the state that must be restored when the plugin releases the player.
   *
   * @param player player
   */
  public void beginManagement(Player player) {
    originalStates.putIfAbsent(player.getUniqueId(), PlayerStateSnapshot.capture(player));
  }

  /**
   * Captures the lobby state immediately before instance entry.
   *
   * @param player player
   */
  public void captureLobby(Player player) {
    lobbyStates.putIfAbsent(player.getUniqueId(), PlayerStateSnapshot.capture(player));
  }

  /**
   * Applies the standardized, empty lobby state and teleports the player.
   *
   * @param player player
   * @param spawn lobby spawn
   */
  public void applyLobby(Player player, Location spawn) {
    applyCleanState(player);
    player.teleport(spawn);
  }

  /**
   * Applies a clean instance state and teleports the player.
   *
   * @param player player
   * @param spawn instance spawn
   * @return teleport result
   */
  public boolean applyInstance(Player player, Location spawn) {
    applyCleanState(player);
    return player.teleport(spawn);
  }

  /**
   * Restores the last lobby state, falling back to the standardized lobby.
   *
   * @param player player
   * @param fallbackSpawn lobby spawn
   */
  public void restoreLobby(Player player, Location fallbackSpawn) {
    Optional.ofNullable(lobbyStates.remove(player.getUniqueId()))
        .ifPresentOrElse(
            snapshot -> {
              snapshot.restore(player);
              player.teleport(fallbackSpawn);
            },
            () -> applyLobby(player, fallbackSpawn));
  }

  /**
   * Releases a player and restores their pre-plugin state.
   *
   * @param player player
   */
  public void endManagement(Player player) {
    lobbyStates.remove(player.getUniqueId());
    Optional.ofNullable(originalStates.remove(player.getUniqueId()))
        .ifPresent(snapshot -> snapshot.restore(player));
  }

  /** Clears offline references after worlds have been safely unloaded. */
  public void clear() {
    originalStates.clear();
    lobbyStates.clear();
  }

  private static void applyCleanState(Player player) {
    player.getInventory().clear();
    player.getInventory().setArmorContents(null);
    player.getInventory().setItemInOffHand(null);
    player.setExp(0.0F);
    player.setLevel(0);
    player.setTotalExperience(0);
    player.setFoodLevel(20);
    player.setSaturation(5.0F);
    player.setExhaustion(0.0F);
    player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
    player.setGameMode(GameMode.ADVENTURE);
    player.setWalkSpeed(0.2F);
    player.setFlySpeed(0.1F);
    player.setFlying(false);
    player.setAllowFlight(false);
    player.setFireTicks(0);
    player.setFallDistance(0.0F);
    player.setHealth(
        Objects.requireNonNull(
                player.getAttribute(PaperAttributeResolver.maxHealth()), "max health")
            .getValue());
  }
}

