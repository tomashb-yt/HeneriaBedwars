package fr.heneria.zombie.plugin.editor;

import fr.heneria.zombie.core.editor.MapEditorService;
import java.util.Objects;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/** Saves and releases editor state when an administrator disconnects. */
public final class EditorSessionListener implements Listener {
  private final MapEditorService editors;
  private final EditorItemService items;
  private final MapVisualizationService visualizations;

  public EditorSessionListener(
      MapEditorService editors, EditorItemService items, MapVisualizationService visualizations) {
    this.editors = Objects.requireNonNull(editors, "editors");
    this.items = Objects.requireNonNull(items, "items");
    this.visualizations = Objects.requireNonNull(visualizations, "visualizations");
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    editors.leave(event.getPlayer().getUniqueId());
    items.remove(event.getPlayer());
    visualizations.clearEditor(event.getPlayer().getUniqueId());
  }
}
