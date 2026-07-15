package fr.heneria.bedwars.plugin.gui;

import java.util.UUID;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/** Collision-proof marker carrying only immutable GUI view identifiers. */
public final class GuiInventoryHolder implements InventoryHolder {
  private final UUID sessionId;
  private final UUID viewId;
  private final String menuId;
  private Inventory inventory;

  public GuiInventoryHolder(UUID sessionId, UUID viewId, String menuId) {
    this.sessionId = sessionId;
    this.viewId = viewId;
    this.menuId = menuId;
  }

  public UUID sessionId() {
    return sessionId;
  }

  public UUID viewId() {
    return viewId;
  }

  public String menuId() {
    return menuId;
  }

  void inventory(Inventory value) {
    inventory = value;
  }

  @Override
  public @NotNull Inventory getInventory() {
    return inventory;
  }
}
