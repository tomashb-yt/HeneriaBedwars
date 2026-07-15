package fr.heneria.bedwars.core.item;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;

/** Resolves item inheritance without mutating parents and rejects cycles atomically. */
public final class ItemInheritanceResolver {
  private final int maximumDepth;

  /** Creates a resolver with a strictly positive maximum parent-chain depth. */
  public ItemInheritanceResolver(int maximumDepth) {
    if (maximumDepth < 1) throw new IllegalArgumentException("maximumDepth must be positive");
    this.maximumDepth = maximumDepth;
  }

  /**
   * Resolves a complete temporary snapshot without mutating any template.
   *
   * @throws ItemResolutionException for an unknown parent, cycle, or excessive depth
   */
  public Map<ItemKey, ItemDefinitionTemplate> resolve(
      Map<ItemKey, ItemDefinitionTemplate> templates) {
    Map<ItemKey, ItemDefinitionTemplate> resolved = new LinkedHashMap<>();
    for (ItemKey key : templates.keySet()) resolve(key, templates, resolved, new ArrayDeque<>());
    return Map.copyOf(resolved);
  }

  private ItemDefinitionTemplate resolve(
      ItemKey key,
      Map<ItemKey, ItemDefinitionTemplate> templates,
      Map<ItemKey, ItemDefinitionTemplate> resolved,
      ArrayDeque<ItemKey> path) {
    if (path.contains(key))
      throw new ItemResolutionException("Item inheritance cycle: " + path + " -> " + key);
    if (path.size() >= maximumDepth)
      throw new ItemResolutionException("Item inheritance exceeds depth " + maximumDepth);
    if (resolved.containsKey(key)) return resolved.get(key);
    ItemDefinitionTemplate current = templates.get(key);
    if (current == null) throw new ItemResolutionException("Unknown item parent '" + key + "'");
    path.addLast(key);
    ItemDefinitionTemplate value = current;
    if (current.parent() != null) {
      ItemDefinitionTemplate parent = resolve(current.parent(), templates, resolved, path);
      value = current.inherit(parent);
    }
    path.removeLast();
    resolved.put(key, value);
    return value;
  }
}
