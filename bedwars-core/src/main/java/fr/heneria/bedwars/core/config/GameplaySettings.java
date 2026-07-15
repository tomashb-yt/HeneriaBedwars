package fr.heneria.bedwars.core.config;

/** Immutable subset of future gameplay settings. */
public record GameplaySettings(
    String combatProfile,
    boolean attackCooldownEnabled,
    boolean shieldsEnabled,
    boolean friendlyFire,
    boolean respawnEnabled,
    int respawnDelaySeconds,
    int respawnProtectionSeconds,
    int voidMinimumY,
    boolean breakOnlyPlayerPlaced,
    boolean restoreAfterGame) {}
