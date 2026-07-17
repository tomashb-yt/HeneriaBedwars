package fr.heneria.bedwars.core.game.item;

/** Result of validating one runtime item interaction. */
public enum RuntimeItemDecision {
  EXECUTE,
  OFF_HAND,
  UNKNOWN_KEY,
  STALE_GAME,
  INVALID_STATE,
  COOLDOWN
}
