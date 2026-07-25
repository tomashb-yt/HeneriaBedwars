package fr.heneria.zombie.core.config;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Validates a complete settings candidate before it can replace the active snapshot. */
public final class ZombieSettingsValidator {

  /**
   * Validates the supplied settings.
   *
   * @param settings candidate settings
   * @return immutable diagnostics; any {@link ValidationSeverity#ERROR} rejects activation
   */
  public List<ConfigurationIssue> validate(ZombieSettings settings) {
    Objects.requireNonNull(settings, "settings");
    List<ConfigurationIssue> issues = new ArrayList<>();

    if (settings.configVersion() != 1) {
      error(issues, "config-version", "Only configuration version 1 is supported");
    }
    if (settings.plugin().language().isBlank()) {
      error(issues, "plugin.language", "Language must not be blank");
    }
    if (settings.server().lobbyWorld().isBlank()) {
      error(issues, "server.lobby-world", "Lobby world must not be blank");
    }
    if (settings.server().fallbackWorld().isBlank()) {
      error(issues, "server.fallback-world", "Fallback world must not be blank");
    }
    if (!"sqlite".equals(settings.storage().type().toLowerCase(Locale.ROOT))) {
      error(issues, "storage.type", "Only the sqlite configuration value is supported");
    }
    validateSqlitePath(settings.storage().sqliteFile(), issues);
    if (settings.lobby().world().isBlank() || settings.lobby().spawn().world().isBlank()) {
      error(issues, "lobby", "Lobby world and spawn world must not be blank");
    }
    if (!settings.lobby().world().equals(settings.lobby().spawn().world())) {
      error(issues, "lobby.spawn.world", "Lobby spawn must belong to the lobby world");
    }

    int maximumGames = settings.instances().maximumConcurrentGames();
    if (maximumGames == 0 || maximumGames < -1) {
      error(
          issues,
          "instances.maximum-concurrent-games",
          "Maximum games must be -1 or a positive integer");
    }
    validateRelativeDirectory(
        settings.instances().worldsDirectory(), "instances.worlds-directory", issues);
    validateRelativeDirectory(
        settings.instances().templatesDirectory(), "instances.templates-directory", issues);
    if (settings.instances().worldsDirectory().equals(settings.instances().templatesDirectory())) {
      error(issues, "instances", "Worlds and templates directories must be different");
    }
    if (settings.instances().unloadDelaySeconds() < 0) {
      error(issues, "instances.unload-delay-seconds", "Unload delay cannot be negative");
    }
    if (settings.instances().creationTimeoutSeconds() <= 0) {
      error(issues, "instances.creation-timeout-seconds", "Creation timeout must be positive");
    }
    if (settings.instances().defaultMapMaximumPlayers() <= 0) {
      error(
          issues, "instances.default-map-maximum-players", "Default map capacity must be positive");
    }
    if (settings.reconnect().gracePeriodSeconds() <= 0) {
      error(issues, "reconnect.grace-period-seconds", "Reconnect grace period must be positive");
    }
    if (settings.gui().defaultTheme().isBlank()) {
      error(issues, "gui.default-theme", "GUI theme must not be blank");
    }
    if (!settings.documentation().requireContextUpdate()) {
      issues.add(
          new ConfigurationIssue(
              ValidationSeverity.WARNING,
              "documentation.require-context-update",
              "Central context updates are disabled"));
    }
    return List.copyOf(issues);
  }

  private static void validateRelativeDirectory(
      String configuredPath, String configPath, List<ConfigurationIssue> issues) {
    if (configuredPath.isBlank()) {
      error(issues, configPath, "Directory must not be blank");
      return;
    }
    try {
      Path path = Path.of(configuredPath);
      if (path.isAbsolute() || path.getNameCount() != 1 || path.normalize().startsWith("..")) {
        error(issues, configPath, "Directory must be one safe relative folder name");
      }
    } catch (InvalidPathException invalidPath) {
      error(issues, configPath, "Directory path is invalid");
    }
  }

  private static void validateSqlitePath(String configuredPath, List<ConfigurationIssue> issues) {
    if (configuredPath.isBlank()) {
      error(issues, "storage.sqlite-file", "SQLite file must not be blank");
      return;
    }
    try {
      Path path = Path.of(configuredPath);
      if (path.isAbsolute() || path.normalize().startsWith("..")) {
        error(
            issues,
            "storage.sqlite-file",
            "SQLite file must remain relative to the plugin data folder");
      }
    } catch (InvalidPathException invalidPath) {
      error(issues, "storage.sqlite-file", "SQLite file path is invalid");
    }
  }

  private static void error(List<ConfigurationIssue> issues, String path, String message) {
    issues.add(new ConfigurationIssue(ValidationSeverity.ERROR, path, message));
  }
}
