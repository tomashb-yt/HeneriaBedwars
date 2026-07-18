package fr.heneria.bedwars.plugin.statistics;

/** Unchecked boundary exception propagated through asynchronous repository stages. */
public final class StatisticsStorageException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public StatisticsStorageException(String message, Throwable cause) {
    super(message, cause);
  }
}
