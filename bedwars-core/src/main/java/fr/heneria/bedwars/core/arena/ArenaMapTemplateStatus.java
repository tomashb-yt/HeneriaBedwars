package fr.heneria.bedwars.core.arena;

/** Arena-facing projection of map-template validity without exposing map implementations. */
public enum ArenaMapTemplateStatus {
  VALID,
  NOT_FOUND,
  INVALID_TYPE,
  ERROR
}
