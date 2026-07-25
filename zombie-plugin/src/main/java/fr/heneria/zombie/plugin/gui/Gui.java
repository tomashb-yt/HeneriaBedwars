package fr.heneria.zombie.plugin.gui;

import net.kyori.adventure.text.Component;

/** Reusable screen contract rendered by the central GUI service. */
public interface Gui {

  /**
   * @return stable screen identifier
   */
  GuiId id();

  /**
   * Resolves the inventory title.
   *
   * @param context render context
   * @return title
   */
  Component title(GuiContext context);

  /**
   * Resolves the inventory size.
   *
   * @param context render context
   * @return multiple of nine from 9 to 54
   */
  int size(GuiContext context);

  /**
   * Renders the current state.
   *
   * @param view mutable render target
   * @param context render context
   */
  void render(GuiView view, GuiContext context);

  /**
   * Periodic refresh interval for dynamic content.
   *
   * @param context render context
   * @return ticks, or zero for no periodic refresh
   */
  default int refreshTicks(GuiContext context) {
    return 0;
  }
}
