package fr.heneria.bedwars.core.game.countdown;

import fr.heneria.bedwars.core.game.GameId;
import java.time.Instant;

/** Immutable countdown view used by commands, displays and tests. */
public record CountdownSnapshot(
    GameId gameId,
    int initialDuration,
    int remainingSeconds,
    GameCountdownStatus status,
    Instant startedAt,
    int minimumPlayers,
    boolean forced,
    boolean accelerated) {}
