package fr.heneria.bedwars.core.game;

/** Stable reason returned by the pure combat policy before Bukkit mutates an event. */
public enum CombatDecision {
  ALLOW,
  NOT_PLAYING,
  INVALID_PARTICIPANT,
  SPECTATOR,
  RESPAWNING,
  SPAWN_PROTECTED,
  DIFFERENT_GAME,
  FRIENDLY_FIRE
}
