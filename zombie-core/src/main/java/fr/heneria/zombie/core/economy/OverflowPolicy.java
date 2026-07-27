package fr.heneria.zombie.core.economy;

/** Policy applied when a credit would exceed the configured maximum balance. */
public enum OverflowPolicy {
  CLAMP,
  REJECT,
  LOG_AND_CLAMP
}
