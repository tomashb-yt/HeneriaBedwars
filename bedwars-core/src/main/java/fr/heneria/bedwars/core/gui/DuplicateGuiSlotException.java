package fr.heneria.bedwars.core.gui;

/** Raised when a builder would silently overwrite a button. */
public final class DuplicateGuiSlotException extends GuiBuildException {
  private static final long serialVersionUID = 1L;

  public DuplicateGuiSlotException(int slot) {
    super("GUI slot is already occupied: " + slot);
  }
}
