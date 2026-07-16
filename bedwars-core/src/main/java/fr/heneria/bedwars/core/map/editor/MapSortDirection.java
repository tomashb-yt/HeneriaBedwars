package fr.heneria.bedwars.core.map.editor;

public enum MapSortDirection {
  ASCENDING,
  DESCENDING;

  public MapSortDirection opposite() {
    return this == ASCENDING ? DESCENDING : ASCENDING;
  }
}
