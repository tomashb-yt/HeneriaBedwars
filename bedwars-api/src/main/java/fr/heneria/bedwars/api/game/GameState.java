package fr.heneria.bedwars.api.game;

/** Stable lifecycle states exposed to addons. */
public enum GameState {
  CREATING,
  WAITING,
  STARTING,
  PLAYING,
  ENDING,
  RESETTING,
  DESTROYED
}
