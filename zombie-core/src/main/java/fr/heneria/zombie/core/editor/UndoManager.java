package fr.heneria.zombie.core.editor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/** Bounded immutable-version history supporting undo and redo. */
public final class UndoManager {
  private static final int LIMIT = 64;
  private final Deque<MapDefinition> undo = new ArrayDeque<>();
  private final Deque<MapDefinition> redo = new ArrayDeque<>();

  public void record(MapDefinition previous) {
    if (undo.size() == LIMIT) {
      undo.removeFirst();
    }
    undo.addLast(previous);
    redo.clear();
  }

  public Optional<MapDefinition> undo(MapDefinition current) {
    MapDefinition previous = undo.pollLast();
    if (previous == null) {
      return Optional.empty();
    }
    redo.addLast(current);
    return Optional.of(previous);
  }

  public Optional<MapDefinition> redo(MapDefinition current) {
    MapDefinition next = redo.pollLast();
    if (next == null) {
      return Optional.empty();
    }
    undo.addLast(current);
    return Optional.of(next);
  }

  public int undoSize() {
    return undo.size();
  }
}
