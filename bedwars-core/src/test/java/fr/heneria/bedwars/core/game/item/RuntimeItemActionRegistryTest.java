package fr.heneria.bedwars.core.game.item;

import static org.junit.jupiter.api.Assertions.assertEquals;

import fr.heneria.bedwars.api.game.GameState;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RuntimeItemActionRegistryTest {
  private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private final RuntimeItemActionRegistry registry = new RuntimeItemActionRegistry();

  @Test
  void acceptsCurrentMainHandWaitingItem() {
    assertEquals(
        RuntimeItemDecision.EXECUTE,
        registry.evaluate(
            PLAYER, "waiting_leave", "game", "game", GameState.WAITING, true, 1000, 500));
  }

  @Test
  void rejectsOffHandUnknownStaleAndInvalidState() {
    assertEquals(
        RuntimeItemDecision.OFF_HAND,
        decision("waiting_leave", "game", GameState.WAITING, false, 1000));
    assertEquals(
        RuntimeItemDecision.UNKNOWN_KEY,
        decision("unknown", "game", GameState.WAITING, true, 1000));
    assertEquals(
        RuntimeItemDecision.STALE_GAME,
        decision("waiting_info", "old", GameState.WAITING, true, 1000));
    assertEquals(
        RuntimeItemDecision.INVALID_STATE,
        decision("waiting_info", "game", GameState.PLAYING, true, 1000));
  }

  @Test
  void appliesCooldownOncePerActionAndCanForgetPlayer() {
    assertEquals(
        RuntimeItemDecision.EXECUTE,
        decision("waiting_info", "game", GameState.STARTING, true, 1000));
    assertEquals(
        RuntimeItemDecision.COOLDOWN,
        decision("waiting_info", "game", GameState.STARTING, true, 1200));
    assertEquals(
        RuntimeItemDecision.EXECUTE,
        decision("waiting_leave", "game", GameState.STARTING, true, 1200));
    registry.forget(PLAYER);
    assertEquals(
        RuntimeItemDecision.EXECUTE,
        decision("waiting_info", "game", GameState.STARTING, true, 1200));
  }

  @Test
  void spectatorActionsAreRestrictedToPlayingAndEnding() {
    assertEquals(
        RuntimeItemDecision.EXECUTE,
        decision("spectator_teleporter", "game", GameState.PLAYING, true, 1000));
    assertEquals(
        RuntimeItemDecision.EXECUTE,
        decision("spectator_leave", "game", GameState.ENDING, true, 1000));
    assertEquals(
        RuntimeItemDecision.INVALID_STATE,
        decision("spectator_leave", "game", GameState.WAITING, true, 2000));
  }

  private RuntimeItemDecision decision(
      String key, String itemGame, GameState state, boolean mainHand, long now) {
    return registry.evaluate(PLAYER, key, itemGame, "game", state, mainHand, now, 500);
  }
}
