package fr.heneria.bedwars.plugin.config;

import java.util.Locale;
import org.bukkit.configuration.file.FileConfiguration;

/** Immutable minimal startup configuration loaded from {@code config.yml}. */
public record GeneralConfiguration(
    String language, boolean debug, boolean shutdownOnCriticalStartupError, String storageType) {

  public GeneralConfiguration {
    if (language == null || language.isBlank()) {
      throw new IllegalArgumentException("plugin.language must not be blank");
    }
    if (storageType == null || storageType.isBlank()) {
      throw new IllegalArgumentException("storage.type must not be blank");
    }
    storageType = storageType.toLowerCase(Locale.ROOT);
    if (!storageType.equals("sqlite")) {
      throw new IllegalArgumentException("Only sqlite is declared during Ticket 001");
    }
  }

  public static GeneralConfiguration from(FileConfiguration configuration) {
    return new GeneralConfiguration(
        configuration.getString("plugin.language", "fr_FR"),
        configuration.getBoolean("plugin.debug", false),
        configuration.getBoolean("server.shutdown-on-critical-startup-error", true),
        configuration.getString("storage.type", "sqlite"));
  }
}
