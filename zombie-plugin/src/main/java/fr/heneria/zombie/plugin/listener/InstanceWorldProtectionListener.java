package fr.heneria.zombie.plugin.listener;

import fr.heneria.zombie.core.session.PlayerSessionService;
import fr.heneria.zombie.plugin.config.ConfigurationManager;
import fr.heneria.zombie.plugin.world.PaperWorldInstanceService;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/** Applies fundamental configurable protections only inside owned runtime worlds. */
public final class InstanceWorldProtectionListener implements Listener {

  private final PaperWorldInstanceService worlds;
  private final PlayerSessionService sessions;
  private final ConfigurationManager configurations;

  /**
   * Creates the protection listener.
   *
   * @param worlds runtime world registry
   * @param sessions player sessions
   * @param configurations active settings
   */
  public InstanceWorldProtectionListener(
      PaperWorldInstanceService worlds,
      PlayerSessionService sessions,
      ConfigurationManager configurations) {
    this.worlds = Objects.requireNonNull(worlds, "worlds");
    this.sessions = Objects.requireNonNull(sessions, "sessions");
    this.configurations = Objects.requireNonNull(configurations, "configurations");
  }

  @EventHandler(ignoreCancelled = true)
  public void onBreak(BlockBreakEvent event) {
    if (protectedPlayer(event.getPlayer())
        && !configurations.current().settings().worldRules().allowBlockBreaking()) {
      event.setCancelled(true);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlace(BlockPlaceEvent event) {
    if (protectedPlayer(event.getPlayer())
        && !configurations.current().settings().worldRules().allowBlockPlacing()) {
      event.setCancelled(true);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onDrop(PlayerDropItemEvent event) {
    if (protectedPlayer(event.getPlayer())
        && !configurations.current().settings().worldRules().allowItemDropping()) {
      event.setCancelled(true);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onPickup(EntityPickupItemEvent event) {
    if (event.getEntity() instanceof Player player
        && protectedPlayer(player)
        && !configurations.current().settings().worldRules().allowItemPickup()) {
      event.setCancelled(true);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onPvp(EntityDamageByEntityEvent event) {
    if (event.getEntity() instanceof Player victim
        && event.getDamager() instanceof Player
        && protectedPlayer(victim)
        && !configurations.current().settings().worldRules().allowPvp()) {
      event.setCancelled(true);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onNaturalSpawn(CreatureSpawnEvent event) {
    if (worlds.isInstanceWorld(event.getLocation().getWorld().getName())
        && event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL
        && !configurations.current().settings().worldRules().allowNaturalMobSpawning()) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onWorldEntry(PlayerTeleportEvent event) {
    if (!configurations.current().settings().instances().preventEntryWithoutSession()) {
      return;
    }
    Location destination = event.getTo();
    if (destination == null) {
      return;
    }
    worlds
        .ownerOf(destination.getWorld().getName())
        .ifPresent(
            owner -> {
              boolean authorized =
                  sessions
                      .findSession(event.getPlayer().getUniqueId())
                      .flatMap(session -> session.instanceId())
                      .filter(owner::equals)
                      .isPresent();
              if (!authorized && !event.getPlayer().hasPermission("zombie.world.bypass")) {
                event.setCancelled(true);
              }
            });
  }

  @EventHandler(ignoreCancelled = true)
  public void onVoid(PlayerMoveEvent event) {
    Player player = event.getPlayer();
    if (worlds.isInstanceWorld(player.getWorld().getName())
        && configurations.current().settings().worldRules().voidRescueEnabled()
        && player.getY() < player.getWorld().getMinHeight() - 8) {
      player.teleport(player.getWorld().getSpawnLocation());
    }
  }

  private boolean protectedPlayer(Player player) {
    return worlds.isInstanceWorld(player.getWorld().getName())
        && !player.hasPermission("zombie.world.bypass");
  }
}
