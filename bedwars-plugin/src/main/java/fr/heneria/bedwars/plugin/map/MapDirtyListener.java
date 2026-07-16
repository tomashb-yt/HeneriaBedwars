package fr.heneria.bedwars.plugin.map;

import fr.heneria.bedwars.core.map.MapTemplateService;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.world.StructureGrowEvent;

/** Best-effort dirty detection for the most common construction changes. */
public final class MapDirtyListener implements Listener {
  private final MapTemplateService maps;

  public MapDirtyListener(MapTemplateService maps) {
    this.maps = maps;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlace(BlockPlaceEvent event) {
    dirty(event.getBlock().getWorld());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBreak(BlockBreakEvent event) {
    dirty(event.getBlock().getWorld());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBucketEmpty(PlayerBucketEmptyEvent event) {
    dirty(event.getBlockClicked().getWorld());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBucketFill(PlayerBucketFillEvent event) {
    dirty(event.getBlockClicked().getWorld());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityExplosion(EntityExplodeEvent event) {
    dirty(event.getLocation().getWorld());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockExplosion(BlockExplodeEvent event) {
    dirty(event.getBlock().getWorld());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onStructureGrow(StructureGrowEvent event) {
    dirty(event.getWorld());
  }

  private void dirty(World world) {
    if (world == null) return;
    maps.findByWorld(world.getName())
        .filter(template -> template.loaded() && !template.dirty())
        .ifPresent(template -> maps.markDirty(template.id().value()));
  }
}
