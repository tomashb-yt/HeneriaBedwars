package fr.heneria.bedwars.plugin.item;

import java.util.Locale;
import java.util.Optional;
import org.bukkit.inventory.ItemFlag;

/** Safe resolver for Bukkit item flags. */
public final class ItemFlagResolver {
  /** Resolves a flag case-insensitively and returns empty for unknown input. */
  public Optional<ItemFlag> resolve(String value) {
    try {
      return Optional.of(ItemFlag.valueOf(value.toUpperCase(Locale.ROOT)));
    } catch (IllegalArgumentException exception) {
      return Optional.empty();
    }
  }
}
