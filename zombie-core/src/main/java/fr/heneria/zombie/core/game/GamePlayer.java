package fr.heneria.zombie.core.game;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Mutable player statistics owned exclusively by one game aggregate. */
public final class GamePlayer {
  private final UUID playerId;
  private GamePlayerState state = GamePlayerState.WAITING;
  private int points;
  private int kills;
  private int headshots;
  private long damage;
  private int revives;
  private int downs;
  private int maximumRound;
  private Instant disconnectedUntil;
  private GamePlayerState stateBeforeDisconnect = GamePlayerState.ALIVE;

  public GamePlayer(UUID playerId, int startingPoints) {
    this.playerId = Objects.requireNonNull(playerId, "playerId");
    this.points = startingPoints;
  }

  public void state(GamePlayerState target) {
    state = Objects.requireNonNull(target, "target");
    if (target != GamePlayerState.DISCONNECTED) {
      disconnectedUntil = null;
    }
  }

  public void disconnectedUntil(Instant value) {
    if (state != GamePlayerState.DISCONNECTED) {
      stateBeforeDisconnect = state;
    }
    state = GamePlayerState.DISCONNECTED;
    disconnectedUntil = Objects.requireNonNull(value, "value");
  }

  public boolean reconnect(Instant now) {
    if (state != GamePlayerState.DISCONNECTED
        || disconnectedUntil == null
        || now.isAfter(disconnectedUntil)) {
      return false;
    }
    state(stateBeforeDisconnect);
    return true;
  }

  public boolean expireDisconnect(Instant now) {
    if (state != GamePlayerState.DISCONNECTED
        || disconnectedUntil == null
        || now.isBefore(disconnectedUntil)) {
      return false;
    }
    state(GamePlayerState.DEAD);
    return true;
  }

  public void killed(int reward) {
    kills++;
    points = Math.addExact(points, reward);
  }

  public void downed() {
    downs++;
    state(GamePlayerState.DOWNED);
  }

  public void revived() {
    revives++;
    state(GamePlayerState.ALIVE);
  }

  public void maximumRound(int value) {
    maximumRound = Math.max(maximumRound, value);
  }

  public Snapshot snapshot() {
    return new Snapshot(
        playerId,
        state,
        points,
        kills,
        headshots,
        damage,
        revives,
        downs,
        maximumRound,
        Optional.ofNullable(disconnectedUntil));
  }

  public record Snapshot(
      UUID playerId,
      GamePlayerState state,
      int points,
      int kills,
      int headshots,
      long damage,
      int revives,
      int downs,
      int maximumRound,
      Optional<Instant> disconnectedUntil) {}
}
