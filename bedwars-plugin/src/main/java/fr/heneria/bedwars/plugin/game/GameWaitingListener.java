package fr.heneria.bedwars.plugin.game;

import fr.heneria.bedwars.api.game.GameState;
import fr.heneria.bedwars.core.config.PlaceholderContext;
import fr.heneria.bedwars.core.game.GameInstance;
import fr.heneria.bedwars.core.game.GameInstanceManager;
import fr.heneria.bedwars.core.game.GameLocationMapper;
import fr.heneria.bedwars.core.game.item.RuntimeItemAction;
import fr.heneria.bedwars.core.game.item.RuntimeItemActionRegistry;
import fr.heneria.bedwars.core.game.item.RuntimeItemDecision;
import fr.heneria.bedwars.core.game.lobby.GameLobbyService;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import fr.heneria.bedwars.plugin.gui.GuiService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
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
import org.bukkit.inventory.EquipmentSlot;
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
  private final GuiService gui;
  private final GamePublicInfoMenuFactory infoMenus;
  private final GameLocationMapper locations = new GameLocationMapper();
  private final RuntimeItemPdc runtimeItems;
  private final RuntimeItemActionRegistry actionRegistry = new RuntimeItemActionRegistry();
  private final Map<UUID, Long> lastRescue = new HashMap<>();
  private final Set<UUID> leaving = ConcurrentHashMap.newKeySet();

  public GameWaitingListener(
      JavaPlugin plugin,
      ConfigurationService configurations,
      GameInstanceManager games,
      GameLobbyService lobby,
      BukkitRuntimePlayerGateway players,
      BukkitGameDisplayService displays,
      GuiService gui,
      GamePublicInfoMenuFactory infoMenus) {
    this.plugin = plugin;
    this.configurations = configurations;
    this.games = games;
    this.lobby = lobby;
    this.players = players;
    this.displays = displays;
    this.gui = gui;
    this.infoMenus = infoMenus;
    this.runtimeItems = new RuntimeItemPdc(plugin);
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

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onInteract(PlayerInteractEvent event) {
    GameInstance game = waiting(event.getPlayer()).orElse(null);
    if (game == null) return;
    RuntimeItemPdc.Token token = runtimeItems.read(event.getItem()).orElse(null);
    if (token == null) {
      if (event.getClickedBlock() != null && configurations.snapshot().game().protectPlayers())
        event.setCancelled(true);
      return;
    }
    event.setCancelled(true);
    RuntimeItemDecision decision =
        actionRegistry.evaluate(
            event.getPlayer().getUniqueId(),
            token.actionKey(),
            token.gameId(),
            game.id().toString(),
            game.state(),
            event.getHand() == EquipmentSlot.HAND,
            System.currentTimeMillis(),
            configurations.snapshot().game().itemInteractionCooldownMillis());
    if (configurations.snapshot().plugin().debug()) {
      plugin
          .getLogger()
          .info(
              "[GameItem] player="
                  + event.getPlayer().getName()
                  + " item="
                  + token.actionKey()
                  + " action="
                  + event.getAction()
                  + " game="
                  + game.id().shortId()
                  + " decision="
                  + decision);
    }
    if (decision == RuntimeItemDecision.STALE_GAME) {
      event
          .getPlayer()
          .getInventory()
          .setItem(event.getPlayer().getInventory().getHeldItemSlot(), null);
      return;
    }
    if (decision != RuntimeItemDecision.EXECUTE) return;
    RuntimeItemAction action = RuntimeItemAction.find(token.actionKey()).orElseThrow();
    if (action == RuntimeItemAction.WAITING_LEAVE) {
      if (!leaving.add(event.getPlayer().getUniqueId())) return;
      lobby
          .leave(event.getPlayer().getUniqueId())
          .whenComplete(
              (result, failure) -> {
                leaving.remove(event.getPlayer().getUniqueId());
                main(
                    () ->
                        event
                            .getPlayer()
                            .sendMessage(
                                message(
                                    failure == null && result != null && result.successful()
                                        ? "game.leave.success"
                                        : "game.stop.failed",
                                    Map.of(
                                        "code",
                                        failure == null && result != null
                                            ? result.code().name()
                                            : "INTERNAL_ERROR"))));
              });
      return;
    }
    if (action == RuntimeItemAction.WAITING_INFO) {
      gui.open(event.getPlayer(), infoMenus.create(event.getPlayer().getUniqueId()));
      return;
    }
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
    actionRegistry.forget(playerId);
    leaving.remove(playerId);
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
