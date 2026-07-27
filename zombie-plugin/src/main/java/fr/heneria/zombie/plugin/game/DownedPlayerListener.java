package fr.heneria.zombie.plugin.game;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

/** Enforces the immobile and non-interactive Paper presentation of a downed player. */
public final class DownedPlayerListener implements Listener {
  private final PaperGameRuntime games;

  public DownedPlayerListener(PaperGameRuntime games) {
    this.games = java.util.Objects.requireNonNull(games, "games");
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
  public void onMove(PlayerMoveEvent event) {
    if (!games.isDowned(event.getPlayer().getUniqueId()) || !event.hasChangedPosition()) {
      return;
    }
    var target = event.getTo().clone();
    target.setX(event.getFrom().getX());
    target.setY(event.getFrom().getY());
    target.setZ(event.getFrom().getZ());
    event.setTo(target);
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
  public void onInteract(PlayerInteractEvent event) {
    if (games.isDowned(event.getPlayer().getUniqueId())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
  public void onInteractEntity(PlayerInteractEntityEvent event) {
    if (games.isDowned(event.getPlayer().getUniqueId())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
  public void onDrop(PlayerDropItemEvent event) {
    if (games.isDowned(event.getPlayer().getUniqueId())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
  public void onSwap(PlayerSwapHandItemsEvent event) {
    if (games.isDowned(event.getPlayer().getUniqueId())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
  public void onInventory(InventoryClickEvent event) {
    if (event.getWhoClicked() instanceof Player player && games.isDowned(player.getUniqueId())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
  public void onPickup(EntityPickupItemEvent event) {
    if (event.getEntity() instanceof Player player && games.isDowned(player.getUniqueId())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
  public void onAttack(EntityDamageByEntityEvent event) {
    if (event.getDamager() instanceof Player player && games.isDowned(player.getUniqueId())) {
      event.setCancelled(true);
    }
  }
}
