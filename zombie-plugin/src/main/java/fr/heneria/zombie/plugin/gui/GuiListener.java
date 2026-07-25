package fr.heneria.zombie.plugin.gui;

import java.util.Objects;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Thin Paper listener delegating all inventory policy to {@link GuiService}. */
public final class GuiListener implements Listener {
  private final GuiService service;

  public GuiListener(GuiService service) {
    this.service = Objects.requireNonNull(service, "service");
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onClick(InventoryClickEvent event) {
    service.handleClick(event);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onDrag(InventoryDragEvent event) {
    service.handleDrag(event);
  }

  @EventHandler
  public void onClose(InventoryCloseEvent event) {
    service.handleClose(event);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    service.remove(event.getPlayer().getUniqueId());
  }
}
