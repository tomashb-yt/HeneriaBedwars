package fr.heneria.zombie.core.weapon;

import java.util.Optional;
import java.util.UUID;

/** Runtime-owned weapon with deterministic ammo, cooldown, reload and upgrade state. */
public final class WeaponInstance {
  private final UUID id;
  private final UUID gameId;
  private final UUID ownerId;
  private final WeaponDefinition definition;
  private int magazine;
  private int reserve;
  private int upgradeLevel;
  private long nextFireTick;
  private long reloadCompleteTick;
  private boolean reloading;
  private long shots;
  private long hits;
  private long headshots;
  private double damage;

  public WeaponInstance(
      UUID id, UUID gameId, UUID ownerId, WeaponDefinition definition, long currentTick) {
    this.id = java.util.Objects.requireNonNull(id, "id");
    this.gameId = java.util.Objects.requireNonNull(gameId, "gameId");
    this.ownerId = java.util.Objects.requireNonNull(ownerId, "ownerId");
    this.definition = java.util.Objects.requireNonNull(definition, "definition");
    this.magazine = definition.ammo().magazineSize();
    this.reserve = definition.ammo().startingReserve();
    this.nextFireTick = currentTick;
  }

  public synchronized FireDecision tryFire(long tick, boolean infiniteAmmo) {
    if (reloading || tick < nextFireTick) {
      return FireDecision.COOLDOWN;
    }
    if (magazine <= 0 && !definition.ammo().infinite() && !infiniteAmmo) {
      return FireDecision.EMPTY;
    }
    if (definition.fire().consumesAmmo() && !definition.ammo().infinite() && !infiniteAmmo) {
      magazine--;
    }
    nextFireTick = tick + definition.fire().cooldownTicks();
    shots++;
    return FireDecision.FIRED;
  }

  public synchronized FireDecision tryFollowUpShot(boolean infiniteAmmo) {
    if (reloading) {
      return FireDecision.COOLDOWN;
    }
    if (magazine <= 0 && !definition.ammo().infinite() && !infiniteAmmo) {
      return FireDecision.EMPTY;
    }
    if (definition.fire().consumesAmmo() && !definition.ammo().infinite() && !infiniteAmmo) {
      magazine--;
    }
    shots++;
    return FireDecision.FIRED;
  }

  public synchronized boolean beginReload(long tick) {
    if (reloading
        || magazine >= magazineCapacity()
        || (reserve <= 0 && !definition.ammo().infinite())) {
      return false;
    }
    reloading = true;
    reloadCompleteTick = tick + definition.reload().durationTicks();
    return true;
  }

  public synchronized boolean completeReload(long tick) {
    if (!reloading || tick < reloadCompleteTick) {
      return false;
    }
    int missing = magazineCapacity() - magazine;
    int transferred = definition.ammo().infinite() ? missing : Math.min(missing, reserve);
    magazine += transferred;
    reserve -= definition.ammo().infinite() ? 0 : transferred;
    reloading = false;
    reloadCompleteTick = 0;
    return true;
  }

  public synchronized boolean interruptReload() {
    if (!reloading || !definition.reload().interruptible()) {
      return false;
    }
    reloading = false;
    reloadCompleteTick = 0;
    return true;
  }

  public synchronized int refillReserve() {
    int previous = reserve;
    reserve = definition.ammo().maximumReserve();
    return reserve - previous;
  }

  public synchronized boolean upgrade() {
    if (upgradeLevel >= definition.upgrades().size()) {
      return false;
    }
    upgradeLevel++;
    magazine = Math.min(magazineCapacity(), magazine + currentUpgrade().magazineBonus());
    return true;
  }

  public synchronized void recordHit(double appliedDamage, boolean headshot) {
    if (appliedDamage <= 0) {
      return;
    }
    hits++;
    damage += appliedDamage;
    if (headshot) {
      headshots++;
    }
  }

  public synchronized double damageMultiplier() {
    return upgradeLevel == 0 ? 1 : currentUpgrade().damageMultiplier();
  }

  public synchronized int nextUpgradeCost() {
    return upgradeLevel >= definition.upgrades().size()
        ? -1
        : definition.upgrades().get(upgradeLevel).cost();
  }

  public synchronized int magazineCapacity() {
    return definition.ammo().magazineSize()
        + (upgradeLevel == 0 ? 0 : currentUpgrade().magazineBonus());
  }

  private WeaponDefinition.Upgrade currentUpgrade() {
    return definition.upgrades().get(upgradeLevel - 1);
  }

  public synchronized Snapshot snapshot() {
    return new Snapshot(
        id,
        gameId,
        ownerId,
        definition.id(),
        magazine,
        reserve,
        upgradeLevel,
        nextFireTick,
        reloading ? Optional.of(reloadCompleteTick) : Optional.empty(),
        shots,
        hits,
        headshots,
        damage);
  }

  public UUID id() {
    return id;
  }

  public UUID gameId() {
    return gameId;
  }

  public UUID ownerId() {
    return ownerId;
  }

  public WeaponDefinition definition() {
    return definition;
  }

  public enum FireDecision {
    FIRED,
    EMPTY,
    COOLDOWN
  }

  public record Snapshot(
      UUID id,
      UUID gameId,
      UUID ownerId,
      String weaponId,
      int magazine,
      int reserve,
      int upgradeLevel,
      long nextFireTick,
      Optional<Long> reloadCompleteTick,
      long shots,
      long hits,
      long headshots,
      double damage) {}
}
