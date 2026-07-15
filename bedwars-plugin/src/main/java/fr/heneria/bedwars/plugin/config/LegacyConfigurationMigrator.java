package fr.heneria.bedwars.plugin.config;

import fr.heneria.bedwars.core.logging.ProjectLogger;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/** Migrates the recognizable unversioned {@code config.yml} shipped by Ticket 001. */
public final class LegacyConfigurationMigrator implements ConfigurationMigration {
  private final DefaultConfigurationInstaller.ResourceProvider resources;
  private final BackupService backups;
  private final YamlWriter writer;
  private final ProjectLogger logger;

  public LegacyConfigurationMigrator(
      DefaultConfigurationInstaller.ResourceProvider resources,
      BackupService backups,
      YamlWriter writer,
      ProjectLogger logger) {
    this.resources = Objects.requireNonNull(resources, "resources");
    this.backups = Objects.requireNonNull(backups, "backups");
    this.writer = Objects.requireNonNull(writer, "writer");
    this.logger = Objects.requireNonNull(logger, "logger");
  }

  @Override
  public int sourceVersion() {
    return 0;
  }

  @Override
  public int targetVersion() {
    return 1;
  }

  @Override
  public void migrate(Path file) throws IOException {
    migrateIfNeeded(file);
  }

  /** Returns without writing when the file is already versioned. */
  public MigrationResult migrateIfNeeded(Path file) throws IOException {
    YamlConfiguration legacy = load(file);
    if (legacy.contains("config-version")) {
      return new MigrationResult(false, Optional.empty(), 0);
    }
    requireTicket001Signature(legacy);
    YamlConfiguration defaults = loadDefaults();
    int added = mergeMissingDefaults(legacy, defaults);
    Path backup = backups.backup(file);
    try {
      writer.write(file, legacy.saveToString());
    } catch (IOException exception) {
      throw new IOException(
          "Legacy config.yml migration failed; the original file and backup were preserved",
          exception);
    }
    logger.warning("[Configuration] config.yml is using a legacy unversioned format.");
    logger.warning(
        "A backup was created and the file was migrated to configuration version 1: " + backup);
    return new MigrationResult(true, Optional.of(backup), added);
  }

  private static YamlConfiguration load(Path file) throws IOException {
    YamlConfiguration yaml = new YamlConfiguration();
    try {
      yaml.load(file.toFile());
      return yaml;
    } catch (InvalidConfigurationException exception) {
      throw new IOException(
          "Legacy config.yml is not valid YAML and cannot be migrated", exception);
    }
  }

  private YamlConfiguration loadDefaults() throws IOException {
    try (InputStream input = resources.open("config.yml")) {
      if (input == null) throw new IOException("Missing embedded config.yml used for migration");
      YamlConfiguration defaults = new YamlConfiguration();
      try {
        defaults.load(new InputStreamReader(input, StandardCharsets.UTF_8));
      } catch (InvalidConfigurationException exception) {
        throw new IOException("Embedded config.yml is invalid", exception);
      }
      return defaults;
    }
  }

  private static void requireTicket001Signature(YamlConfiguration legacy) throws IOException {
    Object language = legacy.get("plugin.language");
    Object debug = legacy.get("plugin.debug");
    if (!(language instanceof String text) || text.isBlank() || !(debug instanceof Boolean)) {
      throw new IOException(
          "Unversioned config.yml does not match the safe Ticket 001 legacy signature");
    }
  }

  private static int mergeMissingDefaults(YamlConfiguration legacy, YamlConfiguration defaults) {
    int added = 0;
    for (var entry : defaults.getValues(true).entrySet()) {
      if (entry.getValue() instanceof ConfigurationSection || legacy.contains(entry.getKey())) {
        continue;
      }
      legacy.set(entry.getKey(), entry.getValue());
      added++;
    }
    return added;
  }

  /** Summary used by startup diagnostics and tests. */
  public record MigrationResult(boolean migrated, Optional<Path> backup, int defaultsAdded) {
    public MigrationResult {
      Objects.requireNonNull(backup, "backup");
      if (defaultsAdded < 0)
        throw new IllegalArgumentException("defaultsAdded must not be negative");
    }
  }
}
