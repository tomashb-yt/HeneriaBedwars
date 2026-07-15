package fr.heneria.bedwars.plugin.gui;

import fr.heneria.bedwars.core.config.MenuSettings;
import fr.heneria.bedwars.core.config.PlaceholderContext;
import fr.heneria.bedwars.core.config.SoundSettings;
import fr.heneria.bedwars.core.config.TranslationKey;
import fr.heneria.bedwars.core.gui.Gui;
import fr.heneria.bedwars.core.gui.GuiActionExecutor;
import fr.heneria.bedwars.core.gui.GuiButton;
import fr.heneria.bedwars.core.gui.GuiClickContext;
import fr.heneria.bedwars.core.gui.GuiClickType;
import fr.heneria.bedwars.core.gui.GuiCloseReason;
import fr.heneria.bedwars.core.gui.GuiRenderContext;
import fr.heneria.bedwars.core.gui.GuiRuntime;
import fr.heneria.bedwars.core.gui.GuiSession;
import fr.heneria.bedwars.core.gui.GuiSessionManager;
import fr.heneria.bedwars.core.lifecycle.LifecycleComponent;
import fr.heneria.bedwars.core.logging.ProjectLogger;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Bukkit adapter owning GUI views, one central refresh task, and guarded action dispatch. */
public final class BukkitGuiService implements GuiService, LifecycleComponent {
  private final JavaPlugin plugin;
  private final ConfigurationService configurations;
  private final ProjectLogger logger;
  private final GuiSessionManager sessions = new GuiSessionManager();
  private final GuiItemRenderer items = new GuiItemRenderer();
  private final GuiActionExecutor actions;
  private BukkitTask refreshTask;
  private boolean stopped;

  public BukkitGuiService(
      JavaPlugin plugin, ConfigurationService configurations, ProjectLogger logger) {
    this.plugin = plugin;
    this.configurations = configurations;
    this.logger = logger;
    this.actions =
        new GuiActionExecutor(
            (context, exception) -> {
              Player player = Bukkit.getPlayer(context.session().playerId());
              logger.error(
                  "[GUI] Action failure in menu '"
                      + context.gui().id()
                      + "', session '"
                      + context.session().sessionId()
                      + "', player '"
                      + context.runtime().playerName()
                      + "', slot "
                      + context.slot()
                      + ", click "
                      + context.clickType()
                      + '.',
                  exception);
              if (player != null) {
                message(player, TranslationKey.GUI_INTERNAL_ERROR, PlaceholderContext.EMPTY);
                play(player, "error");
              }
            });
  }

  @Override
  public String name() {
    return "gui-service";
  }

  @Override
  public void start() {
    plugin.getServer().getPluginManager().registerEvents(new GuiListener(this), plugin);
    int ticks = settings().minimumRefreshTicks();
    refreshTask =
        plugin
            .getServer()
            .getScheduler()
            .runTaskTimer(plugin, this::refreshDueSessions, ticks, ticks);
  }

  @Override
  public void stop() {
    closeAll();
    if (refreshTask != null) refreshTask.cancel();
    stopped = true;
  }

  @Override
  public void open(Player player, Gui gui) {
    runSync(
        () -> {
          sessions
              .remove(player.getUniqueId())
              .ifPresent(old -> old.current().closed(old, GuiCloseReason.REPLACED));
          GuiSession session = sessions.create(player.getUniqueId(), gui, historyLimit());
          renderNewView(player, session, true);
        });
  }

  void navigate(Player player, Gui gui, boolean remember) {
    runSync(
        () ->
            sessions
                .find(player.getUniqueId())
                .ifPresentOrElse(
                    session -> {
                      session.current().closed(session, GuiCloseReason.NAVIGATION);
                      session.navigate(gui, remember && settings().historyEnabled());
                      renderNewView(player, session, true);
                    },
                    () -> open(player, gui)));
  }

  @Override
  public void close(Player player) {
    runSync(
        () -> {
          sessions.find(player.getUniqueId()).ifPresent(session -> session.closing(true));
          player.closeInventory();
        });
  }

  @Override
  public void refresh(Player player) {
    runSync(
        () -> sessions.find(player.getUniqueId()).ifPresent(session -> refresh(player, session)));
  }

  @Override
  public Optional<GuiSession> findSession(UUID playerId) {
    return sessions.find(playerId);
  }

  @Override
  public int openCount() {
    return sessions.size();
  }

  @Override
  public void closeAll() {
    if (stopped) return;
    for (GuiSession session : sessions.all()) {
      sessions.remove(session.playerId());
      session.closing(true);
      session.current().closed(session, GuiCloseReason.PLUGIN);
      Player player = Bukkit.getPlayer(session.playerId());
      if (player != null && player.isOnline()) player.closeInventory();
    }
    sessions.clear();
  }

  void observeOpen(InventoryOpenEvent event) {
    if (event.getInventory().getHolder() instanceof GuiInventoryHolder holder
        && !valid(event.getPlayer().getUniqueId(), holder)) event.setCancelled(true);
  }

  void handleClick(InventoryClickEvent event) {
    InventoryView view = event.getView();
    if (!(view.getTopInventory().getHolder() instanceof GuiInventoryHolder holder)) return;
    if (settings().cancelPlayerInventoryClicks()
        || event.getRawSlot() < view.getTopInventory().getSize()) event.setCancelled(true);
    if (!(event.getWhoClicked() instanceof Player player)
        || event.getRawSlot() < 0
        || event.getRawSlot() >= view.getTopInventory().getSize()
        || !valid(player.getUniqueId(), holder)) return;
    GuiSession session = sessions.find(player.getUniqueId()).orElseThrow();
    GuiButton button = session.current().buttons().get(event.getRawSlot());
    if (button == null) return;
    GuiRenderContext renderContext = new GuiRenderContext(player.getName(), session);
    if (!button.visible(renderContext)) return;
    if (button.permission().isPresent()
        && !player.hasPermission(button.permission().orElseThrow())) {
      message(player, TranslationKey.GUI_NO_PERMISSION, PlaceholderContext.EMPTY);
      play(player, "error");
      return;
    }
    if (!button.enabled(renderContext)) {
      message(player, TranslationKey.GUI_DISABLED, PlaceholderContext.EMPTY);
      play(player, "error");
      return;
    }
    long cooldown =
        button.cooldownMillis() >= 0
            ? button.cooldownMillis()
            : settings().defaultClickCooldownMillis();
    if (!session.acceptClick(
        session.current().id() + ':' + event.getRawSlot(), cooldown, System.currentTimeMillis()))
      return;
    GuiClickType type = click(event.getClick());
    button
        .action(type)
        .ifPresent(
            action -> {
              GuiClickContext context =
                  new GuiClickContext(
                      new Runtime(player),
                      session.current(),
                      session,
                      event.getRawSlot(),
                      type,
                      session.current().data());
              if (actions.execute(action, context)) play(player, "click");
            });
  }

  void handleDrag(InventoryDragEvent event) {
    if (!(event.getView().getTopInventory().getHolder() instanceof GuiInventoryHolder)) return;
    if (settings().cancelDragEvents()
        && event.getRawSlots().stream()
            .anyMatch(slot -> slot < event.getView().getTopInventory().getSize()))
      event.setCancelled(true);
  }

  void handleClose(InventoryCloseEvent event, GuiCloseReason reason) {
    if (!(event.getInventory().getHolder() instanceof GuiInventoryHolder holder)) return;
    UUID player = event.getPlayer().getUniqueId();
    Optional<GuiSession> found = sessions.find(player);
    if (found.isEmpty() || !matches(found.get(), holder)) return;
    GuiSession session = found.get();
    session.current().closed(session, reason);
    sessions.remove(player, holder.sessionId(), holder.viewId());
    if (event.getPlayer() instanceof Player bukkitPlayer) play(bukkitPlayer, "close");
  }

  void disconnect(Player player, GuiCloseReason reason) {
    sessions
        .remove(player.getUniqueId())
        .ifPresent(
            session -> {
              session.closing(true);
              session.current().closed(session, reason);
            });
  }

  void pluginDisabled(Plugin disabled) {
    if (disabled == plugin) closeAll();
  }

  private void renderNewView(Player player, GuiSession session, boolean logicalOpen) {
    Gui gui = session.current();
    GuiInventoryHolder holder =
        new GuiInventoryHolder(session.sessionId(), session.viewId(), gui.id());
    Inventory inventory = Bukkit.createInventory(holder, gui.size(), gui.title());
    holder.inventory(inventory);
    renderContents(player, session, inventory);
    if (logicalOpen) gui.opened(session);
    player.openInventory(inventory);
    play(player, "open");
  }

  private void refresh(Player player, GuiSession session) {
    Inventory top = player.getOpenInventory().getTopInventory();
    if (!(top.getHolder() instanceof GuiInventoryHolder holder) || !matches(session, holder))
      return;
    top.clear();
    renderContents(player, session, top);
    session.refreshed(System.currentTimeMillis());
  }

  private void renderContents(Player player, GuiSession session, Inventory inventory) {
    Gui gui = session.current();
    GuiRenderContext context = new GuiRenderContext(player.getName(), session);
    gui.buttons()
        .forEach(
            (slot, button) -> {
              if (button.visible(context))
                inventory.setItem(slot, items.render(button.render(context)));
            });
    if (gui.fillEmptySlots())
      for (int slot = 0; slot < inventory.getSize(); slot++)
        if (inventory.getItem(slot) == null) inventory.setItem(slot, items.render(gui.filler()));
  }

  private void refreshDueSessions() {
    if (!settings().refreshEnabled()) return;
    long now = System.currentTimeMillis();
    for (GuiSession session : sessions.all())
      session
          .current()
          .autoRefresh()
          .ifPresent(
              interval -> {
                if (now - session.lastRefreshMillis() >= interval.toMillis()) {
                  Player player = Bukkit.getPlayer(session.playerId());
                  if (player != null && player.isOnline()) refresh(player, session);
                }
              });
  }

  private boolean valid(UUID player, GuiInventoryHolder holder) {
    return sessions.find(player).map(s -> matches(s, holder)).orElse(false);
  }

  private static boolean matches(GuiSession session, GuiInventoryHolder holder) {
    return session.sessionId().equals(holder.sessionId())
        && session.viewId().equals(holder.viewId())
        && session.current().id().equals(holder.menuId());
  }

  private int historyLimit() {
    return settings().historyEnabled() ? settings().maximumHistorySize() : 0;
  }

  private MenuSettings settings() {
    return configurations.snapshot().menus();
  }

  private void runSync(Runnable action) {
    if (Bukkit.isPrimaryThread()) action.run();
    else {
      logger.debug("Rescheduling GUI operation to server thread");
      Bukkit.getScheduler().runTask(plugin, action);
    }
  }

  private void message(Player player, TranslationKey key, PlaceholderContext placeholders) {
    player.sendMessage(configurations.language().message(key, placeholders));
  }

  private void play(Player player, String id) {
    if (!settings().soundsEnabled()) return;
    SoundSettings value = settings().sounds().get(id);
    if (value == null) return;
    try {
      player.playSound(
          player.getLocation(), Sound.valueOf(value.sound()), value.volume(), value.pitch());
    } catch (IllegalArgumentException exception) {
      logger.warning("[GUI] Invalid sound ignored: " + value.sound());
    }
  }

  private static GuiClickType click(ClickType type) {
    return switch (type) {
      case LEFT -> GuiClickType.LEFT;
      case RIGHT -> GuiClickType.RIGHT;
      case SHIFT_LEFT -> GuiClickType.SHIFT_LEFT;
      case SHIFT_RIGHT -> GuiClickType.SHIFT_RIGHT;
      case MIDDLE -> GuiClickType.MIDDLE;
      case DOUBLE_CLICK -> GuiClickType.DOUBLE;
      case NUMBER_KEY -> GuiClickType.NUMBER_KEY;
      case DROP -> GuiClickType.DROP;
      case CONTROL_DROP -> GuiClickType.CONTROL_DROP;
      case CREATIVE -> GuiClickType.CREATIVE;
      default -> GuiClickType.UNKNOWN;
    };
  }

  private final class Runtime implements GuiRuntime {
    private final Player player;

    Runtime(Player player) {
      this.player = player;
    }

    public UUID playerId() {
      return player.getUniqueId();
    }

    public String playerName() {
      return player.getName();
    }

    public boolean hasPermission(String permission) {
      return player.hasPermission(permission);
    }

    public void open(Gui gui) {
      navigate(player, gui, true);
    }

    public void replace(Gui gui) {
      navigate(player, gui, false);
    }

    public void back() {
      sessions
          .find(playerId())
          .ifPresent(
              session -> {
                session.current().closed(session, GuiCloseReason.NAVIGATION);
                session
                    .back()
                    .ifPresentOrElse(
                        ignored -> {
                          renderNewView(player, session, true);
                          play(player, "back");
                        },
                        () -> BukkitGuiService.this.close(player));
              });
    }

    public void root() {
      sessions
          .find(playerId())
          .ifPresent(
              session -> {
                session.current().closed(session, GuiCloseReason.NAVIGATION);
                session.goRoot();
                renderNewView(player, session, true);
              });
    }

    public void refresh() {
      BukkitGuiService.this.refresh(player);
    }

    public void close() {
      BukkitGuiService.this.close(player);
    }

    public void playSound(String soundId) {
      play(player, soundId);
    }

    public void message(TranslationKey key, PlaceholderContext placeholders) {
      BukkitGuiService.this.message(player, key, placeholders);
    }
  }
}
