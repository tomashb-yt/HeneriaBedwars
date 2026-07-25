package fr.heneria.zombie.plugin.gui;

import java.util.Objects;

/** Stable namespaced identifier of a GUI screen. */
public record GuiId(String value) {

  /** Validates the serialized identifier. */
  public GuiId {
    Objects.requireNonNull(value, "value");
    if (!value.matches("[a-z0-9][a-z0-9_-]{0,63}")) {
      throw new IllegalArgumentException("Invalid GUI id: " + value);
    }
  }

  @Override
  public String toString() {
    return value;
  }
}
