package fr.heneria.bedwars.core.game;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Atomic runtime state for one two-block bed. */
public final class RuntimeBed {
  private final String teamId;
  private final RuntimeBlockPosition foot;
  private final RuntimeBlockPosition head;
  private boolean alive = true;
  private Instant destroyedAt;
  private UUID destroyedBy;

  public RuntimeBed(String teamId, RuntimeBlockPosition foot, RuntimeBlockPosition head) {
    this.teamId = Objects.requireNonNull(teamId, "teamId");
    this.foot = Objects.requireNonNull(foot, "foot");
    this.head = Objects.requireNonNull(head, "head");
    if (foot.equals(head)) throw new IllegalArgumentException("Bed halves must be distinct");
  }

  public String teamId() {
    return teamId;
  }

  public RuntimeBlockPosition foot() {
    return foot;
  }

  public RuntimeBlockPosition head() {
    return head;
  }

  public synchronized boolean alive() {
    return alive;
  }

  public synchronized boolean destroy(UUID playerId, Instant now) {
    if (!alive) return false;
    alive = false;
    destroyedBy = Objects.requireNonNull(playerId, "playerId");
    destroyedAt = Objects.requireNonNull(now, "now");
    return true;
  }

  public synchronized Optional<Instant> destroyedAt() {
    return Optional.ofNullable(destroyedAt);
  }

  public synchronized Optional<UUID> destroyedBy() {
    return Optional.ofNullable(destroyedBy);
  }
}
