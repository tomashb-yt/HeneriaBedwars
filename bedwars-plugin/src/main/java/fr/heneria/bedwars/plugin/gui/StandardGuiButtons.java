package fr.heneria.bedwars.plugin.gui;

import fr.heneria.bedwars.core.config.LanguageService;
import fr.heneria.bedwars.core.config.TranslationKey;
import fr.heneria.bedwars.core.gui.GuiButton;
import fr.heneria.bedwars.core.gui.GuiItem;

/** Translated reusable buttons shared by current and future internal menus. */
public final class StandardGuiButtons {
  private final LanguageService language;

  public StandardGuiButtons(LanguageService language) {
    this.language = language;
  }

  public GuiButton back() {
    return GuiButton.builder()
        .item(GuiItem.of("ARROW", text(TranslationKey.GUI_BACK)))
        .onLeftClick(context -> context.back())
        .build();
  }

  public GuiButton close() {
    return GuiButton.builder()
        .item(GuiItem.of("BARRIER", text(TranslationKey.GUI_CLOSE)))
        .onLeftClick(context -> context.close())
        .build();
  }

  public GuiButton refresh() {
    return GuiButton.builder()
        .item(GuiItem.of("CLOCK", text(TranslationKey.GUI_REFRESH)))
        .onLeftClick(context -> context.refresh())
        .build();
  }

  public GuiButton previous(fr.heneria.bedwars.core.gui.GuiAction action) {
    return GuiButton.builder()
        .item(GuiItem.of("ARROW", text(TranslationKey.GUI_PREVIOUS)))
        .onLeftClick(action)
        .build();
  }

  public GuiButton next(fr.heneria.bedwars.core.gui.GuiAction action) {
    return GuiButton.builder()
        .item(GuiItem.of("ARROW", text(TranslationKey.GUI_NEXT)))
        .onLeftClick(action)
        .build();
  }

  public GuiItem confirmItem() {
    return GuiItem.of("LIME_CONCRETE", text(TranslationKey.GUI_CONFIRM));
  }

  public GuiItem cancelItem() {
    return GuiItem.of("RED_CONCRETE", text(TranslationKey.GUI_CANCEL));
  }

  public GuiButton information(GuiItem item) {
    return GuiButton.builder().item(item).build();
  }

  private String text(TranslationKey key) {
    return language.message(key);
  }
}
