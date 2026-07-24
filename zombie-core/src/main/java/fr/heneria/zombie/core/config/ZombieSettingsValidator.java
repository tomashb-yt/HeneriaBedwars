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
      error(issues, "storage.type", "Ticket 001 supports only the sqlite configuration value");
    }
    validateSqlitePath(settings.storage().sqliteFile(), issues);

    int maximumGames = settings.instances().maximumConcurrentGames();
    if (maximumGames == 0 || maximumGames < -1) {
      error(
          issues,
          "instances.maximum-concurrent-games",
          "Maximum games must be -1 or a positive integer");
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
