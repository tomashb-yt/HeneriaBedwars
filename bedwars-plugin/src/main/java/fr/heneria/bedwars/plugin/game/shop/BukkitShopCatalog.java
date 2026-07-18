package fr.heneria.bedwars.plugin.game.shop;

import fr.heneria.bedwars.core.config.ConfigurationDocument;
import fr.heneria.bedwars.core.config.ConfigurationId;
import fr.heneria.bedwars.core.game.shop.ShopCatalog;
import fr.heneria.bedwars.core.game.shop.ShopCategory;
import fr.heneria.bedwars.core.game.shop.ShopCurrency;
import fr.heneria.bedwars.core.game.shop.ShopOffer;
import fr.heneria.bedwars.core.game.shop.ShopOfferId;
import fr.heneria.bedwars.core.logging.ProjectLogger;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.bukkit.Material;

/** Builds the immutable runtime catalog from the active shops.yml snapshot. */
public final class BukkitShopCatalog {
  private final ConfigurationService configurations;
  private final ProjectLogger logger;

  public BukkitShopCatalog(ConfigurationService configurations, ProjectLogger logger) {
    this.configurations = Objects.requireNonNull(configurations, "configurations");
    this.logger = Objects.requireNonNull(logger, "logger");
  }

  public ShopCatalog current() {
    ConfigurationDocument document =
        configurations.snapshot().documents().get(ConfigurationId.SHOPS);
    if (!bool(document, "shops.runtime-enabled", true)) return new ShopCatalog(java.util.List.of());
    Set<String> ids = new TreeSet<>();
    document.values().keySet().stream()
        .filter(key -> key.startsWith("shops.offers.") && key.endsWith(".material"))
        .map(key -> key.substring("shops.offers.".length(), key.length() - ".material".length()))
        .forEach(ids::add);
    var offers = new ArrayList<ShopOffer>();
    for (String id : ids) {
      String prefix = "shops.offers." + id + '.';
      try {
        String materialName = string(document, prefix + "material", "BARRIER");
        Material material = Material.matchMaterial(materialName);
        if (material == null || !material.isItem())
          throw new IllegalArgumentException("invalid material " + materialName);
        offers.add(
            new ShopOffer(
                new ShopOfferId(id),
                ShopCategory.valueOf(
                    string(document, prefix + "category", "UTILITY").toUpperCase(Locale.ROOT)),
                material.name(),
                integer(document, prefix + "amount", 1),
                ShopCurrency.valueOf(
                    string(document, prefix + "currency", "IRON").toUpperCase(Locale.ROOT)),
                integer(document, prefix + "price", 1),
                string(document, prefix + "translation-key", "shop.offer." + id),
                integer(document, prefix + "order", offers.size())));
      } catch (RuntimeException exception) {
        logger.warning("[Shop] Ignoring invalid offer '" + id + "': " + exception.getMessage());
      }
    }
    return new ShopCatalog(offers);
  }

  public boolean enabled() {
    ConfigurationDocument document =
        configurations.snapshot().documents().get(ConfigurationId.SHOPS);
    return bool(document, "shops.runtime-enabled", true);
  }

  private static String string(ConfigurationDocument document, String key, String fallback) {
    Object value = document.value(key);
    return value == null ? fallback : String.valueOf(value).trim();
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
