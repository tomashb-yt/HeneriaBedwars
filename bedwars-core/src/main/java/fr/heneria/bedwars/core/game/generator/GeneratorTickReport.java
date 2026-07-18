package fr.heneria.bedwars.core.game.generator;

import java.util.List;

/** Observable result of one global generator pass. */
public record GeneratorTickReport(
    List<GeneratorEmission> emissions,
    int activeGames,
    int visitedGenerators,
    int capacityBlocks,
    boolean truncated) {
  public GeneratorTickReport {
    emissions = List.copyOf(emissions);
    if (activeGames < 0 || visitedGenerators < 0 || capacityBlocks < 0)
      throw new IllegalArgumentException("Generator tick counters cannot be negative");
  }

  public static GeneratorTickReport empty() {
    return new GeneratorTickReport(List.of(), 0, 0, 0, false);
  }
}
