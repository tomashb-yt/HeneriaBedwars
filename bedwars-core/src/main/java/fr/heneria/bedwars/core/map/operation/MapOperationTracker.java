package fr.heneria.bedwars.core.map.operation;

import fr.heneria.bedwars.core.map.MapId;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory visibility for long operations, independent from open inventory sessions. */
public final class MapOperationTracker {
  private final Clock clock;
  private final Map<MapId, MapOperationSnapshot> operations = new ConcurrentHashMap<>();

  public MapOperationTracker(Clock clock) {
    this.clock = clock;
  }

  public synchronized Optional<MapOperationSnapshot> start(
      MapId mapId, MapOperationType type, UUID initiatedBy, String detail) {
    MapOperationSnapshot current = operations.get(mapId);
    if (current != null && current.status().active()) return Optional.empty();
    MapOperationSnapshot snapshot =
        new MapOperationSnapshot(
            mapId, type, clock.instant(), initiatedBy, MapOperationStatus.PENDING, clean(detail));
    operations.put(mapId, snapshot);
    return Optional.of(snapshot);
  }

  public void running(MapId mapId, String detail) {
    update(mapId, MapOperationStatus.RUNNING, detail);
  }

  public void success(MapId mapId, String detail) {
    update(mapId, MapOperationStatus.SUCCESS, detail);
  }

  public void failed(MapId mapId, String detail) {
    update(mapId, MapOperationStatus.FAILED, detail);
  }

  public void cancelled(MapId mapId, String detail) {
    update(mapId, MapOperationStatus.CANCELLED, detail);
  }

  public Optional<MapOperationSnapshot> find(MapId mapId) {
    return Optional.ofNullable(operations.get(mapId));
  }

  public boolean active(MapId mapId) {
    return find(mapId).map(snapshot -> snapshot.status().active()).orElse(false);
  }

  public void forget(MapId mapId) {
    operations.remove(mapId);
  }

  public void clear() {
    operations.clear();
  }

  private synchronized void update(MapId mapId, MapOperationStatus status, String detail) {
    MapOperationSnapshot current = operations.get(mapId);
    if (current == null) return;
    operations.put(
        mapId,
        new MapOperationSnapshot(
            current.mapId(),
            current.type(),
            current.startedAt(),
            current.initiatedBy(),
            status,
            clean(detail)));
  }

  private static String clean(String value) {
    return value == null ? "" : value.trim();
  }
}
