package fr.heneria.zombie.plugin.config;

import fr.heneria.zombie.core.config.ConfigurationIssue;
import fr.heneria.zombie.core.config.ValidationSeverity;
import fr.heneria.zombie.core.config.ZombieSettings;
import fr.heneria.zombie.core.config.ZombieSettings.DocumentationOptions;
import fr.heneria.zombie.core.config.ZombieSettings.GuiOptions;
import fr.heneria.zombie.core.config.ZombieSettings.InstanceOptions;
import fr.heneria.zombie.core.config.ZombieSettings.PluginOptions;
import fr.heneria.zombie.core.config.ZombieSettings.ServerOptions;
import fr.heneria.zombie.core.config.ZombieSettings.StorageOptions;
import fr.heneria.zombie.core.config.ZombieSettingsValidator;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Installs defaults and exposes only validated configuration snapshots.
 *
 * <p>Reload is transactional: parsing and validation happen before the active atomic reference is
 * replaced. Runtime map, world and game state are deliberately outside Ticket 001 reload scope.
 */
public final class ConfigurationManager {

  private final Path dataDirectory;
  private final ClassLoader resourceLoader;
  private final ZombieSettingsValidator validator;
  private final AtomicReference<ConfigurationSnapshot> active = new AtomicReference<>();

  /**
   * Creates the configuration manager.
   *
   * @param dataDirectory plugin data folder
   * @param resourceLoader class loader containing defaults
   * @param validator settings validator
   */
  public ConfigurationManager(
      Path dataDirectory, ClassLoader resourceLoader, ZombieSettingsValidator validator) {
    this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
    this.resourceLoader = Objects.requireNonNull(resourceLoader, "resourceLoader");
    this.validator = Objects.requireNonNull(validator, "validator");
  }

  /**
   * Creates missing files and activates the first valid snapshot.
   *
   * @return validation warnings
   * @throws IOException when files cannot be created or read
   * @throws InvalidConfigurationException when defaults or existing files are invalid
   */
  public List<ConfigurationIssue> initialize() throws IOException, InvalidConfigurationException {
    Files.createDirectories(dataDirectory);
    installDefault("config.yml");
    installDefault("messages.yml");
    Candidate candidate = loadCandidate();
    List<ConfigurationIssue> errors =
        candidate.issues().stream()
            .filter(issue -> issue.severity() == ValidationSeverity.ERROR)
            .toList();
    if (!errors.isEmpty()) {
      throw new InvalidConfigurationException(errors);
    }
    active.set(candidate.snapshot());
    return candidate.issues();
  }

  /**
   * Reloads safe scalar configuration and messages.
   *
   * @return reload outcome; a rejected candidate leaves the previous snapshot untouched
   */
  public ReloadResult reload() {
    try {
      Candidate candidate = loadCandidate();
      boolean rejected =
          candidate.issues().stream()
              .anyMatch(issue -> issue.severity() == ValidationSeverity.ERROR);
      if (rejected) {
        return new ReloadResult(false, candidate.issues());
      }
      active.set(candidate.snapshot());
      return new ReloadResult(true, candidate.issues());
    } catch (RuntimeException exception) {
      return new ReloadResult(
          false,
          List.of(
              new ConfigurationIssue(
                  ValidationSeverity.ERROR, "root", "YAML illisible: " + exception.getMessage())));
    }
  }

  /**
   * Returns the current validated snapshot.
   *
   * @return active snapshot
   * @throws IllegalStateException before successful initialization
   */
  public ConfigurationSnapshot current() {
    ConfigurationSnapshot snapshot = active.get();
    if (snapshot == null) {
      throw new IllegalStateException("Configuration is not initialized");
    }
    return snapshot;
  }

  private Candidate loadCandidate() {
    YamlConfiguration config =
        YamlConfiguration.loadConfiguration(dataDirectory.resolve("config.yml").toFile());
    YamlConfiguration messages =
        YamlConfiguration.loadConfiguration(dataDirectory.resolve("messages.yml").toFile());
    ZombieSettings settings =
        new ZombieSettings(
            config.getInt("config-version", 1),
            new PluginOptions(
                config.getString("plugin.language", ""),
                config.getBoolean("plugin.debug", false),
                config.getBoolean("plugin.check-for-updates", false)),
            new ServerOptions(
                config.getString("server.lobby-world", ""),
                config.getString("server.fallback-world", "")),
            new StorageOptions(
                config.getString("storage.type", ""), config.getString("storage.sqlite-file", "")),
            new InstanceOptions(
                config.getInt("instances.maximum-concurrent-games", -1),
                config.getBoolean("instances.automatic-cleanup", true)),
            new GuiOptions(
                config.getString("gui.default-theme", ""),
                config.getBoolean("gui.sounds-enabled", true),
                config.getBoolean("gui.animations-enabled", true)),
            new DocumentationOptions(
                config.getBoolean("documentation.require-context-update", true)));
    return new Candidate(
        new ConfigurationSnapshot(settings, flatten(messages)), validator.validate(settings));
  }

  private void installDefault(String resourceName) throws IOException {
    Path target = dataDirectory.resolve(resourceName);
    if (Files.exists(target)) {
      return;
    }
    try (InputStream input = resourceLoader.getResourceAsStream(resourceName)) {
      if (input == null) {
        throw new IOException("Missing bundled resource " + resourceName);
      }
      Files.copy(input, target);
    }
  }

  private static Map<String, String> flatten(YamlConfiguration yaml) {
    Map<String, String> values = new LinkedHashMap<>();
    copySection(yaml, "", values);
    return values;
  }

  private static void copySection(
      ConfigurationSection section, String prefix, Map<String, String> target) {
    for (String key : section.getKeys(false)) {
      String path = prefix.isEmpty() ? key : prefix + "." + key;
      if (section.isConfigurationSection(key)) {
        ConfigurationSection child = section.getConfigurationSection(key);
        if (child != null) {
          copySection(child, path, target);
        }
      } else if (section.isString(key)) {
        target.put(path, section.getString(key, ""));
      }
    }
  }

  private record Candidate(ConfigurationSnapshot snapshot, List<ConfigurationIssue> issues) {}
}
