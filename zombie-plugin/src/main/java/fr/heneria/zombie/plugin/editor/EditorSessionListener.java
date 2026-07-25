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

  public EditorSessionListener(MapEditorService editors, EditorItemService items) {
    this.editors = Objects.requireNonNull(editors, "editors");
    this.items = Objects.requireNonNull(items, "items");
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    editors.leave(event.getPlayer().getUniqueId());
    items.remove(event.getPlayer());
  }
}
