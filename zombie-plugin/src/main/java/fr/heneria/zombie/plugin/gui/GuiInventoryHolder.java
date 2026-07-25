package fr.heneria.zombie.plugin.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/** Inventory identity and slot bindings for one rendered session view. */
public final class GuiInventoryHolder implements InventoryHolder {

  private final UUID playerId;
  private final UUID token;
  private final GuiId guiId;
  private final Inventory inventory;
  private final Map<Integer, GuiButton> buttons = new HashMap<>();

  public GuiInventoryHolder(UUID playerId, UUID token, GuiId guiId, int size, Component title) {
    this.playerId = Objects.requireNonNull(playerId, "playerId");
    this.token = Objects.requireNonNull(token, "token");
    this.guiId = Objects.requireNonNull(guiId, "guiId");
    inventory = Bukkit.createInventory(this, size, Objects.requireNonNull(title, "title"));
  }

  public UUID playerId() {
    return playerId;
  }

  public UUID token() {
    return token;
  }

  public GuiId guiId() {
    return guiId;
  }

  public void bind(int slot, GuiButton button) {
    buttons.put(slot, Objects.requireNonNull(button, "button"));
    inventory.setItem(slot, button.item());
  }

  public GuiButton button(int slot) {
    return buttons.get(slot);
  }

  public void reset() {
    buttons.clear();
    inventory.clear();
  }

  @Override
  public @NotNull Inventory getInventory() {
    return inventory;
  }
}
