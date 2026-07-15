package fr.heneria.bedwars.plugin.item;

import java.util.Locale;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

/** Resolves safe enchantment ranges without requiring a running server in unit tests. */
public final class EnchantmentResolver {
  /** Resolves a safe level range, with an offline table for tests lacking a Bukkit server. */
  public Optional<ResolvedEnchantment> resolve(String name) {
    String key = name.toLowerCase(Locale.ROOT);
    if (Bukkit.getServer() != null) {
      Enchantment value = bukkit(key);
      return value == null
          ? Optional.empty()
          : Optional.of(new ResolvedEnchantment(key, value.getStartLevel(), value.getMaxLevel()));
    }
    return Optional.ofNullable(known(key));
  }

  /** Resolves the live Bukkit enchantment; this method requires an initialized server registry. */
  public Enchantment bukkit(String key) {
    return Bukkit.getRegistry(Enchantment.class)
        .get(NamespacedKey.minecraft(key.toLowerCase(Locale.ROOT)));
  }

  private static ResolvedEnchantment known(String key) {
    return switch (key) {
      case "sharpness", "smite", "bane_of_arthropods", "efficiency", "power" -> range(key, 5);
      case "protection", "projectile_protection", "blast_protection", "fire_protection" ->
          range(key, 4);
      case "unbreaking", "fortune", "looting", "luck_of_the_sea", "lure" -> range(key, 3);
      case "knockback", "fire_aspect", "punch" -> range(key, 2);
      case "mending", "infinity", "silk_touch", "flame", "aqua_affinity" -> range(key, 1);
      default -> null;
    };
  }

  private static ResolvedEnchantment range(String key, int maximum) {
    return new ResolvedEnchantment(key, 1, maximum);
  }

  public record ResolvedEnchantment(String key, int minimum, int maximum) {}
}
