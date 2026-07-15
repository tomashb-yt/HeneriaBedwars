package fr.heneria.bedwars.plugin.gui;

import fr.heneria.bedwars.core.gui.GuiCloseReason;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;

/** Cancels unsafe inventory interactions and forwards lifecycle events to the GUI service. */
public final class GuiListener implements Listener {
  private final BukkitGuiService service;

  public GuiListener(BukkitGuiService service) {
    this.service = service;
  }

  @EventHandler
  public void onOpen(InventoryOpenEvent event) {
    service.observeOpen(event);
  }

  @EventHandler
  public void onClick(InventoryClickEvent event) {
    service.handleClick(event);
  }

  @EventHandler
  public void onDrag(InventoryDragEvent event) {
    service.handleDrag(event);
  }

  @EventHandler
  public void onClose(InventoryCloseEvent event) {
    service.handleClose(event, GuiCloseReason.PLAYER);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    service.disconnect(event.getPlayer(), GuiCloseReason.DISCONNECT);
  }

  @EventHandler
  public void onKick(PlayerKickEvent event) {
    service.disconnect(event.getPlayer(), GuiCloseReason.KICK);
  }

  @EventHandler
  public void onDisable(PluginDisableEvent event) {
    service.pluginDisabled(event.getPlugin());
  }
}
