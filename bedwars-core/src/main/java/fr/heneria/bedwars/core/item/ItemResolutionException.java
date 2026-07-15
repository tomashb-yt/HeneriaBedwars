package fr.heneria.bedwars.core.item;

/** Indicates an unknown parent, inheritance cycle, or excessive inheritance depth. */
public final class ItemResolutionException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /** Creates a readable inheritance-resolution failure. */
  public ItemResolutionException(String message) {
    super(message);
  }
}
