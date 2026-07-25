package fr.heneria.zombie.plugin.gui;

import java.util.Map;
import java.util.Objects;
import org.bukkit.Material;

/** Validated cached visual theme. */
public record GuiTheme(
    String id, Material backgroundMaterial, String backgroundName, Map<String, String> colors) {

  /** Validates and copies theme values. */
  public GuiTheme {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(backgroundMaterial, "backgroundMaterial");
    Objects.requireNonNull(backgroundName, "backgroundName");
    colors = Map.copyOf(colors);
  }
}
