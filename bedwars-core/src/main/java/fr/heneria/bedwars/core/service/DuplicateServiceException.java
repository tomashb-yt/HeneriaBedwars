package fr.heneria.bedwars.core.service;

/** Raised when a type already has a service registered. */
public final class DuplicateServiceException extends IllegalStateException {
  private static final long serialVersionUID = 1L;

  /** Creates an error naming the service type that was registered twice. */
  public DuplicateServiceException(Class<?> type) {
    super("A service is already registered for type " + type.getName());
  }
}
