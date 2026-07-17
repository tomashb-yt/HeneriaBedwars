package fr.heneria.bedwars.plugin.game;

import fr.heneria.bedwars.api.game.GameState;
import fr.heneria.bedwars.core.config.PlaceholderContext;
import fr.heneria.bedwars.core.game.GameInstance;
import fr.heneria.bedwars.core.game.GameInstanceManager;
import fr.heneria.bedwars.core.game.GameLocationMapper;
import fr.heneria.bedwars.core.game.lobby.GameLobbyService;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** Bukkit protections and stable waiting-item actions scoped only to WAITING/STARTING members. */
public final class GameWaitingListener implements Listener {
  private static final long RESCUE_COOLDOWN_MILLIS = 1_000;
  private final JavaPlugin plugin;
  private final ConfigurationService configurations;
  private final GameInstanceManager games;
  private final GameLobbyService lobby;
  private final BukkitRuntimePlayerGateway players;
  private final BukkitGameDisplayService displays;
  private final GameLocationMapper locations = new GameLocationMapper();
  private final NamespacedKey actionKey;
  private final Map<UUID, Long> lastRescue = new HashMap<>();

  public GameWaitingListener(
      JavaPlugin plugin,
      ConfigurationService configurations,
      GameInstanceManager games,
      GameLobbyService lobby,
      BukkitRuntimePlayerGateway players,
      BukkitGameDisplayService displays) {
    this.plugin = plugin;
    this.configurations = configurations;
    this.games = games;
    this.lobby = lobby;
    this.players = players;
    this.displays = displays;
    this.actionKey = new NamespacedKey(plugin, "action");
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onDamage(EntityDamageEvent event) {
    if (event.getEntity() instanceof Player player
        && waiting(player).isPresent()
        && configurations.snapshot().game().protectPlayers()) event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onHunger(FoodLevelChangeEvent event) {
    if (event.getEntity() instanceof Player player
        && waiting(player).isPresent()
        && configurations.snapshot().game().disableHunger()) {
      event.setCancelled(true);
      player.setFoodLevel(20);
      player.setSaturation(20);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onBreak(BlockBreakEvent event) {
    if (protectedPlayer(event.getPlayer())) event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onPlace(BlockPlaceEvent event) {
    if (protectedPlayer(event.getPlayer())) event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onDrop(PlayerDropItemEvent event) {
    if (waiting(event.getPlayer()).isPresent()
        && configurations.snapshot().game().disableItemDrop()) event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onPickup(EntityPickupItemEvent event) {
    if (event.getEntity() instanceof Player player
        && waiting(player).isPresent()
        && configurations.snapshot().game().disableItemPickup()) event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onInventory(InventoryClickEvent event) {
    if (event.getWhoClicked() instanceof Player player && waiting(player).isPresent())
      event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onDrag(InventoryDragEvent event) {
    if (event.getWhoClicked() instanceof Player player && waiting(player).isPresent())
      event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onCraft(CraftItemEvent event) {
    if (event.getWhoClicked() instanceof Player player && waiting(player).isPresent())
      event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onSwap(PlayerSwapHandItemsEvent event) {
    if (waiting(event.getPlayer()).isPresent()) event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onCombust(EntityCombustEvent event) {
    if (event.getEntity() instanceof Player player && waiting(player).isPresent()) {
      event.setCancelled(true);
      player.setFireTicks(0);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onInteract(PlayerInteractEvent event) {
    GameInstance game = waiting(event.getPlayer()).orElse(null);
    if (game == null) return;
    String action =
        event.getItem() == null || !event.getItem().hasItemMeta()
            ? null
            : event
                .getItem()
                .getItemMeta()
                .getPersistentDataContainer()
                .get(actionKey, PersistentDataType.STRING);
    if ("leave-game".equals(action)) {
      event.setCancelled(true);
      lobby
          .leave(event.getPlayer().getUniqueId())
          .whenComplete(
              (result, failure) ->
                  main(
                      () ->
                          event
                              .getPlayer()
                              .sendMessage(
                                  message(
                                      failure == null && result.successful()
                                          ? "game.leave.success"
                                          : "game.stop.failed",
                                      Map.of(
                                          "code",
                                          failure == null
                                              ? result.code().name()
                                              : "INTERNAL_ERROR")))));
      return;
    }
    if ("game-info".equals(action)) {
      event.setCancelled(true);
      displays.showInfo(event.getPlayer().getUniqueId());
      return;
    }
    if (event.getClickedBlock() != null && configurations.snapshot().game().protectPlayers())
      event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMove(PlayerMoveEvent event) {
    if (event.getTo() == null
        || event.getTo().getY() >= configurations.snapshot().game().voidRescueY()) return;
    GameInstance game = waiting(event.getPlayer()).orElse(null);
    if (game == null) return;
    long now = System.currentTimeMillis();
    if (now - lastRescue.getOrDefault(event.getPlayer().getUniqueId(), 0L) < RESCUE_COOLDOWN_MILLIS)
      return;
    lastRescue.put(event.getPlayer().getUniqueId(), now);
    game.world()
        .ifPresent(
            world ->
                players.rescue(event.getPlayer().getUniqueId(), world, locations.waiting(game)));
  }

  public void forget(UUID playerId) {
    lastRescue.remove(playerId);
  }

  private boolean protectedPlayer(Player player) {
    return waiting(player).isPresent() && configurations.snapshot().game().protectPlayers();
  }

  private Optional<GameInstance> waiting(Player player) {
    return games
        .byPlayer(player.getUniqueId())
        .filter(game -> game.state() == GameState.WAITING || game.state() == GameState.STARTING);
  }

  private String message(String key, Map<String, ?> values) {
    PlaceholderContext.Builder context = PlaceholderContext.builder();
    values.forEach(context::put);
    return configurations
        .language()
        .message(key, configurations.snapshot().plugin().locale(), context.build());
  }

  private void main(Runnable action) {
    if (Bukkit.isPrimaryThread()) action.run();
    else plugin.getServer().getScheduler().runTask(plugin, action);
  }
}
