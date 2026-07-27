package fr.heneria.zombie.core.powerup;

/** Deterministic handling of a second activation of the same power-up. */
public enum StackPolicy {
  REJECT,
  REFRESH_DURATION,
  EXTEND_DURATION,
  MULTIPLY,
  REPLACE,
  KEEP_STRONGEST
}
