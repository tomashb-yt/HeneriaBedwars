package fr.heneria.bedwars.core.item;

/** Raised when a required logical item key is absent from the active registry. */
public final class UnknownItemException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public UnknownItemException(ItemKey key) {
    super("Unknown item definition: " + key);
  }
}
