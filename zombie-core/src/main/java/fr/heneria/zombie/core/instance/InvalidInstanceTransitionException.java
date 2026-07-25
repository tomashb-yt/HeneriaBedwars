package fr.heneria.zombie.core.instance;

/** Reports a refused instance lifecycle transition. */
public final class InvalidInstanceTransitionException extends IllegalStateException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception.
   *
   * @param current current state
   * @param target rejected target
   */
  public InvalidInstanceTransitionException(GameInstanceState current, GameInstanceState target) {
    super("Invalid instance transition: " + current + " -> " + target);
  }
}
