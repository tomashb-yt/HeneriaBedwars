package fr.heneria.bedwars.core.service;

/** Raised when a required service is absent from the registry. */
public final class MissingServiceException extends IllegalStateException {
  private static final long serialVersionUID = 1L;

  /** Creates an error naming the required service type that is absent. */
  public MissingServiceException(Class<?> type) {
    super("No required service is registered for type " + type.getName());
  }
}
