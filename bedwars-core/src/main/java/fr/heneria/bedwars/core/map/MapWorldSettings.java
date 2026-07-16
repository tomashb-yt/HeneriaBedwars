package fr.heneria.bedwars.core.map;

/** Per-template world behavior persisted independently from global defaults. */
public record MapWorldSettings(
    boolean autoSave,
    boolean allowAnimals,
    boolean allowMonsters,
    long fixedTime,
    boolean clearWeather) {}
