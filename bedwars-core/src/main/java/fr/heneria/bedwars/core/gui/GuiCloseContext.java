package fr.heneria.bedwars.core.gui;

import java.util.Objects;

/** Immutable logical close notification including its explicit reason. */
public record GuiCloseContext(GuiSession session, Gui gui, GuiCloseReason reason) {
  public GuiCloseContext {
    Objects.requireNonNull(session);
    Objects.requireNonNull(gui);
    Objects.requireNonNull(reason);
  }
}
