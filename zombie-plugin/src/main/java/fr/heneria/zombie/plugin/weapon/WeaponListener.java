package fr.heneria.zombie.plugin.weapon;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

/** Thin Paper input adapter for firing, reload and map weapon stations. */
public final class WeaponListener implements Listener {
  private final PaperWeaponService weapons;

  public WeaponListener(PaperWeaponService weapons) {
    this.weapons = weapons;
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onInteract(PlayerInteractEvent event) {
    if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND
        || (event.getAction() != Action.RIGHT_CLICK_AIR
            && event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
      return;
    }
    if (event.getClickedBlock() != null
        && weapons.interactMapObject(
            event.getPlayer(),
            event.getClickedBlock().getLocation(),
            org.bukkit.Bukkit.getCurrentTick())) {
      event.setCancelled(true);
      return;
    }
    if (weapons.current(event.getPlayer()).isPresent()
        && weapons.fire(event.getPlayer(), org.bukkit.Bukkit.getCurrentTick())) {
      event.setCancelled(true);
      event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
      event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onSwap(PlayerSwapHandItemsEvent event) {
    if (weapons.current(event.getPlayer()).isPresent()) {
      event.setCancelled(true);
      weapons.beginReload(event.getPlayer(), org.bukkit.Bukkit.getCurrentTick());
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onDrop(PlayerDropItemEvent event) {
    if (weapons.find(event.getItemDrop().getItemStack()).isPresent()) {
      event.setCancelled(true);
      weapons.beginReload(event.getPlayer(), org.bukkit.Bukkit.getCurrentTick());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onHeldSlot(PlayerItemHeldEvent event) {
    weapons.current(event.getPlayer()).ifPresent(weapon -> weapon.interruptReload());
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onInventory(InventoryClickEvent event) {
    if (weapons.find(event.getCursor()).isPresent()
        && event.getClickedInventory() != event.getWhoClicked().getInventory()) {
      event.setCancelled(true);
    }
  }
}
