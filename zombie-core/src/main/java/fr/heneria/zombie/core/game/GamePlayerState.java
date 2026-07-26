package fr.heneria.zombie.core.game;

/** State of one player inside one game aggregate. */
public enum GamePlayerState {
  WAITING,
  ALIVE,
  DOWNED,
  DEAD,
  SPECTATING,
  DISCONNECTED,
  LEFT
}
