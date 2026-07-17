package fr.heneria.bedwars.core.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CombatTrackerTest {
  @Test
  void recentAttackerReceivesCreditButExpiredAttackDoesNot() {
    CombatTracker tracker = new CombatTracker();
    UUID victim = UUID.randomUUID();
    UUID attacker = UUID.randomUUID();
    Instant now = Instant.parse("2026-07-18T00:00:00Z");
    tracker.record(victim, attacker, now);

    assertEquals(
        attacker,
        tracker.attacker(victim, now.plusSeconds(9), Duration.ofSeconds(10)).orElseThrow());
    assertTrue(tracker.attacker(victim, now.plusSeconds(11), Duration.ofSeconds(10)).isEmpty());
  }
}
