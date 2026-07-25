package fr.heneria.zombie.core.editor;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe source of truth for loaded editable map definitions. */
public final class MapRegistry {
  private final ConcurrentHashMap<String, MapDefinition> maps = new ConcurrentHashMap<>();

  public boolean register(MapDefinition definition) {
    return maps.putIfAbsent(definition.id(), definition) == null;
  }

  public void update(MapDefinition definition) {
    maps.compute(
        definition.id(),
        (ignored, current) -> {
          if (current == null) {
            throw new IllegalArgumentException("Unknown map " + definition.id());
          }
          return definition;
        });
  }

  public Optional<MapDefinition> find(String id) {
    return Optional.ofNullable(maps.get(id));
  }

  public Collection<MapDefinition> all() {
    return maps.values().stream()
        .sorted(java.util.Comparator.comparing(MapDefinition::id))
        .toList();
  }
}
