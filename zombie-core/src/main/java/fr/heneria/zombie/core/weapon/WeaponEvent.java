package fr.heneria.zombie.core.weapon;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Internal synchronous weapon lifecycle event; PRE events may be cancelled. */
public final class WeaponEvent {
  private final Type type;
  private final UUID gameId;
  private final UUID playerId;
  private final UUID weaponInstanceId;
  private final String weaponId;
  private final Instant occurredAt;
  private final Map<String, String> data;
  private boolean cancelled;

  public WeaponEvent(
      Type type,
      UUID gameId,
      UUID playerId,
      UUID weaponInstanceId,
      String weaponId,
      Instant occurredAt,
      Map<String, String> data) {
    this.type = java.util.Objects.requireNonNull(type, "type");
    this.gameId = java.util.Objects.requireNonNull(gameId, "gameId");
    this.playerId = java.util.Objects.requireNonNull(playerId, "playerId");
    this.weaponInstanceId = java.util.Objects.requireNonNull(weaponInstanceId, "weaponInstanceId");
    this.weaponId = java.util.Objects.requireNonNull(weaponId, "weaponId");
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

  public UUID playerId() {
    return playerId;
  }

  public UUID weaponInstanceId() {
    return weaponInstanceId;
  }

  public String weaponId() {
    return weaponId;
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
    PRE_FIRE(true),
    FIRED(false),
    PRE_RELOAD(true),
    RELOAD_STARTED(false),
    RELOAD_COMPLETED(false),
    PRE_UPGRADE(true),
    UPGRADED(false),
    PURCHASED(false),
    DAMAGE(false);

    private final boolean cancellable;

    Type(boolean cancellable) {
      this.cancellable = cancellable;
    }

    public boolean cancellable() {
      return cancellable;
    }
  }
}
