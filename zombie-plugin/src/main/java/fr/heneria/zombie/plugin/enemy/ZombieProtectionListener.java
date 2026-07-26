package fr.heneria.zombie.plugin.enemy;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.EntityTransformEvent;

/** Prevents vanilla mechanics from breaking engine ownership or instance isolation. */
public final class ZombieProtectionListener implements Listener {
  private final PaperZombieEngine engine;

  public ZombieProtectionListener(PaperZombieEngine engine) {
    this.engine = engine;
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onCombust(EntityCombustEvent event) {
    engine
        .find(event.getEntity().getUniqueId())
        .filter(zombie -> !zombie.definition().environment().burnInDaylight())
        .ifPresent(ignored -> event.setCancelled(true));
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onTarget(EntityTargetLivingEntityEvent event) {
    if (engine.find(event.getEntity().getUniqueId()).isEmpty()) {
      return;
    }
    if (!(event.getTarget() instanceof Player player)
        || !engine.isValidTarget(event.getEntity().getUniqueId(), player.getUniqueId())) {
      event.setCancelled(true);
      event.setTarget(null);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onPortal(EntityPortalEvent event) {
    if (engine.find(event.getEntity().getUniqueId()).isPresent()) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onPickup(EntityPickupItemEvent event) {
    if (engine.find(event.getEntity().getUniqueId()).isPresent()) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onTransform(EntityTransformEvent event) {
    if (engine.find(event.getEntity().getUniqueId()).isPresent()) {
      event.setCancelled(true);
    }
  }
}
