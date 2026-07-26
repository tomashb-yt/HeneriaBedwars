package fr.heneria.zombie.core.enemy;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.function.IntUnaryOperator;

/** Pure target filtering shared by platform adapters and future custom behaviors. */
public final class ZombieTargetSelector {

  public Optional<Candidate> select(
      UUID gameId,
      String worldName,
      ZombieDefinition.TargetStrategy strategy,
      Collection<Candidate> candidates,
      IntUnaryOperator random) {
    java.util.List<Candidate> valid =
        candidates.stream()
            .filter(candidate -> candidate.gameId().equals(gameId))
            .filter(candidate -> candidate.worldName().equals(worldName))
            .filter(Candidate::alive)
            .filter(Candidate::targetable)
            .filter(candidate -> !candidate.spectator())
            .toList();
    if (valid.isEmpty()) {
      return Optional.empty();
    }
    return switch (strategy) {
      case LOWEST_HEALTH -> valid.stream().min(Comparator.comparingDouble(Candidate::health));
      case HIGHEST_POINTS -> valid.stream().max(Comparator.comparingInt(Candidate::points));
      case LAST_ATTACKER ->
          valid.stream()
              .filter(Candidate::lastAttacker)
              .findFirst()
              .or(() -> valid.stream().min(Comparator.comparingDouble(Candidate::distanceSquared)));
      case RANDOM_VALID_PLAYER ->
          Optional.of(valid.get(Math.floorMod(random.applyAsInt(valid.size()), valid.size())));
      case NEAREST_VALID_PLAYER, FIXED, CUSTOM ->
          valid.stream().min(Comparator.comparingDouble(Candidate::distanceSquared));
    };
  }

  public record Candidate(
      UUID playerId,
      UUID gameId,
      String worldName,
      boolean alive,
      boolean targetable,
      boolean spectator,
      double health,
      int points,
      double distanceSquared,
      boolean lastAttacker) {
    public Candidate {
      java.util.Objects.requireNonNull(playerId, "playerId");
      java.util.Objects.requireNonNull(gameId, "gameId");
      java.util.Objects.requireNonNull(worldName, "worldName");
    }
  }
}
