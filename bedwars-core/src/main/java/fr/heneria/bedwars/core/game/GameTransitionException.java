package fr.heneria.bedwars.core.game;

import fr.heneria.bedwars.api.game.GameState;

/** Raised when a caller attempts to bypass the explicit game state machine. */
public final class GameTransitionException extends IllegalStateException {
  private static final long serialVersionUID = 1L;

  public GameTransitionException(GameState from, GameState to) {
    super("Illegal game transition " + from + " -> " + to);
  }
}
