package fr.heneria.zombie.core.instance;

/** Reports a failed instance creation without exposing platform exceptions to callers. */
public final class InstanceCreationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception.
   *
   * @param message diagnostic message
   * @param cause original failure
   */
  public InstanceCreationException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates the exception without a nested cause.
   *
   * @param message diagnostic message
   */
  public InstanceCreationException(String message) {
    super(message);
  }
}
