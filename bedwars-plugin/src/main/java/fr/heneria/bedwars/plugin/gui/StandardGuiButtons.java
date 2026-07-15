package fr.heneria.bedwars.plugin.gui;

import fr.heneria.bedwars.core.gui.GuiButton;

/** Translated reusable buttons shared by current and future internal menus. */
public final class StandardGuiButtons {
  public GuiButton back() {
    return GuiButton.builder().itemKey("gui.back").onLeftClick(context -> context.back()).build();
  }

  public GuiButton close() {
    return GuiButton.builder().itemKey("gui.close").onLeftClick(context -> context.close()).build();
  }

  public GuiButton refresh() {
    return GuiButton.builder()
        .itemKey("gui.refresh")
        .onLeftClick(context -> context.refresh())
        .build();
  }

  public GuiButton previous(fr.heneria.bedwars.core.gui.GuiAction action) {
    return GuiButton.builder().itemKey("gui.previous-page").onLeftClick(action).build();
  }

  public GuiButton next(fr.heneria.bedwars.core.gui.GuiAction action) {
    return GuiButton.builder().itemKey("gui.next-page").onLeftClick(action).build();
  }

  public GuiButton information(String itemKey) {
    return GuiButton.builder().itemKey(itemKey).build();
  }
}
