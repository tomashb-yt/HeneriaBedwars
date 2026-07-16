package fr.heneria.bedwars.core.map;

/** Runtime technical state; it is recalculated after every server start. */
public enum MapState {
  UNLOADED,
  LOADING,
  LOADED,
  SAVING,
  UNLOADING,
  ERROR
}
