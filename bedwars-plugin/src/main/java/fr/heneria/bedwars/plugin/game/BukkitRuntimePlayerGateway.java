package fr.heneria.bedwars.plugin.game;

import fr.heneria.bedwars.core.config.WorldManagerSettings;
import fr.heneria.bedwars.core.game.RuntimeLocation;
import fr.heneria.bedwars.core.game.RuntimePlayerGateway;
import fr.heneria.bedwars.core.game.RuntimeWorldHandle;
import fr.heneria.bedwars.core.game.WaitingPlayerContext;
import fr.heneria.bedwars.core.game.item.RuntimeItemAction;
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
  private final RuntimeItemPdc runtimeItems;

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
    this.runtimeItems = new RuntimeItemPdc(plugin);
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
  public CompletionStage<Boolean> finish(UUID playerId) {
    return main(
        () -> {
          Player player = Bukkit.getPlayer(playerId);
          return player != null && snapshots.restoreToLobby(player);
        });
  }

  @Override
  public void disconnect(UUID playerId) {
    Runnable action =
        () -> {
          Player player = Bukkit.getPlayer(playerId);
          if (player == null) return;
          if (!snapshots.restore(player)) clearRuntimeItems(player);
        };
    if (Bukkit.isPrimaryThread()) action.run();
    else plugin.getServer().getScheduler().runTask(plugin, action);
  }

  /** Removes only PDC-authenticated runtime objects left by an interrupted older session. */
  public void sanitizeJoin(Player player, boolean belongsToActiveGame) {
    if (belongsToActiveGame) return;
    clearRuntimeItems(player);
    player.updateInventory();
  }

  @Override
  public void beginPlaying(UUID playerId) {
    Runnable action =
        () -> {
          Player player = Bukkit.getPlayer(playerId);
          if (player == null) return;
          preparePlayingPlayer(player);
        };
    if (Bukkit.isPrimaryThread()) action.run();
    else plugin.getServer().getScheduler().runTask(plugin, action);
  }

  /** Moves one player from the waiting point to the spawn of the assigned runtime team. */
  public boolean beginPlaying(
      UUID playerId, RuntimeWorldHandle handle, RuntimeLocation destination) {
    if (!Bukkit.isPrimaryThread())
      throw new IllegalStateException("Game start teleport must run on the server thread");
    Player player = Bukkit.getPlayer(playerId);
    World world = Bukkit.getWorld(handle.worldName());
    if (player == null || world == null) return false;
    Location target =
        new Location(
            world,
            destination.x(),
            destination.y(),
            destination.z(),
            destination.yaw(),
            destination.pitch());
    world.getChunkAt(target).load();
    preparePlayingPlayer(player);
    player.setGameMode(org.bukkit.GameMode.SURVIVAL);
    player.setFallDistance(0);
    return player.teleport(target);
  }

  /** Rebuilds the two current runtime items after a valid reload or refresh. */
  public void refreshWaitingItems(Player player, WaitingPlayerContext context) {
    prepareWaitingItems(player, context);
    player.updateInventory();
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

  /** Places a dead player in a safe non-interactive spectator state. */
  public boolean spectate(UUID playerId, RuntimeWorldHandle handle, RuntimeLocation destination) {
    if (!Bukkit.isPrimaryThread())
      throw new IllegalStateException("Spectator transition requires the server thread");
    Player player = Bukkit.getPlayer(playerId);
    World world = Bukkit.getWorld(handle.worldName());
    if (player == null || world == null) return false;
    Location target = location(world, destination);
    world.getChunkAt(target).load();
    preparePlayingPlayer(player);
    player.setGameMode(org.bukkit.GameMode.SPECTATOR);
    prepareSpectatorItems(player, handle.gameId().toString());
    player.setFallDistance(0);
    return player.teleport(target);
  }

  public void prepareSpectatorItems(Player player, String gameId) {
    player.getInventory().clear();
    ItemContext context = ItemContexts.forPlayer(player, configurations).build();
    player
        .getInventory()
        .setItem(
            0,
            runtimeItems.tag(
                items.buildOrFallback("game.spectator.teleporter", context),
                RuntimeItemAction.SPECTATOR_TELEPORTER,
                gameId));
    player
        .getInventory()
        .setItem(
            8,
            runtimeItems.tag(
                items.buildOrFallback("game.spectator.leave", context),
                RuntimeItemAction.SPECTATOR_LEAVE,
                gameId));
    player.updateInventory();
  }

  /** Completes a BedWars respawn at the assigned runtime-team spawn. */
  public boolean respawn(UUID playerId, RuntimeWorldHandle handle, RuntimeLocation destination) {
    if (!Bukkit.isPrimaryThread())
      throw new IllegalStateException("Respawn requires the server thread");
    Player player = Bukkit.getPlayer(playerId);
    World world = Bukkit.getWorld(handle.worldName());
    if (player == null || world == null) return false;
    Location target = location(world, destination);
    world.getChunkAt(target).load();
    preparePlayingPlayer(player);
    player.setGameMode(org.bukkit.GameMode.SURVIVAL);
    player.setHealth(player.getMaxHealth());
    player.setFoodLevel(20);
    player.setSaturation(20);
    player.setFireTicks(0);
    player.setFallDistance(0);
    return player.teleport(target);
  }

  private static Location location(World world, RuntimeLocation destination) {
    return new Location(
        world,
        destination.x(),
        destination.y(),
        destination.z(),
        destination.yaw(),
        destination.pitch());
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
    prepareWaitingItems(player, context);
    player.updateInventory();
  }

  private void clearRuntimeItems(Player player) {
    for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
      org.bukkit.inventory.ItemStack item = player.getInventory().getItem(slot);
      if (runtimeItems.clearIfRuntime(item)) player.getInventory().setItem(slot, null);
    }
    org.bukkit.inventory.ItemStack offHand = player.getInventory().getItemInOffHand();
    if (runtimeItems.clearIfRuntime(offHand)) player.getInventory().setItemInOffHand(null);
  }

  private static void preparePlayingPlayer(Player player) {
    player.getInventory().clear();
    player.getInventory().setArmorContents(new org.bukkit.inventory.ItemStack[4]);
    player.getInventory().setItemInOffHand(null);
    player.updateInventory();
  }

  private void prepareWaitingItems(Player player, WaitingPlayerContext context) {
    String gameId = String.valueOf(context.placeholders().getOrDefault("full_game_id", ""));
    ItemContext.Builder itemContext = ItemContexts.forPlayer(player, configurations);
    context.placeholders().forEach(itemContext::placeholder);
    localizeState(context, itemContext);
    player
        .getInventory()
        .setItem(
            context.infoSlot(),
            runtimeItems.tag(
                items.buildOrFallback("game.waiting.info", itemContext.build()),
                RuntimeItemAction.WAITING_INFO,
                gameId));
    ItemContext.Builder leaveContext = ItemContexts.forPlayer(player, configurations);
    context.placeholders().forEach(leaveContext::placeholder);
    localizeState(context, leaveContext);
    player
        .getInventory()
        .setItem(
            context.leaveSlot(),
            runtimeItems.tag(
                items.buildOrFallback("game.waiting.leave", leaveContext.build()),
                RuntimeItemAction.WAITING_LEAVE,
                gameId));
  }

  private void localizeState(WaitingPlayerContext context, ItemContext.Builder itemContext) {
    Object state = context.placeholders().get("state");
    if (state == null) return;
    String key = "game.state." + state.toString().toLowerCase(java.util.Locale.ROOT);
    itemContext.placeholder(
        "state",
        configurations
            .language()
            .message(
                key,
                configurations.snapshot().plugin().locale(),
                fr.heneria.bedwars.core.config.PlaceholderContext.EMPTY));
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
