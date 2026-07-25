package fr.heneria.zombie.core.instance;

import java.util.EnumSet;
import java.util.Set;

/** Controlled lifecycle states of one isolated Zombies instance. */
public enum GameInstanceState {
  CREATING,
  WAITING,
  STARTING,
  RUNNING,
  ENDING,
  CLEANING,
  CLOSED,
  ERROR;

  /**
   * Returns whether this state may transition directly to the target state.
   *
   * @param target requested target
   * @return whether the transition is valid
   */
  public boolean canTransitionTo(GameInstanceState target) {
    return transitions().contains(target);
  }

  private Set<GameInstanceState> transitions() {
    return switch (this) {
      case CREATING -> EnumSet.of(WAITING, ERROR);
      case WAITING -> EnumSet.of(STARTING, ENDING, ERROR);
      case STARTING -> EnumSet.of(RUNNING, ENDING, ERROR);
      case RUNNING -> EnumSet.of(ENDING, ERROR);
      case ENDING -> EnumSet.of(CLEANING, ERROR);
      case CLEANING -> EnumSet.of(CLOSED, ERROR);
      case ERROR -> EnumSet.of(CLEANING, CLOSED);
      case CLOSED -> EnumSet.noneOf(GameInstanceState.class);
    };
  }
}
