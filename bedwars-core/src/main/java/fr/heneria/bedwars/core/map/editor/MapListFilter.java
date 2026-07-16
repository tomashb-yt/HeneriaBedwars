package fr.heneria.bedwars.core.map.editor;

import fr.heneria.bedwars.core.map.MapState;
import fr.heneria.bedwars.core.map.MapTemplate;
import fr.heneria.bedwars.core.map.MapType;

/** Small set of administrator-facing map filters. */
public enum MapListFilter {
  ALL,
  BEDWARS,
  LOBBY,
  GENERIC,
  LOADED,
  UNLOADED,
  ERROR,
  UNLINKED;

  public boolean accepts(MapTemplate template) {
    return switch (this) {
      case ALL -> true;
      case BEDWARS -> template.type() == MapType.BEDWARS;
      case LOBBY -> template.type() == MapType.LOBBY;
      case GENERIC -> template.type() == MapType.GENERIC;
      case LOADED -> template.loaded();
      case UNLOADED -> template.state() == MapState.UNLOADED;
      case ERROR -> template.state() == MapState.ERROR;
      case UNLINKED -> template.linkedArenaIds().isEmpty();
    };
  }

  public MapListFilter next() {
    MapListFilter[] values = values();
    return values[(ordinal() + 1) % values.length];
  }

  public MapListFilter previous() {
    MapListFilter[] values = values();
    return values[(ordinal() - 1 + values.length) % values.length];
  }
}
