package fr.heneria.zombie.core.enemy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntUnaryOperator;

/** Deterministic weighted selector with round, zone, pool, category and alive-cap filters. */
public final class ZombieSelectionService {

  public Optional<ZombieDefinition> select(
      Collection<ZombieDefinition> definitions,
      SelectionContext context,
      IntUnaryOperator boundedRandom) {
    ArrayList<ZombieDefinition> eligible =
        definitions.stream()
            .filter(
                value ->
                    context.categories().isEmpty()
                        || context.categories().contains(value.category()))
            .filter(value -> value.spawnRules().minimumRound() <= context.round())
            .filter(value -> value.spawnRules().maximumRound() >= context.round())
            .filter(
                value ->
                    value.spawnRules().zones().isEmpty()
                        || context.zoneId().map(value.spawnRules().zones()::contains).orElse(false))
            .filter(
                value ->
                    value.spawnRules().pools().isEmpty()
                        || value.spawnRules().pools().contains(context.poolId()))
            .filter(
                value ->
                    value.spawnRules().maximumAlive() < 0
                        || context.aliveByType().getOrDefault(value.id(), 0)
                            < value.spawnRules().maximumAlive())
            .sorted(Comparator.comparing(ZombieDefinition::id))
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    int totalWeight = eligible.stream().mapToInt(value -> value.spawnRules().weight()).sum();
    if (totalWeight <= 0) {
      return Optional.empty();
    }
    int cursor = Math.floorMod(boundedRandom.applyAsInt(totalWeight), totalWeight);
    for (ZombieDefinition definition : eligible) {
      cursor -= definition.spawnRules().weight();
      if (cursor < 0) {
        return Optional.of(definition);
      }
    }
    return Optional.empty();
  }

  public record SelectionContext(
      int round,
      java.util.Optional<String> zoneId,
      String poolId,
      Set<ZombieDefinition.ZombieCategory> categories,
      Map<String, Integer> aliveByType) {
    public SelectionContext {
      if (round <= 0) {
        throw new IllegalArgumentException("round must be > 0");
      }
      zoneId = java.util.Objects.requireNonNull(zoneId, "zoneId");
      poolId = java.util.Objects.requireNonNull(poolId, "poolId");
      categories = Set.copyOf(categories);
      aliveByType = Map.copyOf(aliveByType);
    }
  }
}
