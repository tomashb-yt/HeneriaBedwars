package fr.heneria.zombie.plugin.gui;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable atomically activated GUI configuration. */
public record GuiConfigurationSnapshot(
    String defaultTheme,
    Map<String, GuiTheme> themes,
    Map<GuiId, GuiMenuTemplate> menus,
    List<String> warnings) {

  /** Defensively copies values. */
  public GuiConfigurationSnapshot {
    Objects.requireNonNull(defaultTheme, "defaultTheme");
    themes = Map.copyOf(themes);
    menus = Map.copyOf(menus);
    warnings = List.copyOf(warnings);
  }

  /**
   * @return resolved default theme
   */
  public GuiTheme defaultThemeValue() {
    return Objects.requireNonNull(themes.get(defaultTheme), "default theme");
  }

  /**
   * Resolves a menu theme with default fallback.
   *
   * @param menu menu
   * @return theme
   */
  public GuiTheme theme(GuiMenuTemplate menu) {
    return themes.getOrDefault(menu.theme(), defaultThemeValue());
  }

  /**
   * Finds a menu.
   *
   * @param id id
   * @return optional template
   */
  public Optional<GuiMenuTemplate> menu(GuiId id) {
    return Optional.ofNullable(menus.get(id));
  }
}
