package fr.heneria.zombie.plugin.gui;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.Sound;

/** Cached YAML presentation and action references for one button. */
public record GuiButtonTemplate(
    int slot,
    Material material,
    String name,
    List<String> lore,
    String permission,
    boolean showWhenLocked,
    String leftAction,
    String rightAction,
    String shiftAction,
    Sound sound) {

  /** Validates and defensively copies values. */
  public GuiButtonTemplate {
    Objects.requireNonNull(material, "material");
    Objects.requireNonNull(name, "name");
    lore = List.copyOf(lore);
    permission = permission == null ? "" : permission;
    leftAction = leftAction == null ? "" : leftAction;
    rightAction = rightAction == null ? "" : rightAction;
    shiftAction = shiftAction == null ? "" : shiftAction;
  }

  /**
   * @return optional click sound
   */
  public Optional<Sound> clickSound() {
    return Optional.ofNullable(sound);
  }
}
