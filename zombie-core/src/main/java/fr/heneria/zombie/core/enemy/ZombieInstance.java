package fr.heneria.zombie.core.enemy;

import fr.heneria.zombie.core.editor.MapPoint;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Mutable runtime entity owned by the engine; its definition is a spawn-time snapshot. */
public final class ZombieInstance {
  private final UUID id;
  private final UUID entityId;
  private final UUID gameId;
  private final int round;
  private final String spawnId;
  private final String worldName;
  private final ZombieDefinition definition;
  private final ZombieAttributeCalculator.CalculatedAttributes attributes;
  private final MapPoint spawnPoint;
  private final long spawnedAtTick;
  private final java.util.HashMap<String, Long> cooldowns = new java.util.HashMap<>();
  private ZombieState state = ZombieState.SPAWNING;
  private double health;
  private UUID targetPlayerId;
  private UUID lastAttackerId;
  private long lastAttackTick = Long.MIN_VALUE;
  private long lastTargetUpdateTick = Long.MIN_VALUE;
  private long lastMovementTick;
  private MapPoint lastPosition;
  private int repathAttempts;
  private ZombieRemovalReason removalReason;
  private boolean deathClaimed;

  public ZombieInstance(
      UUID id,
      UUID entityId,
      UUID gameId,
      int round,
      String spawnId,
      String worldName,
      ZombieDefinition definition,
      ZombieAttributeCalculator.CalculatedAttributes attributes,
      MapPoint spawnPoint,
      long spawnedAtTick) {
    this.id = java.util.Objects.requireNonNull(id, "id");
    this.entityId = java.util.Objects.requireNonNull(entityId, "entityId");
    this.gameId = java.util.Objects.requireNonNull(gameId, "gameId");
    if (round <= 0) {
      throw new IllegalArgumentException("round must be > 0");
    }
    this.round = round;
    this.spawnId = java.util.Objects.requireNonNull(spawnId, "spawnId");
    this.worldName = java.util.Objects.requireNonNull(worldName, "worldName");
    this.definition = java.util.Objects.requireNonNull(definition, "definition");
    this.attributes = java.util.Objects.requireNonNull(attributes, "attributes");
    this.spawnPoint = java.util.Objects.requireNonNull(spawnPoint, "spawnPoint");
    this.spawnedAtTick = spawnedAtTick;
    this.health = attributes.maximumHealth();
    this.lastPosition = spawnPoint;
    this.lastMovementTick = spawnedAtTick;
  }

  public synchronized boolean transition(ZombieState expected, ZombieState next) {
    if (state != expected || state.terminal()) {
      return false;
    }
    state = java.util.Objects.requireNonNull(next, "next");
    return true;
  }

  public synchronized void state(ZombieState next) {
    if (!state.terminal()) {
      state = java.util.Objects.requireNonNull(next, "next");
    }
  }

  public synchronized double applyDamage(double damage, UUID attackerId) {
    if (deathClaimed || state.terminal() || state == ZombieState.DYING || damage <= 0) {
      return 0;
    }
    double applied = Math.min(health, damage);
    health -= applied;
    lastAttackerId = attackerId;
    return applied;
  }

  public synchronized boolean claimDeath(ZombieRemovalReason reason) {
    if (deathClaimed || state.terminal()) {
      return false;
    }
    deathClaimed = true;
    removalReason = java.util.Objects.requireNonNull(reason, "reason");
    state = ZombieState.DYING;
    return true;
  }

  public synchronized void completeRemoval() {
    state =
        removalReason != null && removalReason.rewards() ? ZombieState.DEAD : ZombieState.DESPAWNED;
    targetPlayerId = null;
    cooldowns.clear();
  }

  public synchronized boolean canAttack(long tick) {
    return !state.terminal()
        && state != ZombieState.DYING
        && (lastAttackTick == Long.MIN_VALUE
            || tick - lastAttackTick >= definition.behavior().attackCooldownTicks());
  }

  public synchronized void attackedAt(long tick) {
    lastAttackTick = tick;
  }

  public synchronized void target(UUID playerId, long tick) {
    targetPlayerId = playerId;
    lastTargetUpdateTick = tick;
  }

  public synchronized void clearTarget(long tick) {
    targetPlayerId = null;
    lastTargetUpdateTick = tick;
  }

  public synchronized void observedPosition(MapPoint position, long tick) {
    double dx = position.x() - lastPosition.x();
    double dy = position.y() - lastPosition.y();
    double dz = position.z() - lastPosition.z();
    double minimum = definition.navigation().minimumMovementDistance();
    if (dx * dx + dy * dy + dz * dz >= minimum * minimum) {
      lastMovementTick = tick;
      repathAttempts = 0;
    }
    lastPosition = position;
  }

  public synchronized boolean stuck(long tick) {
    return definition.navigation().stuckDetection()
        && (state == ZombieState.MOVING || state == ZombieState.SEARCHING_TARGET)
        && tick - lastMovementTick >= definition.navigation().stuckTimeoutTicks();
  }

  public synchronized int incrementRepathAttempts() {
    return ++repathAttempts;
  }

  public synchronized boolean abilityReady(String id, long tick) {
    return cooldowns.getOrDefault(id, Long.MIN_VALUE) <= tick;
  }

  public synchronized void abilityCooldown(String id, long readyAtTick) {
    cooldowns.put(id, readyAtTick);
  }

  public synchronized Snapshot snapshot() {
    return new Snapshot(
        id,
        entityId,
        gameId,
        round,
        spawnId,
        worldName,
        definition.id(),
        state,
        health,
        attributes,
        Optional.ofNullable(targetPlayerId),
        Optional.ofNullable(lastAttackerId),
        lastAttackTick,
        lastTargetUpdateTick,
        lastMovementTick,
        lastPosition,
        spawnPoint,
        spawnedAtTick,
        Map.copyOf(cooldowns),
        Optional.ofNullable(removalReason));
  }

  public UUID id() {
    return id;
  }

  public UUID entityId() {
    return entityId;
  }

  public UUID gameId() {
    return gameId;
  }

  public int round() {
    return round;
  }

  public ZombieDefinition definition() {
    return definition;
  }

  public ZombieAttributeCalculator.CalculatedAttributes attributes() {
    return attributes;
  }

  public record Snapshot(
      UUID id,
      UUID entityId,
      UUID gameId,
      int round,
      String spawnId,
      String worldName,
      String typeId,
      ZombieState state,
      double health,
      ZombieAttributeCalculator.CalculatedAttributes attributes,
      Optional<UUID> targetPlayerId,
      Optional<UUID> lastAttackerId,
      long lastAttackTick,
      long lastTargetUpdateTick,
      long lastMovementTick,
      MapPoint lastPosition,
      MapPoint spawnPoint,
      long spawnedAtTick,
      Map<String, Long> cooldowns,
      Optional<ZombieRemovalReason> removalReason) {}
}
