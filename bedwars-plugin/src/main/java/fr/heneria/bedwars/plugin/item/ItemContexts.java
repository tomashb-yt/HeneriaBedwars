package fr.heneria.bedwars.plugin.item;

import fr.heneria.bedwars.core.item.ItemContext;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import org.bukkit.entity.Player;

/** Small adapter for constructing core item contexts from online players. */
public final class ItemContexts {
  private ItemContexts() {}

  public static ItemContext.Builder forPlayer(Player player, ConfigurationService configurations) {
    return ItemContext.builder()
        .player(player.getUniqueId(), player.getName())
        .locale(configurations.snapshot().plugin().locale())
        .placeholder("language", configurations.snapshot().plugin().locale())
        .placeholder("plugin_version", "runtime")
        .placeholder("sessions", 0)
        .placeholder("gui_sessions", 0)
        .placeholder("refresh_count", 0)
        .placeholder("element", 1)
        .placeholder("max_page", 1);
  }
}
