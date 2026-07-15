package fr.heneria.bedwars.plugin.item;

import fr.heneria.bedwars.core.item.ItemContext;
import fr.heneria.bedwars.core.item.ItemDefinition;
import fr.heneria.bedwars.core.item.ItemReloadResult;
import java.util.Optional;
import java.util.Set;
import org.bukkit.inventory.ItemStack;

/**
 * Central Bukkit item service. Every build returns a fresh mutable stack and must run on the server
 * thread when a Bukkit player context is involved.
 */
public interface ItemService {
  /** Builds a fresh stack or throws for an absent key, missing context, or adapter failure. */
  ItemStack build(String key, ItemContext context);

  /** Builds a fresh stack and converts every failure into the centralized safe fallback. */
  ItemStack buildOrFallback(String key, ItemContext context);

  /** Finds one definition in the currently active immutable snapshot. */
  Optional<ItemDefinition> findDefinition(String key);

  /** Requires one definition and never returns null. */
  ItemDefinition requireDefinition(String key);

  /** Lists the normalized keys in the active registry. */
  Set<String> registeredKeys();

  /** Checks a key without raising an exception for malformed input. */
  boolean exists(String key);

  /** Reloads the complete configuration transactionally, including languages and items. */
  ItemReloadResult reload();
}
