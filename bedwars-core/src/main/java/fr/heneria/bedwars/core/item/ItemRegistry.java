package fr.heneria.bedwars.core.item;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Immutable registry activated only as part of a complete configuration snapshot. */
public final class ItemRegistry {
  private final Map<ItemKey, ItemDefinition> definitions;
  private final ItemDefinition fallback;

  public ItemRegistry(Map<ItemKey, ItemDefinition> definitions, ItemDefinition fallback) {
    this.definitions = Map.copyOf(new LinkedHashMap<>(definitions));
    this.fallback = fallback;
  }

  /** Finds a definition; malformed and absent keys both produce an empty optional. */
  public Optional<ItemDefinition> find(String key) {
    try {
      return Optional.ofNullable(definitions.get(ItemKey.of(key)));
    } catch (IllegalArgumentException exception) {
      return Optional.empty();
    }
  }

  /** Requires a definition and raises {@link UnknownItemException} instead of returning null. */
  public ItemDefinition require(String key) {
    ItemKey itemKey = ItemKey.of(key);
    return Optional.ofNullable(definitions.get(itemKey))
        .orElseThrow(() -> new UnknownItemException(itemKey));
  }

  /** Returns the validated fallback definition. */
  public ItemDefinition fallback() {
    return fallback;
  }

  /** Returns an immutable set of normalized registered keys. */
  public Set<String> keys() {
    return definitions.keySet().stream()
        .sorted()
        .map(ItemKey::value)
        .collect(java.util.stream.Collectors.toUnmodifiableSet());
  }

  /** Returns the number of normal definitions, excluding the fallback. */
  public int size() {
    return definitions.size();
  }
}
