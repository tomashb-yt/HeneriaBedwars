package fr.heneria.bedwars.core.item;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Fully resolved immutable item definition; it never exposes Bukkit mutable state.
 *
 * @param key stable logical key
 * @param material prevalidated Bukkit material name
 * @param amount valid stack amount
 * @param name direct or translated display name
 * @param lore immutable direct or translated lore
 * @param glow whether a hidden decorative enchantment may be added
 * @param unbreakable unbreakable metadata
 * @param customModelData optional non-negative model identifier
 * @param itemFlags prevalidated Bukkit flag names
 * @param enchantments prevalidated safe enchantment levels
 * @param leatherColor optional RGB integer
 * @param head optional offline-safe head source
 * @param tags non-sensitive configurable metadata
 * @param requiredPlaceholders placeholders required before construction
 */
public record ItemDefinition(
    ItemKey key,
    String material,
    int amount,
    ItemText name,
    List<ItemText> lore,
    boolean glow,
    boolean unbreakable,
    Integer customModelData,
    Set<String> itemFlags,
    Map<String, Integer> enchantments,
    Integer leatherColor,
    HeadDefinition head,
    Map<String, String> tags,
    Set<String> requiredPlaceholders) {
  public ItemDefinition {
    Objects.requireNonNull(key, "key");
    if (material == null || material.isBlank())
      throw new IllegalArgumentException("Blank material");
    if (amount < 1) throw new IllegalArgumentException("Amount must be positive");
    name = Objects.requireNonNullElse(name, ItemText.direct(""));
    lore = List.copyOf(lore);
    itemFlags = Set.copyOf(itemFlags);
    enchantments = Map.copyOf(new LinkedHashMap<>(enchantments));
    tags = Map.copyOf(new LinkedHashMap<>(tags));
    requiredPlaceholders = Set.copyOf(requiredPlaceholders);
  }

  public Optional<Integer> customModelDataValue() {
    return Optional.ofNullable(customModelData);
  }

  public Optional<Integer> leatherColorValue() {
    return Optional.ofNullable(leatherColor);
  }

  public Optional<HeadDefinition> headValue() {
    return Optional.ofNullable(head);
  }
}
