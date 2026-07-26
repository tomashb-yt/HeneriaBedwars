package fr.heneria.zombie.core.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class RoundStateTest {

  @Test
  void enforcesReservationsAliveCapAndIdempotentDefeats() {
    RoundState round = new RoundState(1, 3, Instant.EPOCH);
    round.beginSpawning();

    assertEquals(2, round.reserveSpawns(3, 2));
    round.spawned();
    round.spawned();
    assertEquals(0, round.reserveSpawns(1, 2));
    assertTrue(round.defeated());
    assertEquals(1, round.reserveSpawns(2, 2));
    round.spawned();
    assertTrue(round.defeated());
    assertTrue(round.defeated());
    assertFalse(round.defeated());
    assertTrue(round.canComplete());
    assertTrue(round.complete());
    assertFalse(round.complete());
  }

  @Test
  void rejectsAnUnreservedSpawnAndPrematureCompletion() {
    RoundState round = new RoundState(1, 1, Instant.EPOCH);
    round.beginSpawning();

    assertThrows(IllegalStateException.class, round::spawned);
    assertThrows(IllegalStateException.class, round::complete);
  }
}
