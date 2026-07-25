package fr.heneria.zombie.plugin.listener;

import fr.heneria.zombie.core.config.ZombieSettings.ChatOptions;
import fr.heneria.zombie.core.isolation.VisibilityPolicy;
import fr.heneria.zombie.core.session.PlayerContext;
import fr.heneria.zombie.core.session.PlayerSessionService;
import fr.heneria.zombie.core.session.PlayerSessionSnapshot;
import fr.heneria.zombie.plugin.config.ConfigurationManager;
import fr.heneria.zombie.plugin.message.MessageService;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/** Filters Paper chat viewers by logical context without replacing the chat renderer. */
public final class IsolatedChatListener implements Listener {

  private final PlayerSessionService sessions;
  private final VisibilityPolicy visibility;
  private final ConfigurationManager configurations;
  private final MessageService messages;

  /**
   * Creates the chat listener.
   *
   * @param sessions session source
   * @param visibility context policy
   * @param configurations active settings
   * @param messages messages
   */
  public IsolatedChatListener(
      PlayerSessionService sessions,
      VisibilityPolicy visibility,
      ConfigurationManager configurations,
      MessageService messages) {
    this.sessions = Objects.requireNonNull(sessions, "sessions");
    this.visibility = Objects.requireNonNull(visibility, "visibility");
    this.configurations = Objects.requireNonNull(configurations, "configurations");
    this.messages = Objects.requireNonNull(messages, "messages");
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onChat(AsyncChatEvent event) {
    ChatOptions chat = configurations.current().settings().chat();
    if (!chat.isolationEnabled()) {
      return;
    }
    Player sender = event.getPlayer();
    String plain = PlainTextComponentSerializer.plainText().serialize(event.message());
    if (chat.allowGlobalAdminChannel()
        && sender.hasPermission("zombie.chat.global")
        && plain.startsWith("!")) {
      event.message(Component.text(plain.substring(1).stripLeading()));
      return;
    }
    PlayerSessionSnapshot senderSession = sessions.findSession(sender.getUniqueId()).orElse(null);
    if (senderSession == null) {
      event.setCancelled(true);
      return;
    }
    boolean channelEnabled =
        senderSession.context() == PlayerContext.LOBBY
            ? chat.lobbyChannelEnabled()
            : chat.instanceChannelEnabled();
    if (!channelEnabled) {
      event.setCancelled(true);
      sender.sendMessage(messages.render("context.chat-disabled"));
      return;
    }
    event
        .viewers()
        .removeIf(
            viewer -> {
              if (!(viewer instanceof Player target)) {
                return false;
              }
              return sessions
                  .findSession(target.getUniqueId())
                  .map(targetSession -> !visibility.canSee(senderSession, targetSession))
                  .orElse(true);
            });
  }
}
