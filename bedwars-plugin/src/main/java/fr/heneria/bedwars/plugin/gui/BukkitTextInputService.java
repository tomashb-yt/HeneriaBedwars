package fr.heneria.bedwars.plugin.gui;

import fr.heneria.bedwars.core.arena.editor.ArenaEditorStateStore;
import fr.heneria.bedwars.core.gui.TextInputCancelReason;
import fr.heneria.bedwars.core.gui.TextInputManager;
import fr.heneria.bedwars.core.gui.TextInputRequest;
import fr.heneria.bedwars.core.gui.TextInputService;
import fr.heneria.bedwars.core.gui.TextInputSubmission;
import fr.heneria.bedwars.core.lifecycle.LifecycleComponent;
import fr.heneria.bedwars.core.logging.ProjectLogger;
import java.util.Collection;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Spigot-compatible chat input adapter.
 *
 * <p>The asynchronous chat event is cancelled immediately. Validation and every callback are then
 * rescheduled on the primary server thread; raw input is never logged.
 */
@SuppressWarnings("deprecation")
public final class BukkitTextInputService
    implements TextInputService, LifecycleComponent, Listener {
  private final JavaPlugin plugin;
  private final TextInputManager manager;
  private final ArenaEditorStateStore editorStates;
  private final ProjectLogger logger;
  private BukkitTask expiryTask;

  public BukkitTextInputService(
      JavaPlugin plugin,
      TextInputManager manager,
      ArenaEditorStateStore editorStates,
      ProjectLogger logger) {
    this.plugin = plugin;
    this.manager = manager;
    this.editorStates = editorStates;
    this.logger = logger;
  }

  @Override
  public String name() {
    return "text-input";
  }

  @Override
  public void start() {
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
    expiryTask = plugin.getServer().getScheduler().runTaskTimer(plugin, manager::expire, 20, 20);
  }

  @Override
  public void stop() {
    if (expiryTask != null) expiryTask.cancel();
    manager.cancelAll(TextInputCancelReason.PLUGIN_STOP);
    editorStates.clear();
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onChat(AsyncPlayerChatEvent event) {
    UUID playerId = event.getPlayer().getUniqueId();
    if (!manager.active(playerId)) return;
    event.setCancelled(true);
    String answer = event.getMessage();
    Bukkit.getScheduler().runTask(plugin, () -> manager.submit(playerId, answer));
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    disconnect(event.getPlayer().getUniqueId());
  }

  @EventHandler
  public void onKick(PlayerKickEvent event) {
    disconnect(event.getPlayer().getUniqueId());
  }

  private void disconnect(UUID playerId) {
    manager.cancel(playerId, TextInputCancelReason.DISCONNECT);
    editorStates.remove(playerId);
  }

  @Override
  public boolean begin(UUID playerId, TextInputRequest request) {
    boolean started = manager.begin(playerId, request);
    if (!started) logger.debug("Text input already active for player " + playerId);
    return started;
  }

  @Override
  public boolean active(UUID playerId) {
    return manager.active(playerId);
  }

  @Override
  public TextInputSubmission submit(UUID playerId, String message) {
    if (!Bukkit.isPrimaryThread())
      throw new IllegalStateException("Text input submission must run on the server thread");
    return manager.submit(playerId, message);
  }

  @Override
  public boolean cancel(UUID playerId, TextInputCancelReason reason) {
    return manager.cancel(playerId, reason);
  }

  @Override
  public Collection<UUID> expire() {
    return manager.expire();
  }

  @Override
  public void cancelAll(TextInputCancelReason reason) {
    manager.cancelAll(reason);
  }
}
