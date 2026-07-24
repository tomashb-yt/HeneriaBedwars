package fr.heneria.zombie.core.bootstrap;

/** Reports a lifecycle failure together with the component that caused it. */
public final class LifecycleException extends Exception {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a lifecycle exception.
   *
   * @param message diagnostic message
   * @param cause original failure
   */
  public LifecycleException(String message, Throwable cause) {
    super(message, cause);
  }
}
