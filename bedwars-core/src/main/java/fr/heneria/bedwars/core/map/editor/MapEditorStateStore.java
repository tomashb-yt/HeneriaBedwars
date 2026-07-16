package fr.heneria.bedwars.core.map.editor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** State bounded by online administrators and explicitly cleaned on disconnect and shutdown. */
public final class MapEditorStateStore {
  private final Map<UUID, MapEditorViewState> states = new ConcurrentHashMap<>();

  public MapEditorViewState state(UUID playerId) {
    return states.computeIfAbsent(playerId, ignored -> new MapEditorViewState());
  }

  public void forget(String mapId) {
    states.values().forEach(state -> state.forget(mapId));
  }

  public void remove(UUID playerId) {
    states.remove(playerId);
  }

  public void clear() {
    states.clear();
  }

  public int size() {
    return states.size();
  }
}
