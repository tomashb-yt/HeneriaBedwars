package fr.heneria.zombie.plugin.gui;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Validated cached YAML layout for one screen. */
public record GuiMenuTemplate(
    GuiId id,
    String title,
    int size,
    String theme,
    List<Integer> contentSlots,
    Map<String, GuiButtonTemplate> buttons) {

  /** Defensively copies values. */
  public GuiMenuTemplate {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(theme, "theme");
    contentSlots = List.copyOf(contentSlots);
    buttons = Map.copyOf(buttons);
  }

  /**
   * Finds a configured button.
   *
   * @param key semantic key
   * @return optional template
   */
  public Optional<GuiButtonTemplate> button(String key) {
    return Optional.ofNullable(buttons.get(key));
  }
}
