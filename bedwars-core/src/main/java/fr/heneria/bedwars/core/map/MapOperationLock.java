package fr.heneria.bedwars.core.map;

import java.util.HashSet;
import java.util.Set;

/** Small per-map in-process lock; it is always released by service finally blocks. */
public final class MapOperationLock {
  private final Set<MapId> active = new HashSet<>();

  public synchronized boolean acquire(MapId... ids) {
    for (MapId id : ids) if (active.contains(id)) return false;
    active.addAll(Set.of(ids));
    return true;
  }

  public synchronized void release(MapId... ids) {
    for (MapId id : ids) active.remove(id);
  }

  public synchronized boolean active(MapId id) {
    return active.contains(id);
  }

  public synchronized int size() {
    return active.size();
  }
}
