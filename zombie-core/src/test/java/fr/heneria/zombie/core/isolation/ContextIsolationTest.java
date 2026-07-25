package fr.heneria.zombie.core.isolation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.heneria.zombie.core.session.PlayerSessionService;
import fr.heneria.zombie.core.session.ReconnectPolicy;
import java.time.Clock;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContextIsolationTest {

  @Test
  void selectsAudienceAndVisibilityByExactContext() {
    PlayerSessionService sessions =
        new PlayerSessionService(
            () -> new ReconnectPolicy(true, Duration.ofSeconds(30), true, true), Clock.systemUTC());
    UUID lobby = UUID.randomUUID();
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    UUID instanceA = UUID.randomUUID();
    UUID instanceB = UUID.randomUUID();
    sessions.connect(lobby);
    sessions.connect(first);
    sessions.connect(second);
    sessions.assignInstance(first, instanceA);
    sessions.assignInstance(second, instanceB);

    AudienceSelector audiences = new AudienceSelector(sessions);
    VisibilityPolicy visibility = new VisibilityPolicy();
    assertEquals(Set.of(lobby), audiences.lobby());
    assertEquals(Set.of(first), audiences.instance(instanceA));
    assertTrue(
        visibility.canSee(
            sessions.findSession(first).orElseThrow(), sessions.findSession(first).orElseThrow()));
    assertFalse(
        visibility.canSee(
            sessions.findSession(first).orElseThrow(), sessions.findSession(second).orElseThrow()));
    assertFalse(
        visibility.canSee(
            sessions.findSession(lobby).orElseThrow(), sessions.findSession(first).orElseThrow()));
  }
}
