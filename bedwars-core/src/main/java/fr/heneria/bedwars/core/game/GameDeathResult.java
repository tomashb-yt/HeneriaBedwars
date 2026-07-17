package fr.heneria.bedwars.core.game;

import java.util.Objects;
import java.util.Optional;

public record GameDeathResult(
    DeathDecision decision,
    Optional<GameInstance> game,
    Optional<String> eliminatedTeam,
    Optional<String> winnerTeam) {
  public GameDeathResult {
    Objects.requireNonNull(decision, "decision");
    game = Objects.requireNonNull(game, "game");
    eliminatedTeam = Objects.requireNonNull(eliminatedTeam, "eliminatedTeam");
    winnerTeam = Objects.requireNonNull(winnerTeam, "winnerTeam");
  }

  public static GameDeathResult ignored() {
    return new GameDeathResult(
        DeathDecision.IGNORE, Optional.empty(), Optional.empty(), Optional.empty());
  }
}
