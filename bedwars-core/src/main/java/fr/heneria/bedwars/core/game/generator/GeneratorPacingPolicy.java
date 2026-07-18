package fr.heneria.bedwars.core.game.generator;

import java.time.Duration;
import java.util.Objects;

/** Bounded population-aware interval policy captured when a match starts. */
public final class GeneratorPacingPolicy {
  private final boolean enabled;
  private final double minimumFactor;
  private final double maximumFactor;

  public GeneratorPacingPolicy(boolean enabled, double minimumFactor, double maximumFactor) {
    if (!Double.isFinite(minimumFactor) || minimumFactor <= 0)
      throw new IllegalArgumentException("Minimum generator pace factor must be positive");
    if (!Double.isFinite(maximumFactor) || maximumFactor < minimumFactor)
      throw new IllegalArgumentException("Maximum generator pace factor is invalid");
    this.enabled = enabled;
    this.minimumFactor = minimumFactor;
    this.maximumFactor = maximumFactor;
  }

  public GeneratorDefinition adjust(
      GeneratorDefinition definition, int configuredTeams, int players) {
    Objects.requireNonNull(definition, "definition");
    if (!enabled) return definition;
    int safeTeams = Math.max(1, configuredTeams);
    int safePlayers = Math.max(1, players);
    double rawFactor = Math.sqrt((double) safeTeams / safePlayers);
    double factor = Math.max(minimumFactor, Math.min(maximumFactor, rawFactor));
    long baseMillis = definition.interval().toMillis();
    long adjustedMillis = Math.max(50L, Math.round((baseMillis * factor) / 50.0) * 50L);
    return definition.withInterval(Duration.ofMillis(adjustedMillis));
  }

  public double minimumFactor() {
    return minimumFactor;
  }

  public double maximumFactor() {
    return maximumFactor;
  }
}
