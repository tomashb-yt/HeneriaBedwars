package fr.heneria.bedwars.plugin.game;

import fr.heneria.bedwars.core.config.LobbySettings;
import fr.heneria.bedwars.core.config.WorldManagerSettings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

/** Main-thread-only in-memory capture and restoration service for pre-game player state. */
public final class BukkitPlayerSnapshotService {
  private final WorldManagerSettings worlds;
  private final Supplier<LobbySettings> lobbySettings;
  private final Map<UUID, PlayerPreGameSnapshot> snapshots = new LinkedHashMap<>();

  public BukkitPlayerSnapshotService(
      WorldManagerSettings worlds, Supplier<LobbySettings> lobbySettings) {
    this.worlds = worlds;
    this.lobbySettings = lobbySettings;
  }

  /** Captures once and refuses a second mutable snapshot for the same player. */
  public synchronized boolean capture(Player player) {
    requireMainThread();
    if (snapshots.containsKey(player.getUniqueId())) return false;
    Location location = player.getLocation();
    snapshots.put(
        player.getUniqueId(),
        new PlayerPreGameSnapshot(
            location.getWorld().getName(),
            location.getX(),
            location.getY(),
            location.getZ(),
            location.getYaw(),
            location.getPitch(),
            player.getGameMode(),
            player.getInventory().getStorageContents(),
            player.getInventory().getArmorContents(),
            player.getInventory().getItemInOffHand(),
            player.getLevel(),
            player.getExp(),
            player.getHealth(),
            player.getFoodLevel(),
            player.getSaturation(),
            player.getFireTicks(),
            player.getFallDistance(),
            player.getAllowFlight(),
            player.isFlying(),
            player.getWalkSpeed(),
            player.getFlySpeed(),
            List.copyOf(player.getActivePotionEffects()),
            player.getScoreboard()));
    return true;
  }

  /** Restores and removes a snapshot only after the destination teleport succeeds. */
  public synchronized boolean restore(Player player) {
    return restore(player, false);
  }

  /** Restores gameplay state but always sends match participants back to the main lobby. */
  public synchronized boolean restoreToLobby(Player player) {
    return restore(player, true);
  }

  private boolean restore(Player player, boolean mainLobby) {
    requireMainThread();
    PlayerPreGameSnapshot snapshot = snapshots.get(player.getUniqueId());
    if (snapshot == null) return false;
    Location location = mainLobby ? lobbyLocation() : previousLocation(snapshot);
    if (location == null) return false;
    if (!player.teleport(location)) return false;
    player.getInventory().setStorageContents(snapshot.storage());
    player.getInventory().setArmorContents(snapshot.armor());
    player.getInventory().setItemInOffHand(snapshot.offHand());
    player.setGameMode(snapshot.gameMode());
    player.setLevel(snapshot.level());
    player.setExp(snapshot.experience());
    player.setFoodLevel(snapshot.food());
    player.setSaturation(snapshot.saturation());
    player.setFireTicks(snapshot.fireTicks());
    player.setFallDistance(snapshot.fallDistance());
    player.setAllowFlight(snapshot.allowFlight());
    player.setFlying(snapshot.allowFlight() && snapshot.flying());
    player.setWalkSpeed(snapshot.walkSpeed());
    player.setFlySpeed(snapshot.flySpeed());
    for (PotionEffect effect : player.getActivePotionEffects())
      player.removePotionEffect(effect.getType());
    player.addPotionEffects(snapshot.effects());
    player.setScoreboard(snapshot.scoreboard());
    player.setHealth(Math.min(snapshot.health(), player.getMaxHealth()));
    player.updateInventory();
    snapshots.remove(player.getUniqueId());
    return true;
  }

  private Location lobbyLocation() {
    LobbySettings lobby = lobbySettings.get();
    if (lobby.configured()) {
      World world = Bukkit.getWorld(lobby.world());
      if (world != null)
        return new Location(world, lobby.x(), lobby.y(), lobby.z(), lobby.yaw(), lobby.pitch());
    }
    World fallback = Bukkit.getWorld(worlds.fallbackWorld());
    return fallback == null ? null : fallback.getSpawnLocation();
  }

  private Location previousLocation(PlayerPreGameSnapshot snapshot) {
    World destination = Bukkit.getWorld(snapshot.worldName());
    if (destination == null) destination = Bukkit.getWorld(worlds.fallbackWorld());
    if (destination == null) return null;
    return destination.getName().equals(snapshot.worldName())
        ? new Location(
            destination, snapshot.x(), snapshot.y(), snapshot.z(), snapshot.yaw(), snapshot.pitch())
        : destination.getSpawnLocation();
  }

  public synchronized Optional<PlayerPreGameSnapshot> find(UUID playerId) {
    return Optional.ofNullable(snapshots.get(playerId));
  }

  public synchronized void discard(UUID playerId) {
    snapshots.remove(playerId);
  }

  public synchronized int size() {
    return snapshots.size();
  }

  private static void requireMainThread() {
    if (!Bukkit.isPrimaryThread())
      throw new IllegalStateException("Player snapshots require the Bukkit server thread");
  }
}
