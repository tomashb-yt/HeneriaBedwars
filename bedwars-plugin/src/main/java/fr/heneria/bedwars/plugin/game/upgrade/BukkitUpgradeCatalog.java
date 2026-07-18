package fr.heneria.bedwars.plugin.game.upgrade;

import fr.heneria.bedwars.core.config.ConfigurationDocument;
import fr.heneria.bedwars.core.config.ConfigurationId;
import fr.heneria.bedwars.core.game.shop.ShopCurrency;
import fr.heneria.bedwars.core.game.upgrade.TeamUpgradeCatalog;
import fr.heneria.bedwars.core.game.upgrade.TeamUpgradeDefinition;
import fr.heneria.bedwars.core.game.upgrade.TeamUpgradeType;
import fr.heneria.bedwars.core.logging.ProjectLogger;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Builds an immutable runtime upgrade catalog from upgrades.yml. */
public final class BukkitUpgradeCatalog {
  private final ConfigurationService configurations;
  private final ProjectLogger logger;

  public BukkitUpgradeCatalog(ConfigurationService configurations, ProjectLogger logger) {
    this.configurations = Objects.requireNonNull(configurations, "configurations");
    this.logger = Objects.requireNonNull(logger, "logger");
  }

  public TeamUpgradeCatalog current() {
    ConfigurationDocument document =
        configurations.snapshot().documents().get(ConfigurationId.UPGRADES);
    if (!bool(document, "upgrades.runtime-enabled", true)) return new TeamUpgradeCatalog(List.of());
    Set<String> ids = new TreeSet<>();
    document.values().keySet().stream()
        .filter(key -> key.startsWith("upgrades.definitions.") && key.endsWith(".currency"))
        .map(
            key ->
                key.substring(
                    "upgrades.definitions.".length(), key.length() - ".currency".length()))
        .forEach(ids::add);
    var definitions = new ArrayList<TeamUpgradeDefinition>();
    for (String id : ids) {
      String prefix = "upgrades.definitions." + id + '.';
      try {
        definitions.add(
            new TeamUpgradeDefinition(
                TeamUpgradeType.valueOf(id.toUpperCase(Locale.ROOT)),
                ShopCurrency.valueOf(
                    string(document, prefix + "currency", "DIAMOND").toUpperCase(Locale.ROOT)),
                prices(document.value(prefix + "prices")),
                string(document, prefix + "translation-key", "upgrade." + id),
                integer(document, prefix + "order", definitions.size())));
      } catch (RuntimeException exception) {
        logger.warning(
            "[Upgrade] Ignoring invalid definition '" + id + "': " + exception.getMessage());
      }
    }
    return new TeamUpgradeCatalog(definitions);
  }

  public boolean enabled() {
    return bool(
        configurations.snapshot().documents().get(ConfigurationId.UPGRADES),
        "upgrades.runtime-enabled",
        true);
  }

  private static List<Integer> prices(Object value) {
    if (!(value instanceof List<?> list))
      throw new IllegalArgumentException("prices must be a list");
    return list.stream()
        .map(
            item -> {
              if (item instanceof Number number) return number.intValue();
              return Integer.parseInt(String.valueOf(item));
            })
        .toList();
  }

  private static String string(ConfigurationDocument document, String key, String fallback) {
    Object value = document.value(key);
    return (value == null ? fallback : String.valueOf(value)).trim();
  }

  private static int integer(ConfigurationDocument document, String key, int fallback) {
    Object value = document.value(key);
    return value instanceof Number number ? number.intValue() : fallback;
  }

  private static boolean bool(ConfigurationDocument document, String key, boolean fallback) {
    Object value = document.value(key);
    return value instanceof Boolean bool ? bool : fallback;
  }
}
