package fr.heneria.bedwars.plugin.gui;

import fr.heneria.bedwars.core.gui.Gui;
import fr.heneria.bedwars.core.gui.GuiSession;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;

/** Main-thread Bukkit GUI service. Off-thread openings are rescheduled safely. */
public interface GuiService {
  void open(Player player, Gui gui);

  void close(Player player);

  void refresh(Player player);

  Optional<GuiSession> findSession(UUID playerId);

  default boolean hasOpenGui(UUID playerId) {
    return findSession(playerId).isPresent();
  }

  int openCount();

  void closeAll();
}
