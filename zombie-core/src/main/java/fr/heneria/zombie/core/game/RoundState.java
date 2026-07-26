package fr.heneria.zombie.core.game;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Synchronized counters and invariants for one round. */
public final class RoundState {
  private final int number;
  private final int totalEnemies;
  private final Instant startedAt;
  private RoundPhase phase = RoundPhase.PREPARING;
  private int spawned;
  private int alive;
  private int defeated;
  private int pendingSpawns;

  public RoundState(int number, int totalEnemies, Instant startedAt) {
    if (number <= 0 || totalEnemies <= 0) {
      throw new IllegalArgumentException("Invalid round");
    }
    this.number = number;
    this.totalEnemies = totalEnemies;
    this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
  }

  public synchronized void beginSpawning() {
    if (phase != RoundPhase.PREPARING) {
      throw new IllegalStateException("Round already started");
    }
    phase = RoundPhase.SPAWNING;
  }

  public synchronized int reserveSpawns(int requested, int maximumAlive) {
    if (phase != RoundPhase.SPAWNING && phase != RoundPhase.ACTIVE) {
      return 0;
    }
    int amount =
        Math.min(
            requested,
            Math.min(totalEnemies - spawned - pendingSpawns, maximumAlive - alive - pendingSpawns));
    pendingSpawns += Math.max(0, amount);
    return Math.max(0, amount);
  }

  public synchronized void spawned() {
    if (pendingSpawns <= 0 || spawned >= totalEnemies) {
      throw new IllegalStateException("Spawn was not reserved");
    }
    pendingSpawns--;
    spawned++;
    alive++;
    phase = spawned == totalEnemies ? RoundPhase.ACTIVE : RoundPhase.SPAWNING;
  }

  public synchronized void spawnFailed() {
    if (pendingSpawns <= 0) {
      throw new IllegalStateException("Spawn was not reserved");
    }
    pendingSpawns--;
  }

  public synchronized boolean defeated() {
    if (alive <= 0) {
      return false;
    }
    alive--;
    defeated++;
    return true;
  }

  public synchronized boolean canComplete() {
    return spawned == totalEnemies && alive == 0 && pendingSpawns == 0;
  }

  public synchronized boolean complete() {
    if (phase == RoundPhase.COMPLETED) {
      return false;
    }
    if (!canComplete()) {
      throw new IllegalStateException("Round still has enemies");
    }
    phase = RoundPhase.COMPLETED;
    return true;
  }

  public synchronized void cancel() {
    phase = RoundPhase.CANCELLED;
    pendingSpawns = 0;
  }

  public synchronized Snapshot snapshot(Instant now) {
    return new Snapshot(
        number,
        phase,
        totalEnemies,
        spawned,
        totalEnemies - spawned - pendingSpawns,
        alive,
        defeated,
        pendingSpawns,
        Duration.between(startedAt, now));
  }

  public record Snapshot(
      int number,
      RoundPhase phase,
      int totalEnemies,
      int spawnedEnemies,
      int waitingEnemies,
      int aliveEnemies,
      int defeatedEnemies,
      int pendingSpawns,
      Duration duration) {}
}
