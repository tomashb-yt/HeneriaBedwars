package fr.heneria.zombie.plugin.listener;

import fr.heneria.zombie.core.session.PlayerSessionService;
import fr.heneria.zombie.plugin.isolation.PaperAudienceService;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/** Redirects death messages to the deceased player's exact context. */
public final class ContextDeathListener implements Listener {

  private final PlayerSessionService sessions;
  private final PaperAudienceService audiences;

  /**
   * Creates the listener.
   *
   * @param sessions session source
   * @param audiences targeted audiences
   */
  public ContextDeathListener(PlayerSessionService sessions, PaperAudienceService audiences) {
    this.sessions = Objects.requireNonNull(sessions, "sessions");
    this.audiences = Objects.requireNonNull(audiences, "audiences");
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onDeath(PlayerDeathEvent event) {
    Component deathMessage = event.deathMessage();
    event.deathMessage(null);
    if (deathMessage == null) {
      return;
    }
    sessions
        .findSession(event.getPlayer().getUniqueId())
        .ifPresent(
            session ->
                session
                    .instanceId()
                    .ifPresentOrElse(
                        instanceId -> audiences.instance(instanceId).sendMessage(deathMessage),
                        () -> audiences.lobby().sendMessage(deathMessage)));
  }
}
