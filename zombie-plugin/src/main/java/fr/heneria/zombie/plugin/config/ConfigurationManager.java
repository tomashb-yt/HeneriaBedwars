package fr.heneria.zombie.plugin.config;

import fr.heneria.zombie.core.config.ConfigurationIssue;
import fr.heneria.zombie.core.config.ValidationSeverity;
import fr.heneria.zombie.core.config.ZombieSettings;
import fr.heneria.zombie.core.config.ZombieSettings.ChatOptions;
import fr.heneria.zombie.core.config.ZombieSettings.DocumentationOptions;
import fr.heneria.zombie.core.config.ZombieSettings.GuiOptions;
import fr.heneria.zombie.core.config.ZombieSettings.InstanceOptions;
import fr.heneria.zombie.core.config.ZombieSettings.LobbyOptions;
import fr.heneria.zombie.core.config.ZombieSettings.LocationOptions;
import fr.heneria.zombie.core.config.ZombieSettings.PluginOptions;
import fr.heneria.zombie.core.config.ZombieSettings.ReconnectOptions;
import fr.heneria.zombie.core.config.ZombieSettings.ServerOptions;
import fr.heneria.zombie.core.config.ZombieSettings.StorageOptions;
import fr.heneria.zombie.core.config.ZombieSettings.WorldRuleOptions;
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
 * replaced. Critical reloads are rejected by the command layer while instances are active.
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
            new LobbyOptions(
                config.getString("lobby.world", config.getString("server.lobby-world", "")),
                new LocationOptions(
                    config.getString(
                        "lobby.spawn.world",
                        config.getString(
                            "lobby.world", config.getString("server.lobby-world", ""))),
                    config.getDouble("lobby.spawn.x", 0.5),
                    config.getDouble("lobby.spawn.y", 65.0),
                    config.getDouble("lobby.spawn.z", 0.5),
                    (float) config.getDouble("lobby.spawn.yaw", 0.0),
                    (float) config.getDouble("lobby.spawn.pitch", 0.0))),
            new InstanceOptions(
                config.getString("instances.worlds-directory", "zombie_instances"),
                config.getString("instances.templates-directory", "zombie_templates"),
                config.getBoolean("instances.delete-world-after-game", true),
                config.getBoolean("instances.preserve-failed-worlds", true),
                config.getInt("instances.unload-delay-seconds", 5),
                config.getInt("instances.creation-timeout-seconds", 60),
                config.getInt("instances.maximum-concurrent-games", -1),
                config.getBoolean("instances.prevent-entry-without-session", true),
                config.getInt("instances.default-map-maximum-players", 4)),
            new ChatOptions(
                config.getBoolean("chat.isolation-enabled", true),
                config.getBoolean("chat.lobby-channel-enabled", true),
                config.getBoolean("chat.instance-channel-enabled", true),
                config.getBoolean("chat.allow-global-admin-channel", true)),
            new ReconnectOptions(
                config.getBoolean("reconnect.enabled", true),
                config.getInt("reconnect.grace-period-seconds", 180),
                config.getBoolean("reconnect.reserve-player-slot", true),
                config.getBoolean("reconnect.return-to-lobby-after-expiration", true)),
            new WorldRuleOptions(
                config.getBoolean("world-rules.allow-natural-mob-spawning", false),
                config.getBoolean("world-rules.allow-weather-cycle", false),
                config.getBoolean("world-rules.allow-time-cycle", false),
                config.getBoolean("world-rules.allow-block-breaking", false),
                config.getBoolean("world-rules.allow-block-placing", false),
                config.getBoolean("world-rules.allow-item-dropping", false),
                config.getBoolean("world-rules.allow-item-pickup", true),
                config.getBoolean("world-rules.allow-pvp", false),
                config.getBoolean("world-rules.keep-inventory", true),
                config.getBoolean("world-rules.void-rescue-enabled", true)),
            new GuiOptions(
                config.getString("gui.default-theme", ""),
                config.getBoolean("gui.sounds-enabled", true),
                config.getBoolean("gui.animations-enabled", true)),
            new DocumentationOptions(
                config.getBoolean("documentation.require-context-update", true)));
    Map<String, String> mergedMessages = bundledMessages();
    mergedMessages.putAll(flatten(messages));
    return new Candidate(
        new ConfigurationSnapshot(settings, mergedMessages), validator.validate(settings));
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

  private Map<String, String> bundledMessages() {
    try (InputStream input = resourceLoader.getResourceAsStream("messages.yml")) {
      if (input == null) {
        throw new IllegalStateException("Missing bundled resource messages.yml");
      }
      return new LinkedHashMap<>(
          flatten(
              YamlConfiguration.loadConfiguration(
                  new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8))));
    } catch (IOException failure) {
      throw new IllegalStateException("Could not read bundled messages.yml", failure);
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
