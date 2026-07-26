package fr.heneria.zombie.core.enemy;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Internal enemy lifecycle event.
 *
 * <p>Only PRE_* events are cancellable. Publication is synchronous on the engine-owning thread.
 */
public final class ZombieEvent {
  private final Type type;
  private final UUID gameId;
  private final UUID zombieId;
  private final String typeId;
  private final int round;
  private final Instant occurredAt;
  private final Map<String, String> data;
  private boolean cancelled;

  public ZombieEvent(
      Type type,
      UUID gameId,
      UUID zombieId,
      String typeId,
      int round,
      Instant occurredAt,
      Map<String, String> data) {
    this.type = java.util.Objects.requireNonNull(type, "type");
    this.gameId = java.util.Objects.requireNonNull(gameId, "gameId");
    this.zombieId = zombieId;
    this.typeId = java.util.Objects.requireNonNull(typeId, "typeId");
    this.round = round;
    this.occurredAt = java.util.Objects.requireNonNull(occurredAt, "occurredAt");
    this.data = Map.copyOf(data);
  }

  public void cancel() {
    if (!type.cancellable()) {
      throw new IllegalStateException(type + " is not cancellable");
    }
    cancelled = true;
  }

  public Type type() {
    return type;
  }

  public UUID gameId() {
    return gameId;
  }

  public Optional<UUID> zombieId() {
    return Optional.ofNullable(zombieId);
  }

  public String typeId() {
    return typeId;
  }

  public int round() {
    return round;
  }

  public Instant occurredAt() {
    return occurredAt;
  }

  public Map<String, String> data() {
    return data;
  }

  public boolean cancelled() {
    return cancelled;
  }

  public enum Type {
    PRE_SPAWN(true),
    SPAWNED(false),
    TARGET_SELECTED(false),
    TARGET_LOST(false),
    PRE_ATTACK(true),
    ATTACKED(false),
    PRE_DAMAGE(true),
    DAMAGED(false),
    ABILITY_PRE_ACTIVATE(true),
    ABILITY_ACTIVATED(false),
    PRE_DEATH(true),
    DEATH(false),
    REMOVED(false),
    STUCK(false);

    private final boolean cancellable;

    Type(boolean cancellable) {
      this.cancellable = cancellable;
    }

    public boolean cancellable() {
      return cancellable;
    }
  }
}
