package fr.heneria.bedwars.core.gui;

import java.util.Objects;

/** Executes actions without allowing failures to escape the GUI event boundary. */
public final class GuiActionExecutor {
  private final GuiActionErrorHandler errors;

  public GuiActionExecutor(GuiActionErrorHandler errors) {
    this.errors = Objects.requireNonNull(errors);
  }

  public boolean execute(GuiAction action, GuiClickContext context) {
    try {
      action.execute(context);
      return true;
    } catch (Exception exception) {
      errors.handle(context, exception);
      return false;
    }
  }
}
