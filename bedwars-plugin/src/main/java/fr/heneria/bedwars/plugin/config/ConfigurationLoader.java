package fr.heneria.bedwars.plugin.config;

import fr.heneria.bedwars.core.config.ConfigurationDocument;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/** Reads one UTF-8 YAML file into an immutable flattened document. */
public final class ConfigurationLoader {
  public ConfigurationDocument load(String id, File file)
      throws IOException, InvalidConfigurationException {
    YamlConfiguration yaml = new YamlConfiguration();
    yaml.load(file);
    Map<String, Object> values = new LinkedHashMap<>();
    yaml.getValues(true)
        .forEach(
            (key, value) -> {
              if (!(value instanceof ConfigurationSection)) values.put(key, value);
            });
    return new ConfigurationDocument(id, file.getName(), yaml.getInt("config-version", -1), values);
  }
}
