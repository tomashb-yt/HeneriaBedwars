package fr.heneria.bedwars.core.config;

/** Immutable gameplay rules captured by Bukkit adapters at the point of use. */
public record GameplaySettings(
    String combatProfile,
    boolean attackCooldownEnabled,
    boolean shieldsEnabled,
    boolean friendlyFire,
    int hitInvulnerabilityTicks,
    int killCreditSeconds,
    double knockbackHorizontal,
    double knockbackVertical,
    double sprintKnockbackMultiplier,
    double projectileKnockbackMultiplier,
    boolean respawnEnabled,
    int respawnDelaySeconds,
    int respawnProtectionSeconds,
    int voidMinimumY,
    boolean breakOnlyPlayerPlaced,
    boolean restoreAfterGame) {}
