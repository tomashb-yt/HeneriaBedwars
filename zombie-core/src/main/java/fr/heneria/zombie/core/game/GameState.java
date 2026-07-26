package fr.heneria.zombie.core.game;

import java.util.EnumSet;
import java.util.Set;

/** Strict business lifecycle of one Zombies game. */
public enum GameState {
  CREATED,
  WAITING_FOR_PLAYERS,
  PREPARING,
  COUNTDOWN,
  STARTING,
  ROUND_ACTIVE,
  ROUND_TRANSITION,
  PAUSED,
  ENDING,
  FINISHED,
  FAILED,
  CLEANING;

  private static final java.util.Map<GameState, Set<GameState>> TRANSITIONS =
      java.util.Map.ofEntries(
          java.util.Map.entry(CREATED, EnumSet.of(WAITING_FOR_PLAYERS, FAILED, ENDING)),
          java.util.Map.entry(WAITING_FOR_PLAYERS, EnumSet.of(PREPARING, ENDING, FAILED)),
          java.util.Map.entry(
              PREPARING, EnumSet.of(COUNTDOWN, WAITING_FOR_PLAYERS, FAILED, ENDING)),
          java.util.Map.entry(COUNTDOWN, EnumSet.of(STARTING, WAITING_FOR_PLAYERS, ENDING, FAILED)),
          java.util.Map.entry(STARTING, EnumSet.of(ROUND_ACTIVE, ENDING, FAILED)),
          java.util.Map.entry(ROUND_ACTIVE, EnumSet.of(ROUND_TRANSITION, PAUSED, ENDING, FAILED)),
          java.util.Map.entry(PAUSED, EnumSet.of(ROUND_ACTIVE, ENDING, FAILED)),
          java.util.Map.entry(ROUND_TRANSITION, EnumSet.of(ROUND_ACTIVE, ENDING, FAILED)),
          java.util.Map.entry(ENDING, EnumSet.of(FINISHED, FAILED)),
          java.util.Map.entry(FINISHED, EnumSet.of(CLEANING)),
          java.util.Map.entry(FAILED, EnumSet.of(CLEANING)),
          java.util.Map.entry(CLEANING, EnumSet.noneOf(GameState.class)));

  public boolean canTransitionTo(GameState target) {
    return TRANSITIONS.get(this).contains(target);
  }
}
