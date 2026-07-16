package fr.heneria.bedwars.core.map.editor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Per-player list preferences and optimistic revisions without retaining platform players. */
public final class MapEditorViewState {
  private MapListFilter filter = MapListFilter.ALL;
  private MapListSort sort = MapListSort.ID;
  private MapSortDirection direction = MapSortDirection.ASCENDING;
  private int page;
  private String observedMapId;
  private String followedOperationMapId;
  private final Map<String, Long> observedRevisions = new HashMap<>();

  public MapListFilter filter() {
    return filter;
  }

  public void filter(MapListFilter value) {
    filter = value;
    page = 0;
  }

  public MapListSort sort() {
    return sort;
  }

  public void sort(MapListSort value) {
    sort = value;
    page = 0;
  }

  public MapSortDirection direction() {
    return direction;
  }

  public void direction(MapSortDirection value) {
    direction = value;
    page = 0;
  }

  public int page() {
    return page;
  }

  public void page(int value) {
    page = Math.max(0, value);
  }

  public void observe(String mapId, long revision) {
    observedMapId = mapId;
    observedRevisions.put(mapId, revision);
  }

  public Optional<String> observedMapId() {
    return Optional.ofNullable(observedMapId);
  }

  public long observedRevision(String mapId, long fallback) {
    return observedRevisions.getOrDefault(mapId, fallback);
  }

  public void followOperation(String mapId) {
    followedOperationMapId = mapId;
  }

  public Optional<String> followedOperationMapId() {
    return Optional.ofNullable(followedOperationMapId);
  }

  public void forget(String mapId) {
    observedRevisions.remove(mapId);
    if (mapId.equals(observedMapId)) observedMapId = null;
    if (mapId.equals(followedOperationMapId)) followedOperationMapId = null;
  }
}
