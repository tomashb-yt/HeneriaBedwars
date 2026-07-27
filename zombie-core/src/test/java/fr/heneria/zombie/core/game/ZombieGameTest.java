package fr.heneria.zombie.core.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ZombieGameTest {

  @Test
  void completesOneRoundExactlyOnceAndKeepsGamesIsolated() {
    ZombieGame first = game(new MutableClock());
    ZombieGame second = game(new MutableClock());
    UUID firstPlayer = addAndStart(first, 1);
    UUID secondPlayer = addAndStart(second, 1);

    assertEquals(1, first.reserveSpawns(1, 1));
    first.spawned();
    assertTrue(first.zombieDefeated(firstPlayer));
    assertEquals(GameState.ROUND_TRANSITION, first.snapshot().state());
    assertFalse(first.zombieDefeated(firstPlayer));
    assertEquals(GameState.ROUND_ACTIVE, second.snapshot().state());
    assertEquals(0, second.snapshot().players().get(secondPlayer).kills());
  }

  @Test
  void supportsDownReviveAndTeamDefeat() {
    ZombieGame game = game(new MutableClock());
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    game.addPlayer(first);
    game.addPlayer(second);
    game.prepare();
    game.start(1);

    assertTrue(game.hasLivingTeammate(first));
    assertTrue(game.down(first));
    assertTrue(game.revive(first, second));
    assertEquals(1, game.snapshot().players().get(second).revives());
    assertTrue(game.down(first));
    assertTrue(game.eliminate(first));
    assertTrue(game.down(second));
    assertTrue(game.eliminate(second));
    assertTrue(game.defeated());
  }

  @Test
  void soloPlayerHasNoPossibleReviver() {
    ZombieGame game = game(new MutableClock());
    UUID player = addAndStart(game, 1);

    assertFalse(game.hasLivingTeammate(player));
    assertTrue(game.eliminate(player));
    assertTrue(game.defeated());
  }

  @Test
  void restoresAPlayerWithinGraceAndExpiresAfterDeadline() {
    MutableClock clock = new MutableClock();
    ZombieGame game = game(clock);
    UUID player = addAndStart(game, 1);

    assertTrue(game.disconnect(player, clock.instant().plusSeconds(10)));
    clock.advance(Duration.ofSeconds(5));
    assertTrue(game.reconnect(player));
    assertEquals(GamePlayerState.ALIVE, game.snapshot().players().get(player).state());

    game.disconnect(player, clock.instant().plusSeconds(10));
    clock.advance(Duration.ofSeconds(10));
    assertEquals(1, game.expireDisconnectedPlayers());
    assertEquals(GamePlayerState.DEAD, game.snapshot().players().get(player).state());
    assertFalse(game.reconnect(player));
  }

  private static ZombieGame game(Clock clock) {
    return new ZombieGame(
        UUID.randomUUID(), "crypt", GameFixtures.configuration(), clock, event -> {});
  }

  private static UUID addAndStart(ZombieGame game, int enemies) {
    UUID player = UUID.randomUUID();
    game.addPlayer(player);
    game.prepare();
    game.start(enemies);
    return player;
  }

  private static final class MutableClock extends Clock {
    private Instant instant = Instant.EPOCH;

    void advance(Duration duration) {
      instant = instant.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return instant;
    }
  }
}
