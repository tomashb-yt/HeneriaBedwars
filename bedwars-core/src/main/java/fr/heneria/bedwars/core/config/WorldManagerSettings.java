package fr.heneria.bedwars.core.config;

import java.util.Set;

/** Validated autonomous world-manager configuration. Directory values remain relative and safe. */
public record WorldManagerSettings(
    boolean enabled,
    String templatesDirectory,
    String metadataDirectory,
    String instancesDirectory,
    String backupsDirectory,
    String templateWorldPrefix,
    String instanceWorldPrefix,
    String fallbackWorld,
    boolean createSafetyPlatform,
    String platformMaterial,
    int platformRadius,
    int platformY,
    String environment,
    String difficulty,
    long fixedTime,
    boolean clearWeather,
    boolean pvp,
    boolean animals,
    boolean monsters,
    boolean autoSave,
    int autoSaveIntervalMinutes,
    boolean saveBeforeUnload,
    boolean refusePlayersOnUnload,
    Set<String> excludedFiles,
    Set<String> excludedDirectories) {
  public WorldManagerSettings {
    excludedFiles = Set.copyOf(excludedFiles);
    excludedDirectories = Set.copyOf(excludedDirectories);
  }
}
