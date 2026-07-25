package fr.heneria.zombie.plugin.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Mutable render target that applies the active theme and binds typed actions to slots. */
public final class GuiView {

  private static final MiniMessage MINI = MiniMessage.miniMessage();
  private final Player player;
  private final GuiInventoryHolder holder;
  private final GuiMenuTemplate menu;
  private final GuiTheme theme;
  private final GuiActionRegistry actions;

  GuiView(
      Player player,
      GuiInventoryHolder holder,
      GuiMenuTemplate menu,
      GuiTheme theme,
      GuiActionRegistry actions) {
    this.player = Objects.requireNonNull(player, "player");
    this.holder = Objects.requireNonNull(holder, "holder");
    this.menu = Objects.requireNonNull(menu, "menu");
    this.theme = Objects.requireNonNull(theme, "theme");
    this.actions = Objects.requireNonNull(actions, "actions");
  }

  /** Fills every empty slot with the configured theme background. */
  public void background() {
    ItemStack item = item(theme.backgroundMaterial(), theme.backgroundName(), List.of());
    for (int slot = 0; slot < holder.getInventory().getSize(); slot++) {
      holder.getInventory().setItem(slot, item);
    }
  }

  /**
   * Renders a configured semantic button, respecting its permission visibility.
   *
   * @param key YAML button key
   */
  public void configured(String key) {
    menu.button(key).ifPresent(template -> configured(template));
  }

  /** Renders a dynamic button. */
  public void button(
      int slot,
      Material material,
      String name,
      List<String> lore,
      String permission,
      GuiAction left,
      GuiAction right,
      GuiAction shift) {
    boolean allowed =
        permission == null || permission.isBlank() || player.hasPermission(permission);
    if (GuiPermissionPolicy.evaluate(allowed, false) == GuiPermissionPolicy.Visibility.HIDDEN) {
      return;
    }
    holder.bind(
        slot, new GuiButton(item(material, name, lore), permission, left, right, shift, null));
  }

  /** Renders a non-clickable information item. */
  public void information(int slot, Material material, String name, List<String> lore) {
    holder.getInventory().setItem(slot, item(material, name, lore));
  }

  public GuiMenuTemplate menu() {
    return menu;
  }

  /**
   * @return player owning this isolated render
   */
  public Player player() {
    return player;
  }

  private void configured(GuiButtonTemplate template) {
    boolean allowed =
        template.permission().isBlank() || player.hasPermission(template.permission());
    GuiPermissionPolicy.Visibility visibility =
        GuiPermissionPolicy.evaluate(allowed, template.showWhenLocked());
    if (visibility == GuiPermissionPolicy.Visibility.HIDDEN) {
      return;
    }
    List<String> lore = new ArrayList<>(template.lore());
    Material material = template.material();
    if (visibility == GuiPermissionPolicy.Visibility.LOCKED) {
      lore.add("");
      lore.add("<red>Permission requise : " + template.permission());
      material = Material.BARRIER;
    }
    holder.bind(
        template.slot(),
        new GuiButton(
            item(material, template.name(), lore),
            template.permission(),
            action(template.leftAction()),
            action(template.rightAction()),
            action(template.shiftAction()),
            template.sound()));
  }

  private GuiAction action(String id) {
    return id.isBlank() ? null : actions.find(id).orElse(null);
  }

  private static ItemStack item(Material material, String name, List<String> lore) {
    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();
    meta.displayName(MINI.deserialize(name));
    meta.lore(lore.stream().map(MINI::deserialize).toList());
    item.setItemMeta(meta);
    return item;
  }
}
