package fr.heneria.zombie.plugin.gui;

import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

/**
 * Validated click routed to a button action.
 *
 * @param player clicking player
 * @param service GUI service
 * @param session isolated GUI session
 * @param click click type
 * @param slot raw top-inventory slot
 */
public record GuiClickContext(
    Player player, GuiService service, GuiSession session, ClickType click, int slot) {

  /** Validates mandatory values. */
  public GuiClickContext {
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(service, "service");
    Objects.requireNonNull(session, "session");
    Objects.requireNonNull(click, "click");
  }
}
