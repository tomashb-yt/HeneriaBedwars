package fr.heneria.bedwars.core.lifecycle;

/** Reports a lifecycle failure while preserving its original cause. */
public final class LifecycleException extends Exception {
  private static final long serialVersionUID = 1L;

  /** Creates a lifecycle failure while retaining the triggering exception. */
  public LifecycleException(String message, Throwable cause) {
    super(message, cause);
  }
}
