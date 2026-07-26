package fr.heneria.zombie.plugin.listener;

import fr.heneria.zombie.core.session.PlayerSessionService;
import fr.heneria.zombie.plugin.config.ConfigurationManager;
import fr.heneria.zombie.plugin.game.PaperGameRuntime;
import fr.heneria.zombie.plugin.instance.InstanceCoordinator;
import fr.heneria.zombie.plugin.isolation.PaperAudienceService;
import fr.heneria.zombie.plugin.message.MessageService;
import java.util.Objects;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Maintains a valid session for every connection and suppresses global join/quit leakage. */
public final class PlayerContextListener implements Listener {

  private final InstanceCoordinator coordinator;
  private final PlayerSessionService sessions;
  private final ConfigurationManager configurations;
  private final PaperAudienceService audiences;
  private final MessageService messages;
  private final PaperGameRuntime games;

  /**
   * Creates the listener.
   *
   * @param coordinator context coordinator
   * @param sessions session service
   * @param configurations active settings
   * @param audiences targeted audiences
   * @param messages messages
   */
  public PlayerContextListener(
      InstanceCoordinator coordinator,
      PlayerSessionService sessions,
      ConfigurationManager configurations,
      PaperAudienceService audiences,
      MessageService messages,
      PaperGameRuntime games) {
    this.coordinator = Objects.requireNonNull(coordinator, "coordinator");
    this.sessions = Objects.requireNonNull(sessions, "sessions");
    this.configurations = Objects.requireNonNull(configurations, "configurations");
    this.audiences = Objects.requireNonNull(audiences, "audiences");
    this.messages = Objects.requireNonNull(messages, "messages");
    this.games = Objects.requireNonNull(games, "games");
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onJoin(PlayerJoinEvent event) {
    event.joinMessage(null);
    coordinator
        .connect(event.getPlayer())
        .thenAccept(
            context -> {
              var message =
                  messages.render("context.player-joined", "player", event.getPlayer().getName());
              if (context == fr.heneria.zombie.core.session.PlayerContext.INSTANCE) {
                sessions
                    .findSession(event.getPlayer().getUniqueId())
                    .flatMap(session -> session.instanceId())
                    .ifPresent(
                        instanceId -> {
                          games.reconnected(instanceId, event.getPlayer().getUniqueId());
                          audiences.instance(instanceId).sendMessage(message);
                        });
              } else {
                audiences.lobby().sendMessage(message);
              }
            });
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onQuit(PlayerQuitEvent event) {
    event.quitMessage(null);
    sessions
        .findSession(event.getPlayer().getUniqueId())
        .flatMap(session -> session.instanceId())
        .ifPresentOrElse(
            instanceId -> {
              games.disconnected(instanceId, event.getPlayer().getUniqueId());
              audiences
                  .instance(instanceId)
                  .sendMessage(
                      messages.render(
                          "context.player-disconnected", "player", event.getPlayer().getName()));
            },
            () ->
                audiences
                    .lobby()
                    .sendMessage(
                        messages.render(
                            "context.player-left", "player", event.getPlayer().getName())));
    coordinator.disconnect(
        event.getPlayer().getUniqueId(),
        configurations.current().settings().reconnect().reservePlayerSlot());
  }
}
