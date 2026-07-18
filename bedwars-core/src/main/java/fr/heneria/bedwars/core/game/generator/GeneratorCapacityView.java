package fr.heneria.bedwars.core.game.generator;

import fr.heneria.bedwars.core.game.GameId;

/** Platform-neutral view of matching resource items already present near a generator. */
@FunctionalInterface
public interface GeneratorCapacityView {
  int nearbyAmount(GameId gameId, GeneratorDefinition generator);

  static GeneratorCapacityView empty() {
    return (gameId, generator) -> 0;
  }
}
