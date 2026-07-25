package fr.heneria.zombie.plugin.gui;

import java.util.Objects;
import java.util.Optional;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

/**
 * Immutable rendered button with distinct normal, right and shift actions.
 *
 * @param item displayed item
 * @param permission optional click permission
 * @param leftAction left-click action
 * @param rightAction right-click action
 * @param shiftAction shift-click action
 */
public record GuiButton(
    ItemStack item,
    String permission,
    GuiAction leftAction,
    GuiAction rightAction,
    GuiAction shiftAction,
    Sound sound) {

  /** Defensively copies the item. */
  public GuiButton {
    item = Objects.requireNonNull(item, "item").clone();
    permission = permission == null ? "" : permission;
  }

  /**
   * Resolves an action for a click family.
   *
   * @param shift shift state
   * @param right right-click state
   * @return optional action
   */
  public Optional<GuiAction> action(boolean shift, boolean right) {
    GuiAction selected =
        shift && shiftAction != null ? shiftAction : right ? rightAction : leftAction;
    return Optional.ofNullable(selected);
  }

  /**
   * @return configured click sound
   */
  public Optional<Sound> clickSound() {
    return Optional.ofNullable(sound);
  }

  @Override
  public ItemStack item() {
    return item.clone();
  }
}
