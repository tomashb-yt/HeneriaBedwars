package fr.heneria.bedwars.core.arena.editor;

import fr.heneria.bedwars.core.arena.ArenaDefinition;
import java.util.Comparator;

/** Stable sort choices available to an arena list session. */
public enum ArenaListSort {
  ID,
  NAME,
  STATUS,
  UPDATED;

  /** Builds a comparator over immutable registry snapshots. */
  public Comparator<ArenaDefinition> comparator() {
    return switch (this) {
      case ID -> Comparator.comparing(arena -> arena.id().value());
      case NAME ->
          Comparator.comparing(ArenaDefinition::displayName, String.CASE_INSENSITIVE_ORDER);
      case STATUS -> Comparator.comparing(arena -> arena.status().name());
      case UPDATED ->
          Comparator.comparing((ArenaDefinition arena) -> arena.metadata().updatedAt()).reversed();
    };
  }

  /** Returns the next sort in the stable menu cycle. */
  public ArenaListSort next() {
    ArenaListSort[] values = values();
    return values[(ordinal() + 1) % values.length];
  }
}
