package fr.heneria.zombie.plugin.gui;

/** Action executed after a validated GUI click. */
@FunctionalInterface
public interface GuiAction {

  /**
   * Executes the action on the Paper server thread.
   *
   * @param context click context
   */
  void execute(GuiClickContext context);
}
