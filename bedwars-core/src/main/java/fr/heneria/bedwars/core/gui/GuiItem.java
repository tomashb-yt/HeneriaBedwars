package fr.heneria.bedwars.core.gui;

import java.util.List;
import java.util.Objects;

/** Immutable, platform-neutral description of an inventory item. */
public record GuiItem(
    String material,
    int amount,
    String name,
    List<String> lore,
    boolean glow,
    Integer customModelData) {
  public GuiItem {
    if (material == null || material.isBlank())
      throw new IllegalArgumentException("material must not be blank");
    if (amount < 1 || amount > 99)
      throw new IllegalArgumentException("amount must be between 1 and 99");
    name = Objects.requireNonNullElse(name, "");
    lore = List.copyOf(lore);
  }

  public static GuiItem of(String material, String name, String... lore) {
    return new GuiItem(material, 1, name, List.of(lore), false, null);
  }
}
