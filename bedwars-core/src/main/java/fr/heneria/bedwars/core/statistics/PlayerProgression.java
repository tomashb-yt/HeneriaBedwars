package fr.heneria.bedwars.core.statistics;

/** Derived progression view; no mutable reward state is persisted. */
public record PlayerProgression(
    int level, long totalExperience, long currentLevelExperience, long requiredLevelExperience) {
  public PlayerProgression {
    if (level < 1
        || totalExperience < 0
        || currentLevelExperience < 0
        || requiredLevelExperience < 1
        || currentLevelExperience >= requiredLevelExperience)
      throw new IllegalArgumentException("invalid progression");
  }

  public double progressPercent() {
    return currentLevelExperience * 100.0 / requiredLevelExperience;
  }
}
