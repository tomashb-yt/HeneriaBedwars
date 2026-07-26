package fr.heneria.zombie.core.game;

/** Stable reasons for a controlled game termination. */
public enum GameEndReason {
  TEAM_ELIMINATED,
  EXTRACTION_SUCCESS,
  EASTER_EGG_COMPLETED,
  ADMIN_STOP,
  ALL_PLAYERS_LEFT,
  SERVER_SHUTDOWN,
  INSTANCE_ERROR,
  MAP_ERROR,
  MAXIMUM_ROUND,
  CUSTOM
}
