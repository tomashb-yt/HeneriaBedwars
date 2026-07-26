package fr.heneria.zombie.core.enemy;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** Immutable, platform-neutral snapshot of one configurable enemy type. */
public record ZombieDefinition(
    String id,
    String displayName,
    String entityType,
    ZombieCategory category,
    Attributes attributes,
    Behavior behavior,
    Navigation navigation,
    DamageProfile damage,
    SpawnRules spawnRules,
    Rewards rewards,
    List<String> abilities,
    Environment environment,
    Appearance appearance) {

  private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9_]{1,64}");

  public ZombieDefinition {
    if (id == null || !ID_PATTERN.matcher(id).matches()) {
      throw new IllegalArgumentException("id must match [a-z0-9_]{1,64}");
    }
    displayName = Objects.requireNonNull(displayName, "displayName");
    entityType =
        Objects.requireNonNull(entityType, "entityType").toUpperCase(java.util.Locale.ROOT);
    category = Objects.requireNonNull(category, "category");
    attributes = Objects.requireNonNull(attributes, "attributes");
    behavior = Objects.requireNonNull(behavior, "behavior");
    navigation = Objects.requireNonNull(navigation, "navigation");
    damage = Objects.requireNonNull(damage, "damage");
    spawnRules = Objects.requireNonNull(spawnRules, "spawnRules");
    rewards = Objects.requireNonNull(rewards, "rewards");
    abilities = List.copyOf(abilities);
    environment = Objects.requireNonNull(environment, "environment");
    appearance = Objects.requireNonNull(appearance, "appearance");
  }

  /** Validates values that do not depend on the platform entity registry. */
  public List<String> validate() {
    java.util.ArrayList<String> issues = new java.util.ArrayList<>();
    if (!Double.isFinite(attributes.healthBase()) || attributes.healthBase() <= 0) {
      issues.add("attributes.health.base must be finite and > 0");
    }
    if (!Double.isFinite(attributes.damageBase()) || attributes.damageBase() < 0) {
      issues.add("attributes.damage.base must be finite and >= 0");
    }
    if (!Double.isFinite(attributes.speedBase()) || attributes.speedBase() <= 0) {
      issues.add("attributes.speed.base must be finite and > 0");
    }
    if (spawnRules.minimumRound() <= 0 || spawnRules.maximumRound() < spawnRules.minimumRound()) {
      issues.add("spawn-rules round range is invalid");
    }
    if (spawnRules.weight() <= 0 || spawnRules.maximumAlive() == 0) {
      issues.add("spawn-rules weight must be > 0 and maximum-alive cannot be 0");
    }
    damage
        .multipliers()
        .forEach(
            (type, value) -> {
              if (!Double.isFinite(value) || value < 0) {
                issues.add("damage multiplier for " + type + " must be finite and >= 0");
              }
            });
    return List.copyOf(issues);
  }

  /** Base and round-scaled combat attributes. */
  public record Attributes(
      double healthBase,
      boolean scaleHealth,
      double healthRoundMultiplier,
      double maximumHealth,
      double damageBase,
      boolean scaleDamage,
      double damageRoundMultiplier,
      double speedBase,
      double followRange,
      double knockbackResistance,
      double attackKnockback) {}

  /** High-level behavior settings; Bukkit navigation remains an adapter concern. */
  public record Behavior(
      BehaviorType type,
      TargetStrategy targetStrategy,
      double attackRange,
      int attackCooldownTicks,
      int targetUpdateIntervalTicks,
      double loseTargetDistance) {
    public Behavior {
      Objects.requireNonNull(type, "type");
      Objects.requireNonNull(targetStrategy, "targetStrategy");
      if (attackRange <= 0
          || attackCooldownTicks <= 0
          || targetUpdateIntervalTicks <= 0
          || loseTargetDistance <= 0) {
        throw new IllegalArgumentException("behavior numeric values must be > 0");
      }
    }
  }

  /** Physical movement and stuck-recovery policy. */
  public record Navigation(
      NavigationMode mode,
      boolean avoidWater,
      boolean canOpenDoors,
      boolean canBreakDoors,
      boolean stuckDetection,
      int stuckCheckIntervalTicks,
      double minimumMovementDistance,
      int stuckTimeoutTicks,
      int maximumRepathAttempts,
      StuckAction fallbackAction) {
    public Navigation {
      Objects.requireNonNull(mode, "mode");
      Objects.requireNonNull(fallbackAction, "fallbackAction");
    }
  }

  /** Domain damage modifiers, independent from Bukkit damage causes. */
  public record DamageProfile(
      Set<ZombieDamageType> immunities,
      Map<ZombieDamageType, Double> multipliers,
      boolean headshotsEnabled,
      double headshotMultiplier) {
    public DamageProfile {
      immunities = Set.copyOf(immunities);
      multipliers = Map.copyOf(multipliers);
      if (!Double.isFinite(headshotMultiplier) || headshotMultiplier < 0) {
        throw new IllegalArgumentException("headshotMultiplier must be finite and >= 0");
      }
    }
  }

  /** Conditions used by deterministic type selection. */
  public record SpawnRules(
      int minimumRound,
      int maximumRound,
      int weight,
      int maximumAlive,
      Set<String> zones,
      Set<String> pools) {
    public SpawnRules {
      zones = Set.copyOf(zones);
      pools = Set.copyOf(pools);
    }
  }

  /** Rewards emitted once by the death service. */
  public record Rewards(
      int pointsOnHit, int pointsOnKill, int pointsOnHeadshotKill, int experience) {
    public Rewards {
      if (pointsOnHit < 0 || pointsOnKill < 0 || pointsOnHeadshotKill < 0 || experience < 0) {
        throw new IllegalArgumentException("rewards cannot be negative");
      }
    }
  }

  /** Natural-environment protections. */
  public record Environment(boolean burnInDaylight, boolean removeWhenFarAway) {}

  /** Presentation data applied only by the platform factory. */
  public record Appearance(
      boolean customNameVisible,
      boolean glowing,
      boolean baby,
      boolean silent,
      Map<String, String> equipment) {
    public Appearance {
      equipment = Map.copyOf(equipment);
    }
  }

  public enum ZombieCategory {
    NORMAL,
    FAST,
    HEAVY,
    RANGED,
    FLYING,
    SPECIAL,
    ELITE,
    MINI_BOSS,
    BOSS
  }

  public enum BehaviorType {
    MELEE,
    RANGED,
    PASSIVE_UNTIL_DAMAGED,
    BARRICADE_ATTACKER,
    CUSTOM
  }

  public enum TargetStrategy {
    NEAREST_VALID_PLAYER,
    LOWEST_HEALTH,
    HIGHEST_POINTS,
    RANDOM_VALID_PLAYER,
    LAST_ATTACKER,
    FIXED,
    CUSTOM
  }

  public enum NavigationMode {
    GROUND,
    FLYING,
    SWIMMING,
    STATIC,
    TELEPORTING,
    CUSTOM
  }

  public enum StuckAction {
    REPATH,
    CHANGE_TARGET,
    TELEPORT_TO_VALID_POSITION,
    TELEPORT_TO_VALID_SPAWN,
    DESPAWN_AND_REQUEUE,
    KILL_WITHOUT_REWARD,
    CUSTOM
  }
}
