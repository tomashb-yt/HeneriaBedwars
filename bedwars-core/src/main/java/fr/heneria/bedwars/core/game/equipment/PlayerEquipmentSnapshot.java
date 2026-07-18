package fr.heneria.bedwars.core.game.equipment;

/** Immutable view used by platform adapters when rebuilding a player's loadout. */
public record PlayerEquipmentSnapshot(
    int armorTier, int pickaxeTier, int axeTier, boolean shears) {}
