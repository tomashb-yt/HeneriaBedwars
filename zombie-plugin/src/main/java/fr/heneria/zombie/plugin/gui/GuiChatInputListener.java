package fr.heneria.zombie.plugin.gui;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Objects;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/** Private, cancellable chat input adapter for pending GUI requests. */
public final class GuiChatInputListener implements Listener {
  private final JavaPlugin plugin;
  private final GuiService service;

  public GuiChatInputListener(JavaPlugin plugin, GuiService service) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.service = Objects.requireNonNull(service, "service");
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onChat(AsyncChatEvent event) {
    GuiSession session = service.session(event.getPlayer());
    if (session == null || session.inputRequest().isEmpty()) {
      return;
    }
    event.setCancelled(true);
    String value = PlainTextComponentSerializer.plainText().serialize(event.message());
    plugin
        .getServer()
        .getScheduler()
        .runTask(plugin, () -> service.submitInput(event.getPlayer(), value));
  }
}
