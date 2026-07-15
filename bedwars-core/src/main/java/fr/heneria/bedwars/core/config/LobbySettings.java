package fr.heneria.bedwars.core.config;

/** Immutable future lobby settings. */
public record LobbySettings(
    boolean configured,
    String world,
    double x,
    double y,
    double z,
    float yaw,
    float pitch,
    boolean cancelBlockBreak,
    boolean cancelBlockPlace,
    boolean cancelDamage,
    boolean cancelHunger,
    boolean itemsEnabled) {}
