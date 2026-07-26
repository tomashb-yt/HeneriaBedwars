package fr.heneria.zombie.core.game;

/** Controlled lifecycle of one round. */
public enum RoundPhase {
  PREPARING,
  SPAWNING,
  ACTIVE,
  COMPLETING,
  COMPLETED,
  CANCELLED
}
