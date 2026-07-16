package fr.heneria.bedwars.core.arena.editor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bounded-by-online-players state store; plugin lifecycle removes disconnected players. */
public final class ArenaEditorStateStore {
  private final Map<UUID, ArenaEditorViewState> states = new ConcurrentHashMap<>();

  public ArenaEditorViewState state(UUID playerId) {
    return states.computeIfAbsent(playerId, ignored -> new ArenaEditorViewState());
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
