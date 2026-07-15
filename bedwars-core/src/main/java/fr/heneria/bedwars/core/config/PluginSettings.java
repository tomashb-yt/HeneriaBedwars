package fr.heneria.bedwars.core.config;

import java.util.Locale;

/** Immutable general plugin settings. */
public record PluginSettings(
    String locale,
    boolean debug,
    boolean updateCheckEnabled,
    boolean confirmDangerousActions,
    boolean preventReloadDuringGames,
    int mainThreadWarningMillis) {
  public PluginSettings {
    if (locale == null || locale.isBlank())
      throw new IllegalArgumentException("locale must not be blank");
    locale = locale.trim();
    if (mainThreadWarningMillis < 1)
      throw new IllegalArgumentException("mainThreadWarningMillis must be positive");
  }

  public Locale javaLocale() {
    return Locale.forLanguageTag(locale.replace('_', '-'));
  }
}
