package fr.heneria.bedwars.core.arena.editor;

import fr.heneria.bedwars.core.arena.ArenaDefinition;
import fr.heneria.bedwars.core.arena.ArenaStatus;

/** Player-local arena list filter; cycling preserves no mutable arena state. */
public enum ArenaListFilter {
  ALL,
  ENABLED,
  DISABLED,
  INVALID,
  DRAFT;

  /** Returns whether the current registry definition belongs in this view. */
  public boolean accepts(ArenaDefinition arena) {
    return switch (this) {
      case ALL -> true;
      case ENABLED -> arena.enabled();
      case DISABLED -> arena.status() == ArenaStatus.DISABLED;
      case INVALID -> arena.status() == ArenaStatus.INVALID || arena.status() == ArenaStatus.ERROR;
      case DRAFT -> arena.status() == ArenaStatus.DRAFT;
    };
  }

  /** Returns the next filter in the stable menu cycle. */
  public ArenaListFilter next() {
    ArenaListFilter[] values = values();
    return values[(ordinal() + 1) % values.length];
  }
}
