package fr.heneria.zombie.core.config;

import java.util.Objects;

/**
 * Immutable, platform-independent snapshot of the initial plugin configuration.
 *
 * @param configVersion serialized configuration version
 * @param plugin plugin-level options
 * @param server server world options
 * @param storage persistence options
 * @param lobby central lobby options
 * @param instances instance capacity options
 * @param chat chat isolation options
 * @param reconnect reconnection options
 * @param worldRules runtime world protection options
 * @param gui interface options
 * @param documentation documentation policy
 */
public record ZombieSettings(
    int configVersion,
    PluginOptions plugin,
    ServerOptions server,
    StorageOptions storage,
    LobbyOptions lobby,
    InstanceOptions instances,
    ChatOptions chat,
    ReconnectOptions reconnect,
    WorldRuleOptions worldRules,
    GuiOptions gui,
    GameplayOptions gameplay,
    DocumentationOptions documentation) {

  /** Validates mandatory nested records. */
  public ZombieSettings {
    Objects.requireNonNull(plugin, "plugin");
    Objects.requireNonNull(server, "server");
    Objects.requireNonNull(storage, "storage");
    Objects.requireNonNull(lobby, "lobby");
    Objects.requireNonNull(instances, "instances");
    Objects.requireNonNull(chat, "chat");
    Objects.requireNonNull(reconnect, "reconnect");
    Objects.requireNonNull(worldRules, "worldRules");
    Objects.requireNonNull(gui, "gui");
    Objects.requireNonNull(gameplay, "gameplay");
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
   * Central lobby options.
   *
   * @param world lobby world
   * @param spawn lobby spawn
   */
  public record LobbyOptions(String world, LocationOptions spawn) {
    public LobbyOptions {
      Objects.requireNonNull(world, "world");
      Objects.requireNonNull(spawn, "spawn");
    }
  }

  /**
   * Serialized location.
   *
   * @param world world name
   * @param x x coordinate
   * @param y y coordinate
   * @param z z coordinate
   * @param yaw yaw
   * @param pitch pitch
   */
  public record LocationOptions(
      String world, double x, double y, double z, float yaw, float pitch) {
    public LocationOptions {
      Objects.requireNonNull(world, "world");
    }
  }

  /**
   * Instance capacity options.
   *
   * @param worldsDirectory runtime worlds directory
   * @param templatesDirectory source templates directory
   * @param deleteWorldAfterGame whether clean worlds are deleted
   * @param preserveFailedWorlds whether uncertain worlds are retained
   * @param unloadDelaySeconds delay before unloading
   * @param creationTimeoutSeconds maximum creation duration
   * @param maximumConcurrentGames {@code -1} for no functional cap, otherwise a positive cap
   * @param preventEntryWithoutSession whether unmanaged world entry is blocked
   * @param defaultMapMaximumPlayers capacity used by maps without a metadata file
   */
  public record InstanceOptions(
      String worldsDirectory,
      String templatesDirectory,
      boolean deleteWorldAfterGame,
      boolean preserveFailedWorlds,
      int unloadDelaySeconds,
      int creationTimeoutSeconds,
      int maximumConcurrentGames,
      boolean preventEntryWithoutSession,
      int defaultMapMaximumPlayers) {
    public InstanceOptions {
      Objects.requireNonNull(worldsDirectory, "worldsDirectory");
      Objects.requireNonNull(templatesDirectory, "templatesDirectory");
    }
  }

  /**
   * Chat isolation options.
   *
   * @param isolationEnabled whether viewers are context-filtered
   * @param lobbyChannelEnabled whether lobby chat is enabled
   * @param instanceChannelEnabled whether instance chat is enabled
   * @param allowGlobalAdminChannel whether authorized administrators may prefix messages with !
   */
  public record ChatOptions(
      boolean isolationEnabled,
      boolean lobbyChannelEnabled,
      boolean instanceChannelEnabled,
      boolean allowGlobalAdminChannel) {}

  /**
   * Reconnection policy options.
   *
   * @param enabled whether instance membership survives a disconnect
   * @param gracePeriodSeconds grace duration
   * @param reservePlayerSlot whether membership keeps occupying a slot
   * @param returnToLobbyAfterExpiration whether expiration resets to lobby
   */
  public record ReconnectOptions(
      boolean enabled,
      int gracePeriodSeconds,
      boolean reservePlayerSlot,
      boolean returnToLobbyAfterExpiration) {}

  /**
   * Fundamental runtime-world protections.
   *
   * @param allowNaturalMobSpawning natural spawning
   * @param allowWeatherCycle weather cycle
   * @param allowTimeCycle time cycle
   * @param allowBlockBreaking block breaking
   * @param allowBlockPlacing block placing
   * @param allowItemDropping item dropping
   * @param allowItemPickup item pickup
   * @param allowPvp player versus player damage
   * @param keepInventory inventory retention on death
   * @param voidRescueEnabled void rescue
   */
  public record WorldRuleOptions(
      boolean allowNaturalMobSpawning,
      boolean allowWeatherCycle,
      boolean allowTimeCycle,
      boolean allowBlockBreaking,
      boolean allowBlockPlacing,
      boolean allowItemDropping,
      boolean allowItemPickup,
      boolean allowPvp,
      boolean keepInventory,
      boolean voidRescueEnabled) {}

  /**
   * GUI presentation options.
   *
   * @param defaultTheme stable theme identifier
   * @param soundsEnabled whether GUI sounds are enabled
   * @param animationsEnabled whether GUI animations are enabled
   * @param sessionTimeoutSeconds inactive session expiry
   * @param inputTimeoutSeconds chat input expiry
   * @param confirmationDelayTicks dangerous-action delay
   * @param mapsMenuTicks map menu refresh interval
   * @param instancesMenuTicks instance menu refresh interval
   * @param diagnosticsMenuTicks diagnostics refresh interval
   */
  public record GuiOptions(
      String defaultTheme,
      boolean soundsEnabled,
      boolean animationsEnabled,
      int sessionTimeoutSeconds,
      int inputTimeoutSeconds,
      int confirmationDelayTicks,
      int mapsMenuTicks,
      int instancesMenuTicks,
      int diagnosticsMenuTicks) {
    public GuiOptions {
      Objects.requireNonNull(defaultTheme, "defaultTheme");
    }
  }

  /** Complete immutable baseline for the minimal game and round loop. */
  public record GameplayOptions(
      int minimumPlayers,
      int countdownSeconds,
      boolean cancelCountdownWhenInsufficient,
      boolean joinInProgress,
      int endScreenSeconds,
      int startingPoints,
      int maximumRound,
      int firstRoundDelaySeconds,
      int transitionSeconds,
      int enemyBase,
      int enemiesPerRound,
      double playerMultiplier,
      int minimumEnemies,
      int maximumEnemies,
      double baseHealth,
      double healthMultiplier,
      double maximumHealth,
      int maximumAliveBase,
      int maximumAlivePerPlayer,
      int initialSpawnDelayTicks,
      int spawnDelayTicks,
      int minimumSpawnDelayTicks,
      int batchSize,
      boolean downedEnabled,
      int bleedOutSeconds,
      int reviveSeconds,
      double reviveHealth,
      int pointsPerKill) {}

  /**
   * Documentation policy.
   *
   * @param requireContextUpdate whether each ticket must update central context
   */
  public record DocumentationOptions(boolean requireContextUpdate) {}
}
