package fr.heneria.bedwars.core.gui;

/** A guarded button action. Exceptions are intercepted by the platform error handler. */
@FunctionalInterface
public interface GuiAction {
  void execute(GuiClickContext context) throws Exception;
}
