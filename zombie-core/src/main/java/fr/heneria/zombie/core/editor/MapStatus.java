package fr.heneria.zombie.core.editor;

/** Administrative lifecycle of an editable Zombies map. */
public enum MapStatus {
  DRAFT,
  EDITING,
  VALIDATING,
  READY,
  TESTING,
  PUBLISHED,
  MAINTENANCE,
  INVALID,
  ARCHIVED;

  /** Returns whether this status exposes the map in the player catalogue. */
  public boolean playerVisible() {
    return this == PUBLISHED;
  }
}
