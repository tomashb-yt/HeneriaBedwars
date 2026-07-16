package fr.heneria.bedwars.core.map;

import java.util.Locale;
import java.util.Set;

/** Per-template construction-world behavior persisted independently from global defaults. */
public record MapWorldSettings(
    boolean autoSave,
    boolean allowAnimals,
    boolean allowMonsters,
    long fixedTime,
    boolean clearWeather,
    boolean daylightCycle,
    boolean weatherCycle,
    String difficulty,
    boolean pvp,
    boolean fireTick,
    boolean environmentalDamage) {
  private static final Set<String> DIFFICULTIES = Set.of("PEACEFUL", "EASY", "NORMAL", "HARD");

  public MapWorldSettings {
    if (fixedTime < 0 || fixedTime > 24_000)
      throw new IllegalArgumentException("World time must be between 0 and 24000");
    difficulty = difficulty == null ? "PEACEFUL" : difficulty.toUpperCase(Locale.ROOT);
    if (!DIFFICULTIES.contains(difficulty))
      throw new IllegalArgumentException("Unsupported world difficulty");
  }

  /** Compatibility constructor for Ticket 007 metadata and tests. */
  public MapWorldSettings(
      boolean autoSave,
      boolean allowAnimals,
      boolean allowMonsters,
      long fixedTime,
      boolean clearWeather) {
    this(
        autoSave,
        allowAnimals,
        allowMonsters,
        fixedTime,
        clearWeather,
        false,
        false,
        "PEACEFUL",
        false,
        false,
        false);
  }

  public MapWorldSettings withAutoSave(boolean value) {
    return copy(
        value,
        allowAnimals,
        allowMonsters,
        fixedTime,
        clearWeather,
        daylightCycle,
        weatherCycle,
        difficulty,
        pvp,
        fireTick,
        environmentalDamage);
  }

  public MapWorldSettings withCreatures(boolean value) {
    return copy(
        autoSave,
        value,
        value,
        fixedTime,
        clearWeather,
        daylightCycle,
        weatherCycle,
        difficulty,
        pvp,
        fireTick,
        environmentalDamage);
  }

  public MapWorldSettings withTime(long value) {
    return copy(
        autoSave,
        allowAnimals,
        allowMonsters,
        value,
        clearWeather,
        daylightCycle,
        weatherCycle,
        difficulty,
        pvp,
        fireTick,
        environmentalDamage);
  }

  public MapWorldSettings withClearWeather(boolean value) {
    return copy(
        autoSave,
        allowAnimals,
        allowMonsters,
        fixedTime,
        value,
        daylightCycle,
        weatherCycle,
        difficulty,
        pvp,
        fireTick,
        environmentalDamage);
  }

  public MapWorldSettings withDaylightCycle(boolean value) {
    return copy(
        autoSave,
        allowAnimals,
        allowMonsters,
        fixedTime,
        clearWeather,
        value,
        weatherCycle,
        difficulty,
        pvp,
        fireTick,
        environmentalDamage);
  }

  public MapWorldSettings withWeatherCycle(boolean value) {
    return copy(
        autoSave,
        allowAnimals,
        allowMonsters,
        fixedTime,
        clearWeather,
        daylightCycle,
        value,
        difficulty,
        pvp,
        fireTick,
        environmentalDamage);
  }

  public MapWorldSettings withDifficulty(String value) {
    return copy(
        autoSave,
        allowAnimals,
        allowMonsters,
        fixedTime,
        clearWeather,
        daylightCycle,
        weatherCycle,
        value,
        pvp,
        fireTick,
        environmentalDamage);
  }

  public MapWorldSettings withPvp(boolean value) {
    return copy(
        autoSave,
        allowAnimals,
        allowMonsters,
        fixedTime,
        clearWeather,
        daylightCycle,
        weatherCycle,
        difficulty,
        value,
        fireTick,
        environmentalDamage);
  }

  public MapWorldSettings withFireTick(boolean value) {
    return copy(
        autoSave,
        allowAnimals,
        allowMonsters,
        fixedTime,
        clearWeather,
        daylightCycle,
        weatherCycle,
        difficulty,
        pvp,
        value,
        environmentalDamage);
  }

  public MapWorldSettings withEnvironmentalDamage(boolean value) {
    return copy(
        autoSave,
        allowAnimals,
        allowMonsters,
        fixedTime,
        clearWeather,
        daylightCycle,
        weatherCycle,
        difficulty,
        pvp,
        fireTick,
        value);
  }

  private static MapWorldSettings copy(
      boolean autoSave,
      boolean allowAnimals,
      boolean allowMonsters,
      long fixedTime,
      boolean clearWeather,
      boolean daylightCycle,
      boolean weatherCycle,
      String difficulty,
      boolean pvp,
      boolean fireTick,
      boolean environmentalDamage) {
    return new MapWorldSettings(
        autoSave,
        allowAnimals,
        allowMonsters,
        fixedTime,
        clearWeather,
        daylightCycle,
        weatherCycle,
        difficulty,
        pvp,
        fireTick,
        environmentalDamage);
  }
}
