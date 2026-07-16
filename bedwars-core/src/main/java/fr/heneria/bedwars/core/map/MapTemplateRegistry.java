package fr.heneria.bedwars.core.map;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Copy-on-write registry safe for concurrent command reads and asynchronous copies. */
public final class MapTemplateRegistry {
  private volatile Map<MapId, MapTemplate> templates = Map.of();

  public synchronized void replace(Collection<MapTemplate> values) {
    Map<MapId, MapTemplate> next = new LinkedHashMap<>();
    values.stream()
        .sorted(Comparator.comparing(MapTemplate::id))
        .forEach(
            template -> {
              if (next.putIfAbsent(template.id(), template) != null)
                throw new IllegalArgumentException("Duplicate map id: " + template.id());
            });
    templates = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(next));
  }

  public synchronized void put(MapTemplate template) {
    Map<MapId, MapTemplate> next = new LinkedHashMap<>(templates);
    next.put(template.id(), template);
    templates = java.util.Collections.unmodifiableMap(next);
  }

  public synchronized void remove(MapId id) {
    Map<MapId, MapTemplate> next = new LinkedHashMap<>(templates);
    next.remove(id);
    templates = java.util.Collections.unmodifiableMap(next);
  }

  public Optional<MapTemplate> find(MapId id) {
    return Optional.ofNullable(templates.get(id));
  }

  public Optional<MapTemplate> findByWorld(String worldName) {
    return templates.values().stream()
        .filter(template -> template.worldName().equalsIgnoreCase(worldName))
        .findFirst();
  }

  public List<MapTemplate> linkedToArena(String arenaId) {
    return templates.values().stream()
        .filter(template -> template.linkedArenaIds().contains(arenaId.toLowerCase()))
        .toList();
  }

  public List<MapTemplate> all() {
    return List.copyOf(templates.values());
  }

  public Map<MapId, MapTemplate> snapshot() {
    return templates;
  }
}
