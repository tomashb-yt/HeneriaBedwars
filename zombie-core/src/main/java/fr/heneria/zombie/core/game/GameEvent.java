package fr.heneria.zombie.core.game;

import java.time.Instant;
import java.util.UUID;

/** Immutable internal event published synchronously after committed state changes. */
public record GameEvent(Type type, UUID gameId, int round, Instant occurredAt) {
  public enum Type {
    GAME_CREATED,
    GAME_PREPARING,
    COUNTDOWN_STARTED,
    GAME_STARTED,
    ROUND_STARTED,
    ZOMBIE_REGISTERED,
    ZOMBIE_DEFEATED,
    ROUND_COMPLETED,
    PLAYER_DOWNED,
    PLAYER_REVIVED,
    PLAYER_ELIMINATED,
    GAME_ENDING,
    GAME_ENDED
  }
}
