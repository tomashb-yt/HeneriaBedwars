package fr.heneria.zombie.core.config;

import java.util.Objects;

/**
 * Immutable, platform-independent snapshot of the initial plugin configuration.
 *
 * @param configVersion serialized configuration version
 * @param plugin plugin-level options
 * @param server server world options
 * @param storage persistence options
 * @param instances instance capacity options
 * @param gui interface options
 * @param documentation documentation policy
 */
public record ZombieSettings(
    int configVersion,
    PluginOptions plugin,
    ServerOptions server,
    StorageOptions storage,
    InstanceOptions instances,
    GuiOptions gui,
    DocumentationOptions documentation) {

  /** Validates mandatory nested records. */
  public ZombieSettings {
    Objects.requireNonNull(plugin, "plugin");
    Objects.requireNonNull(server, "server");
    Objects.requireNonNull(storage, "storage");
    Objects.requireNonNull(instances, "instances");
    Objects.requireNonNull(gui, "gui");
    Objects.requireNonNull(documentation, "documentation");
  }

  /**
   * Plugin-level options.
   *
   * @param language default locale identifier
   * @param debug whether verbose diagnostics are enabled
   * @param checkForUpdates whether future update checks are allowed
   */
  public record PluginOptions(String language, boolean debug, boolean checkForUpdates) {
    public PluginOptions {
      Objects.requireNonNull(language, "language");
    }
  }

  /**
   * Server world options.
   *
   * @param lobbyWorld configured Zombies lobby world
   * @param fallbackWorld emergency fallback world
   */
  public record ServerOptions(String lobbyWorld, String fallbackWorld) {
    public ServerOptions {
      Objects.requireNonNull(lobbyWorld, "lobbyWorld");
      Objects.requireNonNull(fallbackWorld, "fallbackWorld");
    }
  }

  /**
   * Persistence options.
   *
   * @param type configured backend identifier
   * @param sqliteFile data-folder-relative SQLite path
   */
  public record StorageOptions(String type, String sqliteFile) {
    public StorageOptions {
      Objects.requireNonNull(type, "type");
      Objects.requireNonNull(sqliteFile, "sqliteFile");
    }
  }

  /**
   * Instance capacity options.
   *
   * @param maximumConcurrentGames {@code -1} for no functional cap, otherwise a positive cap
   * @param automaticCleanup whether disposable worlds should be cleaned automatically
   */
  public record InstanceOptions(int maximumConcurrentGames, boolean automaticCleanup) {}

  /**
   * GUI presentation options.
   *
   * @param defaultTheme stable theme identifier
   * @param soundsEnabled whether GUI sounds are enabled
   * @param animationsEnabled whether GUI animations are enabled
   */
  public record GuiOptions(String defaultTheme, boolean soundsEnabled, boolean animationsEnabled) {
    public GuiOptions {
      Objects.requireNonNull(defaultTheme, "defaultTheme");
    }
  }

  /**
   * Documentation policy.
   *
   * @param requireContextUpdate whether each ticket must update central context
   */
  public record DocumentationOptions(boolean requireContextUpdate) {}
}
