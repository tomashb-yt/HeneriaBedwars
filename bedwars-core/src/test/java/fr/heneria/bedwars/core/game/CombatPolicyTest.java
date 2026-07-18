package fr.heneria.bedwars.core.game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import fr.heneria.bedwars.api.game.GameState;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CombatPolicyTest {
  private static final Instant NOW = Instant.parse("2026-07-18T12:00:00Z");
  private final CombatPolicy policy = new CombatPolicy();

  @Test
  void onlyLivingPlayingParticipantsCanReceiveDamage() {
    RuntimePlayer victim = player("red");

    assertEquals(CombatDecision.NOT_PLAYING, policy.damage(GameState.WAITING, victim, NOW));
    victim.spectator(true);
    assertEquals(CombatDecision.SPECTATOR, policy.damage(GameState.PLAYING, victim, NOW));
  }

  @Test
  void spawnProtectionBlocksDamageUntilItsExactDeadline() {
    RuntimePlayer victim = player("red");
    victim.scheduleRespawn(NOW.minusSeconds(5), NOW.minusSeconds(1));
    victim.completeRespawn(NOW.plusSeconds(3));

    assertEquals(CombatDecision.SPAWN_PROTECTED, policy.damage(GameState.PLAYING, victim, NOW));
    assertEquals(
        CombatDecision.ALLOW, policy.damage(GameState.PLAYING, victim, NOW.plusSeconds(3)));
  }

  @Test
  void friendlyFireAndCrossGameAttacksAreRejected() {
    RuntimePlayer attacker = player("aqua");
    RuntimePlayer teammate = player("aqua");
    RuntimePlayer enemy = player("red");

    assertEquals(
        CombatDecision.FRIENDLY_FIRE,
        policy.attack(GameState.PLAYING, attacker, teammate, true, false, NOW));
    assertEquals(
        CombatDecision.DIFFERENT_GAME,
        policy.attack(GameState.PLAYING, attacker, enemy, false, false, NOW));
    assertEquals(
        CombatDecision.ALLOW, policy.attack(GameState.PLAYING, attacker, enemy, true, false, NOW));
  }

  private static RuntimePlayer player(String team) {
    RuntimePlayer player = new RuntimePlayer(UUID.randomUUID(), NOW.minusSeconds(30));
    player.assignTeam(team);
    return player;
  }
}
