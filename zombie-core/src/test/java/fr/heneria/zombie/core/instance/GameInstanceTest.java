package fr.heneria.zombie.core.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GameInstanceTest {

  @Test
  void enforcesLifecycleCapacityAndImmutableSnapshots() {
    GameInstance instance =
        new GameInstance(
            UUID.randomUUID(),
            "crypt",
            GameInstanceOptions.publicGame(1),
            Instant.parse("2026-01-01T00:00:00Z"));
    UUID first = UUID.randomUUID();

    assertEquals(InstanceJoinResult.NOT_JOINABLE, instance.addPlayer(first));
    instance.markPrepared(new WorldInstanceHandle("hz_test"));
    assertEquals(InstanceJoinResult.JOINED, instance.addPlayer(first));
    assertEquals(InstanceJoinResult.ALREADY_JOINED, instance.addPlayer(first));
    assertEquals(InstanceJoinResult.FULL, instance.addPlayer(UUID.randomUUID()));
    assertThrows(
        UnsupportedOperationException.class,
        () -> instance.snapshot().players().add(UUID.randomUUID()));
    instance.transitionTo(GameInstanceState.STARTING);
    instance.transitionTo(GameInstanceState.RUNNING);
    instance.transitionTo(GameInstanceState.ENDING);
    instance.transitionTo(GameInstanceState.CLEANING);
    instance.transitionTo(GameInstanceState.CLOSED);
    assertThrows(
        InvalidInstanceTransitionException.class,
        () -> instance.transitionTo(GameInstanceState.WAITING));
  }

  @Test
  void privateInstanceAcceptsOnlyItsOwner() {
    UUID owner = UUID.randomUUID();
    GameInstance instance =
        new GameInstance(
            UUID.randomUUID(),
            "crypt",
            new GameInstanceOptions(4, Optional.of(owner), InstanceAccess.PRIVATE),
            Instant.parse("2026-01-01T00:00:00Z"));
    instance.markPrepared(new WorldInstanceHandle("hz_private"));

    assertEquals(InstanceJoinResult.ACCESS_DENIED, instance.addPlayer(UUID.randomUUID()));
    assertEquals(InstanceJoinResult.JOINED, instance.addPlayer(owner));
  }
}
