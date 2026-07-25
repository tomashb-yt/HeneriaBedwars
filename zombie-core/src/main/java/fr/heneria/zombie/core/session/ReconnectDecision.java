package fr.heneria.zombie.core.session;

/** Context selected when an existing session reconnects. */
public enum ReconnectDecision {
  RETURN_TO_LOBBY,
  RETURN_TO_INSTANCE
}
