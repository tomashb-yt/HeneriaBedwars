package fr.heneria.zombie.core.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerSessionServiceTest {

  @Test
  void guaranteesSingleMembershipAndExpiresReconnectReservation() {
    MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
    PlayerSessionService sessions =
        new PlayerSessionService(
            () -> new ReconnectPolicy(true, Duration.ofSeconds(30), true, true), clock);
    UUID player = UUID.randomUUID();
    UUID first = UUID.randomUUID();

    assertEquals(ReconnectDecision.RETURN_TO_LOBBY, sessions.connect(player));
    assertEquals(SessionAssignmentResult.ASSIGNED, sessions.assignInstance(player, first));
    assertEquals(
        SessionAssignmentResult.OTHER_INSTANCE, sessions.assignInstance(player, UUID.randomUUID()));
    assertEquals(PlayerContext.INSTANCE, sessions.disconnect(player).context());
    clock.advance(Duration.ofSeconds(31));
    assertEquals(1, sessions.expireReconnectReservations().size());
    PlayerSessionSnapshot expired = sessions.findSession(player).orElseThrow();
    assertEquals(PlayerContext.LOBBY, expired.context());
    assertTrue(expired.instanceId().isEmpty());
  }

  @Test
  void reconnectsInsideGracePeriod() {
    MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
    PlayerSessionService sessions =
        new PlayerSessionService(
            () -> new ReconnectPolicy(true, Duration.ofSeconds(30), true, true), clock);
    UUID player = UUID.randomUUID();
    UUID instance = UUID.randomUUID();
    sessions.connect(player);
    sessions.assignInstance(player, instance);
    sessions.disconnect(player);
    clock.advance(Duration.ofSeconds(10));

    assertEquals(ReconnectDecision.RETURN_TO_INSTANCE, sessions.connect(player));
    assertEquals(instance, sessions.findSession(player).orElseThrow().instanceId().orElseThrow());
  }

  private static final class MutableClock extends Clock {

    private Instant instant;

    private MutableClock(Instant instant) {
      this.instant = instant;
    }

    private void advance(Duration duration) {
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
