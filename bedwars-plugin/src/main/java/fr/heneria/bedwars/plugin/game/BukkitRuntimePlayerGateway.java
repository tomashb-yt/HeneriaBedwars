package fr.heneria.bedwars.plugin.game;

import fr.heneria.bedwars.core.config.WorldManagerSettings;
import fr.heneria.bedwars.core.game.RuntimeLocation;
import fr.heneria.bedwars.core.game.RuntimePlayerGateway;
import fr.heneria.bedwars.core.game.RuntimeWorldHandle;
import fr.heneria.bedwars.core.game.WaitingPlayerContext;
import fr.heneria.bedwars.core.item.ItemContext;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import fr.heneria.bedwars.plugin.item.ItemContexts;
import fr.heneria.bedwars.plugin.item.ItemService;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Local-server player movement adapter; a proxy implementation can replace this port later. */
public final class BukkitRuntimePlayerGateway implements RuntimePlayerGateway {
  private final JavaPlugin plugin;
  private final WorldManagerSettings settings;
  private final ConfigurationService configurations;
  private final ItemService items;
  private final BukkitPlayerSnapshotService snapshots;

  public BukkitRuntimePlayerGateway(
      JavaPlugin plugin,
      WorldManagerSettings settings,
      ConfigurationService configurations,
      ItemService items,
      BukkitPlayerSnapshotService snapshots) {
    this.plugin = plugin;
    this.settings = settings;
    this.configurations = configurations;
    this.items = items;
    this.snapshots = snapshots;
  }

  @Override
  public CompletionStage<Boolean> enter(
      UUID playerId,
      RuntimeWorldHandle handle,
      RuntimeLocation destination,
      WaitingPlayerContext context) {
    return main(
        () -> {
          Player player = Bukkit.getPlayer(playerId);
          World world = Bukkit.getWorld(handle.worldName());
          if (player == null || world == null) return false;
          if (!snapshots.capture(player)) return false;
          Location target =
              new Location(
                  world,
                  destination.x(),
                  destination.y(),
                  destination.z(),
                  destination.yaw(),
                  destination.pitch());
          world.getChunkAt(target).load();
          if (!player.teleport(target)) {
            snapshots.restore(player);
            return false;
          }
          prepareWaitingPlayer(player, context);
          return true;
        });
  }

  @Override
  public CompletionStage<Boolean> leave(UUID playerId) {
    return main(
        () -> {
          Player player = Bukkit.getPlayer(playerId);
          return player != null && snapshots.restore(player);
        });
  }

  @Override
  public void disconnect(UUID playerId) {
    snapshots.discard(playerId);
  }

  @Override
  public void beginPlaying(UUID playerId) {
    Runnable action =
        () -> {
          Player player = Bukkit.getPlayer(playerId);
          if (player == null) return;
          player.getInventory().clear();
          player.getInventory().setArmorContents(new org.bukkit.inventory.ItemStack[4]);
          player.getInventory().setItemInOffHand(null);
          player.updateInventory();
        };
    if (Bukkit.isPrimaryThread()) action.run();
    else plugin.getServer().getScheduler().runTask(plugin, action);
  }

  public boolean rescue(UUID playerId, RuntimeWorldHandle handle, RuntimeLocation destination) {
    if (!Bukkit.isPrimaryThread())
      throw new IllegalStateException("Void rescue requires the Bukkit server thread");
    Player player = Bukkit.getPlayer(playerId);
    World world = Bukkit.getWorld(handle.worldName());
    if (player == null || world == null) return false;
    player.setFallDistance(0);
    return player.teleport(
        new Location(
            world,
            destination.x(),
            destination.y(),
            destination.z(),
            destination.yaw(),
            destination.pitch()));
  }

  private void prepareWaitingPlayer(Player player, WaitingPlayerContext context) {
    player.getInventory().clear();
    player.getInventory().setArmorContents(new org.bukkit.inventory.ItemStack[4]);
    player.getInventory().setItemInOffHand(null);
    for (org.bukkit.potion.PotionEffect effect : player.getActivePotionEffects())
      player.removePotionEffect(effect.getType());
    player.setGameMode(org.bukkit.GameMode.valueOf(context.gameMode()));
    player.setHealth(player.getMaxHealth());
    player.setFoodLevel(20);
    player.setSaturation(20);
    player.setFireTicks(0);
    player.setFallDistance(0);
    player.setAllowFlight(false);
    player.setFlying(false);
    ItemContext.Builder itemContext = ItemContexts.forPlayer(player, configurations);
    context.placeholders().forEach(itemContext::placeholder);
    player
        .getInventory()
        .setItem(
            context.infoSlot(), items.buildOrFallback("game.waiting.info", itemContext.build()));
    ItemContext.Builder leaveContext = ItemContexts.forPlayer(player, configurations);
    context.placeholders().forEach(leaveContext::placeholder);
    player
        .getInventory()
        .setItem(
            context.leaveSlot(), items.buildOrFallback("game.waiting.leave", leaveContext.build()));
    player.updateInventory();
  }

  private CompletionStage<Boolean> main(java.util.concurrent.Callable<Boolean> action) {
    CompletableFuture<Boolean> result = new CompletableFuture<>();
    Runnable task =
        () -> {
          try {
            result.complete(action.call());
          } catch (Exception exception) {
            result.completeExceptionally(exception);
          }
        };
    if (Bukkit.isPrimaryThread()) task.run();
    else plugin.getServer().getScheduler().runTask(plugin, task);
    return result;
  }
}
