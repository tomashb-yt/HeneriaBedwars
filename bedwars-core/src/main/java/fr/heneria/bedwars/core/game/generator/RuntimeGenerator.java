package fr.heneria.bedwars.core.game.generator;

import fr.heneria.bedwars.core.game.GameId;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntSupplier;

/** Mutable match-scoped schedule for one immutable generator definition. */
public final class RuntimeGenerator {
  private final GeneratorDefinition definition;
  private Instant nextEmissionAt;
  private long emissions;
  private long capacityBlocks;

  public RuntimeGenerator(GeneratorDefinition definition, Instant initializedAt) {
    this.definition = Objects.requireNonNull(definition, "definition");
    this.nextEmissionAt =
        Objects.requireNonNull(initializedAt, "initializedAt").plus(definition.interval());
  }

  public GeneratorDefinition definition() {
    return definition;
  }

  public synchronized Instant nextEmissionAt() {
    return nextEmissionAt;
  }

  public synchronized long emissions() {
    return emissions;
  }

  public synchronized long capacityBlocks() {
    return capacityBlocks;
  }

  synchronized GeneratorPollResult poll(
      GameId gameId, Instant now, IntSupplier nearbyAmountSupplier) {
    Objects.requireNonNull(gameId, "gameId");
    Objects.requireNonNull(now, "now");
    Objects.requireNonNull(nearbyAmountSupplier, "nearbyAmountSupplier");
    if (now.isBefore(nextEmissionAt)) return GeneratorPollResult.notDue();

    Instant scheduledAt = nextEmissionAt;
    advanceSchedulePast(now);
    int available = definition.localCapacity() - Math.max(0, nearbyAmountSupplier.getAsInt());
    if (available < 1) {
      capacityBlocks++;
      return GeneratorPollResult.capacityReached();
    }

    int amount = Math.min(definition.amountPerEmission(), available);
    emissions++;
    return GeneratorPollResult.emitted(
        new GeneratorEmission(gameId, definition, amount, scheduledAt, now));
  }

  private void advanceSchedulePast(Instant now) {
    Duration overdue = Duration.between(nextEmissionAt, now);
    long intervals = overdue.dividedBy(definition.interval()) + 1;
    nextEmissionAt = nextEmissionAt.plus(definition.interval().multipliedBy(intervals));
  }

  record GeneratorPollResult(Status status, Optional<GeneratorEmission> emission) {
    GeneratorPollResult {
      status = Objects.requireNonNull(status, "status");
      emission = Objects.requireNonNull(emission, "emission");
    }

    static GeneratorPollResult notDue() {
      return new GeneratorPollResult(Status.NOT_DUE, Optional.empty());
    }

    static GeneratorPollResult capacityReached() {
      return new GeneratorPollResult(Status.CAPACITY_REACHED, Optional.empty());
    }

    static GeneratorPollResult emitted(GeneratorEmission emission) {
      return new GeneratorPollResult(Status.EMITTED, Optional.of(emission));
    }
  }

  enum Status {
    NOT_DUE,
    CAPACITY_REACHED,
    EMITTED
  }
}
