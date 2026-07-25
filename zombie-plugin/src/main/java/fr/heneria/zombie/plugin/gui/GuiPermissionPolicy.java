package fr.heneria.zombie.plugin.gui;

/** Pure permission visibility policy shared by configured buttons. */
public final class GuiPermissionPolicy {
  private GuiPermissionPolicy() {}

  public static Visibility evaluate(boolean permitted, boolean showWhenLocked) {
    if (permitted) {
      return Visibility.ENABLED;
    }
    return showWhenLocked ? Visibility.LOCKED : Visibility.HIDDEN;
  }

  public enum Visibility {
    ENABLED,
    LOCKED,
    HIDDEN
  }
}
