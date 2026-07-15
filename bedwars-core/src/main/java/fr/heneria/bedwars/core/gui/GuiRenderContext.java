package fr.heneria.bedwars.core.gui;

/** Values available while rendering a dynamic item. */
public record GuiRenderContext(String playerName, GuiSession session) {
  public int page() {
    return session.page();
  }
}
