package fr.heneria.bedwars.core.map.editor;

import fr.heneria.bedwars.core.map.MapTemplate;
import java.time.Instant;
import java.util.Comparator;

/** Pure view sorting; registry order is never changed. */
public enum MapListSort {
  ID(Comparator.comparing(map -> map.id().value())),
  NAME(Comparator.comparing(MapTemplate::displayName, String.CASE_INSENSITIVE_ORDER)),
  TYPE(Comparator.comparing(MapTemplate::type).thenComparing(map -> map.id().value())),
  STATE(Comparator.comparing(MapTemplate::state).thenComparing(map -> map.id().value())),
  UPDATED(Comparator.comparing(MapTemplate::updatedAt)),
  SAVED(Comparator.comparing(map -> map.lastSavedAt().orElse(Instant.EPOCH)));

  private final Comparator<MapTemplate> comparator;

  MapListSort(Comparator<MapTemplate> comparator) {
    this.comparator = comparator;
  }

  public Comparator<MapTemplate> comparator(MapSortDirection direction) {
    return direction == MapSortDirection.ASCENDING ? comparator : comparator.reversed();
  }

  public MapListSort next() {
    MapListSort[] values = values();
    return values[(ordinal() + 1) % values.length];
  }

  public MapListSort previous() {
    MapListSort[] values = values();
    return values[(ordinal() - 1 + values.length) % values.length];
  }
}
