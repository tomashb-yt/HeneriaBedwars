package fr.heneria.bedwars.core.logging;

/** Logging boundary used by the platform-independent core. */
public interface ProjectLogger {
  /** Records normal operational information. */
  void info(String message);

  /** Records a recoverable warning. */
  void warning(String message);

  /** Records an error and preserves its complete cause. */
  void error(String message, Throwable cause);

  /** Records diagnostic information when debug mode is enabled. */
  void debug(String message);

  /** Updates debug filtering when the active configuration changes. */
  default void setDebug(boolean enabled) {
    // Platform adapters without filtering do not need to react.
  }
}
