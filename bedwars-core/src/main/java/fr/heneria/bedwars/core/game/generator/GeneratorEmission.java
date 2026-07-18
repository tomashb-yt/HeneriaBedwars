package fr.heneria.bedwars.core.game.generator;

import fr.heneria.bedwars.core.game.GameId;
import java.time.Instant;
import java.util.Objects;

/** Pure request for the platform adapter to create or merge one resource drop. */
public record GeneratorEmission(
    GameId gameId,
    GeneratorDefinition generator,
    int amount,
    Instant scheduledAt,
    Instant emittedAt) {
  public GeneratorEmission {
    gameId = Objects.requireNonNull(gameId, "gameId");
    generator = Objects.requireNonNull(generator, "generator");
    scheduledAt = Objects.requireNonNull(scheduledAt, "scheduledAt");
    emittedAt = Objects.requireNonNull(emittedAt, "emittedAt");
    if (amount < 1 || amount > generator.amountPerEmission())
      throw new IllegalArgumentException("Invalid emitted amount");
  }
}
