package fr.heneria.zombie.core.weapon;

import fr.heneria.zombie.core.enemy.ZombieDamageType;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** Immutable, platform-neutral snapshot of one configurable weapon type. */
public record WeaponDefinition(
    String id,
    String displayName,
    WeaponCategory category,
    WeaponRarity rarity,
    Presentation presentation,
    Fire fire,
    Damage damage,
    Ammo ammo,
    Reload reload,
    Spread spread,
    Recoil recoil,
    Penetration penetration,
    Economy economy,
    int mysteryWeight,
    List<Upgrade> upgrades,
    Map<String, String> sounds,
    Set<String> effects) {
  private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9_]{1,64}");

  public WeaponDefinition {
    if (id == null || !ID_PATTERN.matcher(id).matches()) {
      throw new IllegalArgumentException("id must match [a-z0-9_]{1,64}");
    }
    displayName = Objects.requireNonNull(displayName, "displayName");
    category = Objects.requireNonNull(category, "category");
    rarity = Objects.requireNonNull(rarity, "rarity");
    presentation = Objects.requireNonNull(presentation, "presentation");
    fire = Objects.requireNonNull(fire, "fire");
    damage = Objects.requireNonNull(damage, "damage");
    ammo = Objects.requireNonNull(ammo, "ammo");
    reload = Objects.requireNonNull(reload, "reload");
    spread = Objects.requireNonNull(spread, "spread");
    recoil = Objects.requireNonNull(recoil, "recoil");
    penetration = Objects.requireNonNull(penetration, "penetration");
    economy = Objects.requireNonNull(economy, "economy");
    upgrades = List.copyOf(upgrades);
    sounds = Map.copyOf(sounds);
    effects = Set.copyOf(effects);
  }

  /** Returns all platform-neutral validation diagnostics. */
  public List<String> validate() {
    java.util.ArrayList<String> issues = new java.util.ArrayList<>();
    if (!finitePositive(damage.baseDamage())) {
      issues.add("damage.base must be finite and > 0");
    }
    if (!finitePositive(damage.maximumDistance())
        || damage.minimumDistance() < 0
        || damage.maximumDistance() < damage.minimumDistance()) {
      issues.add("damage distance range is invalid");
    }
    if (!finitePositive(damage.minimumMultiplier())
        || !finitePositive(damage.headshotMultiplier())) {
      issues.add("damage multipliers must be finite and > 0");
    }
    if (fire.cooldownTicks() <= 0
        || fire.pellets() <= 0
        || fire.burstSize() <= 0
        || fire.chargeTicks() < 0) {
      issues.add("fire values are invalid");
    }
    if (ammo.magazineSize() <= 0
        || ammo.maximumReserve() < 0
        || ammo.startingReserve() < 0
        || ammo.startingReserve() > ammo.maximumReserve()) {
      issues.add("ammo values are invalid");
    }
    if (reload.durationTicks() <= 0) {
      issues.add("reload.duration-ticks must be > 0");
    }
    if (penetration.maximumTargets() <= 0
        || penetration.damageRetention() <= 0
        || penetration.damageRetention() > 1) {
      issues.add("penetration values are invalid");
    }
    if (economy.wallCost() < 0
        || economy.ammoCost() < 0
        || economy.mysteryCost() < 0
        || mysteryWeight < 0) {
      issues.add("economy values cannot be negative");
    }
    for (int index = 0; index < upgrades.size(); index++) {
      Upgrade upgrade = upgrades.get(index);
      if (upgrade.level() != index + 1
          || upgrade.cost() < 0
          || !finitePositive(upgrade.damageMultiplier())
          || upgrade.magazineBonus() < 0) {
        issues.add("upgrade levels must be contiguous and valid");
        break;
      }
    }
    return List.copyOf(issues);
  }

  private static boolean finitePositive(double value) {
    return Double.isFinite(value) && value > 0;
  }

  public enum WeaponCategory {
    PISTOL,
    REVOLVER,
    ASSAULT_RIFLE,
    SMG,
    SHOTGUN,
    SNIPER_RIFLE,
    LMG,
    ROCKET_LAUNCHER,
    MELEE,
    WONDER_WEAPON
  }

  public enum WeaponRarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY,
    WONDER
  }

  public enum FireMode {
    SEMI_AUTOMATIC,
    AUTOMATIC,
    BURST,
    CHARGE,
    MELEE
  }

  public record Presentation(String material, int customModelData, String upgradedMaterial) {
    public Presentation {
      material = Objects.requireNonNull(material, "material");
      upgradedMaterial = Objects.requireNonNull(upgradedMaterial, "upgradedMaterial");
      if (customModelData < 0) {
        throw new IllegalArgumentException("customModelData cannot be negative");
      }
    }
  }

  public record Fire(
      FireMode mode,
      int cooldownTicks,
      int burstSize,
      int burstIntervalTicks,
      int chargeTicks,
      int pellets,
      boolean consumesAmmo) {
    public Fire {
      Objects.requireNonNull(mode, "mode");
    }
  }

  public record Damage(
      double baseDamage,
      double minimumDistance,
      double maximumDistance,
      double minimumMultiplier,
      double headshotMultiplier,
      ZombieDamageType damageType) {
    public Damage {
      Objects.requireNonNull(damageType, "damageType");
    }
  }

  public record Ammo(
      int magazineSize,
      int startingReserve,
      int maximumReserve,
      boolean infinite,
      int overloadCapacity) {}

  public record Reload(int durationTicks, boolean interruptible) {}

  public record Spread(
      double standing, double walking, double sprinting, double airborne, double aiming) {}

  public record Recoil(double vertical, double horizontal, int recoveryTicks) {}

  public record Penetration(
      int maximumTargets, double damageRetention, Set<String> penetrableMaterials) {
    public Penetration {
      penetrableMaterials = Set.copyOf(penetrableMaterials);
    }
  }

  public record Economy(int wallCost, int ammoCost, int mysteryCost) {}

  public record Upgrade(
      int level,
      String displayName,
      int cost,
      double damageMultiplier,
      int magazineBonus,
      int customModelData,
      Set<String> effects) {
    public Upgrade {
      displayName = Objects.requireNonNull(displayName, "displayName");
      effects = Set.copyOf(effects);
    }
  }
}
