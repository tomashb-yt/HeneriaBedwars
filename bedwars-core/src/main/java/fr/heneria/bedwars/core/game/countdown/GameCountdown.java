package fr.heneria.bedwars.core.game.countdown;

import fr.heneria.bedwars.core.game.GameId;
import java.time.Instant;
import java.util.Objects;

/** Pure, synchronized countdown state; scheduling is deliberately owned by one central ticker. */
public final class GameCountdown {
  private final GameId gameId;
  private final int initialDuration;
  private final int minimumPlayers;
  private final Instant startedAt;
  private final boolean forced;
  private int remainingSeconds;
  private GameCountdownStatus status = GameCountdownStatus.RUNNING;
  private boolean accelerated;

  public GameCountdown(
      GameId gameId, int durationSeconds, int minimumPlayers, boolean forced, Instant startedAt) {
    this.gameId = Objects.requireNonNull(gameId, "gameId");
    if (durationSeconds < 1) throw new IllegalArgumentException("durationSeconds must be positive");
    if (minimumPlayers < 1) throw new IllegalArgumentException("minimumPlayers must be positive");
    this.initialDuration = durationSeconds;
    this.remainingSeconds = durationSeconds;
    this.minimumPlayers = minimumPlayers;
    this.forced = forced;
    this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
  }

  public synchronized boolean accelerateTo(int seconds) {
    if (status != GameCountdownStatus.RUNNING || seconds < 1 || remainingSeconds <= seconds)
      return false;
    remainingSeconds = seconds;
    accelerated = true;
    return true;
  }

  public synchronized CountdownSnapshot tick() {
    if (status != GameCountdownStatus.RUNNING) return snapshot();
    remainingSeconds--;
    if (remainingSeconds == 0) status = GameCountdownStatus.COMPLETED;
    return snapshot();
  }

  public synchronized CountdownSnapshot cancel() {
    if (status == GameCountdownStatus.RUNNING) status = GameCountdownStatus.CANCELLED;
    return snapshot();
  }

  public synchronized CountdownSnapshot snapshot() {
    return new CountdownSnapshot(
        gameId,
        initialDuration,
        remainingSeconds,
        status,
        startedAt,
        minimumPlayers,
        forced,
        accelerated);
  }
}
