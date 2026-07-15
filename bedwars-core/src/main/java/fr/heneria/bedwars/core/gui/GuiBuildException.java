package fr.heneria.bedwars.core.gui;

/** Raised when an immutable GUI cannot be built from invalid input. */
public class GuiBuildException extends IllegalArgumentException {
  private static final long serialVersionUID = 1L;

  public GuiBuildException(String message) {
    super(message);
  }
}
