package fr.heneria.zombie.plugin.listener;

import fr.heneria.zombie.plugin.map.MapPreviewService;
import java.util.Objects;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/** Cleans temporary map previews when their administrator disconnects. */
public final class MapPreviewListener implements Listener {

  private final MapPreviewService previews;

  /**
   * Creates the listener.
   *
   * @param previews preview lifecycle
   */
  public MapPreviewListener(MapPreviewService previews) {
    this.previews = Objects.requireNonNull(previews, "previews");
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onQuit(PlayerQuitEvent event) {
    previews.disconnect(event.getPlayer().getUniqueId());
  }
}
