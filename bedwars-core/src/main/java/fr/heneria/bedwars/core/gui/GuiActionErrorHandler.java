package fr.heneria.bedwars.core.gui;

/** Central callback receiving every exception thrown by a button action. */
@FunctionalInterface
public interface GuiActionErrorHandler {
  void handle(GuiClickContext context, Exception failure);
}
