package fr.heneria.bedwars.core.game;

import fr.heneria.bedwars.api.game.GameState;
import java.time.Instant;
import java.util.Objects;

/** Pure BedWars combat authorization policy shared by every platform adapter. */
public final class CombatPolicy {
  public CombatDecision damage(GameState state, RuntimePlayer victim, Instant now) {
    Objects.requireNonNull(state, "state");
    Objects.requireNonNull(now, "now");
    if (state != GameState.PLAYING) return CombatDecision.NOT_PLAYING;
    if (victim == null || victim.finalDeath()) return CombatDecision.INVALID_PARTICIPANT;
    if (victim.respawning()) return CombatDecision.RESPAWNING;
    if (victim.spectator()) return CombatDecision.SPECTATOR;
    if (victim.protectedAt(now)) return CombatDecision.SPAWN_PROTECTED;
    return CombatDecision.ALLOW;
  }

  public CombatDecision attack(
      GameState state,
      RuntimePlayer attacker,
      RuntimePlayer victim,
      boolean sameGame,
      boolean friendlyFire,
      Instant now) {
    CombatDecision victimDecision = damage(state, victim, now);
    if (victimDecision != CombatDecision.ALLOW) return victimDecision;
    if (attacker == null || attacker.finalDeath()) return CombatDecision.INVALID_PARTICIPANT;
    if (attacker.respawning()) return CombatDecision.RESPAWNING;
    if (attacker.spectator()) return CombatDecision.SPECTATOR;
    if (!sameGame) return CombatDecision.DIFFERENT_GAME;
    if (!friendlyFire && attacker.teamId().isPresent() && attacker.teamId().equals(victim.teamId()))
      return CombatDecision.FRIENDLY_FIRE;
    return CombatDecision.ALLOW;
  }
}
