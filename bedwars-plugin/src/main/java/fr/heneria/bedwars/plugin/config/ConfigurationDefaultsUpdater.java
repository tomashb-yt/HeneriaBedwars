package fr.heneria.bedwars.plugin.config;

import fr.heneria.bedwars.core.logging.ProjectLogger;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/** Adds newly introduced optional built-in keys without replacing existing customized values. */
public final class ConfigurationDefaultsUpdater {
  private static final List<String> EVOLVING_FILES =
      List.of("game.yml", "menus.yml", "items.yml", "languages/fr_FR.yml", "languages/en_US.yml");
  private final Path root;
  private final DefaultConfigurationInstaller.ResourceProvider resources;
  private final BackupService backups;
  private final YamlWriter writer;
  private final ProjectLogger logger;

  public ConfigurationDefaultsUpdater(
      Path root,
      DefaultConfigurationInstaller.ResourceProvider resources,
      BackupService backups,
      YamlWriter writer,
      ProjectLogger logger) {
    this.root = root;
    this.resources = resources;
    this.backups = backups;
    this.writer = writer;
    this.logger = logger;
  }

  public int update() throws IOException {
    int updated = 0;
    for (String name : EVOLVING_FILES) {
      Path target = root.resolve(name);
      YamlConfiguration current = load(target);
      YamlConfiguration defaults = defaults(name);
      int added = merge(current, defaults);
      if (added == 0) continue;
      Path backup = backups.backup(target);
      writer.write(target, current.saveToString());
      updated++;
      logger.warning(
          "[Configuration] Added "
              + added
              + " new built-in default key(s) to "
              + name
              + "; existing values were preserved. Backup: "
              + backup);
    }
    return updated;
  }

  private static YamlConfiguration load(Path path) throws IOException {
    YamlConfiguration yaml = new YamlConfiguration();
    try {
      yaml.load(path.toFile());
      return yaml;
    } catch (InvalidConfigurationException exception) {
      throw new IOException("Cannot evolve invalid YAML: " + path, exception);
    }
  }

  private YamlConfiguration defaults(String name) throws IOException {
    try (InputStream input = resources.open(name)) {
      if (input == null) throw new IOException("Missing embedded defaults: " + name);
      YamlConfiguration yaml = new YamlConfiguration();
      try {
        yaml.load(new InputStreamReader(input, StandardCharsets.UTF_8));
        return yaml;
      } catch (InvalidConfigurationException exception) {
        throw new IOException("Invalid embedded defaults: " + name, exception);
      }
    }
  }

  private static int merge(YamlConfiguration current, YamlConfiguration defaults) {
    int added = 0;
    for (var entry : defaults.getValues(true).entrySet()) {
      if (!(entry.getValue() instanceof ConfigurationSection)
          && !current.contains(entry.getKey())) {
        current.set(entry.getKey(), entry.getValue());
        added++;
      }
    }
    return added;
  }
}
