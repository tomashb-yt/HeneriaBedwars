package fr.heneria.bedwars.plugin.map;

import fr.heneria.bedwars.core.gui.Gui;
import java.util.UUID;

/** Deferred routes break the dashboard/map/arena factory construction cycle. */
public interface MapMenuNavigation {
  Gui dashboard(UUID playerId);

  Gui arenaEditor(UUID playerId, String arenaId);
}
