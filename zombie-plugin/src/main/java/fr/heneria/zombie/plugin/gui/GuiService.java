package fr.heneria.zombie.plugin.gui;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Central lifecycle, navigation, click routing and refresh engine for every plugin GUI. */
public final class GuiService implements GuiInputProvider {

  private static final MiniMessage MINI = MiniMessage.miniMessage();
  private final JavaPlugin plugin;
  private final GuiRegistry registry;
  private final GuiActionRegistry actions;
  private final GuiConfigurationService configurations;
  private final Clock clock;
  private final Duration sessionTimeout;
  private final IntSupplier inputTimeoutSeconds;
  private final BooleanSupplier soundsEnabled;
  private final Map<UUID, GuiSession> sessions = new ConcurrentHashMap<>();
  private final BukkitTask maintenance;
  private long tick;

  public GuiService(
      JavaPlugin plugin,
      GuiRegistry registry,
      GuiActionRegistry actions,
      GuiConfigurationService configurations,
      Clock clock,
      Duration sessionTimeout,
      IntSupplier inputTimeoutSeconds,
      BooleanSupplier soundsEnabled) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.registry = Objects.requireNonNull(registry, "registry");
    this.actions = Objects.requireNonNull(actions, "actions");
    this.configurations = Objects.requireNonNull(configurations, "configurations");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.sessionTimeout = Objects.requireNonNull(sessionTimeout, "sessionTimeout");
    this.inputTimeoutSeconds = Objects.requireNonNull(inputTimeoutSeconds, "inputTimeoutSeconds");
    this.soundsEnabled = Objects.requireNonNull(soundsEnabled, "soundsEnabled");
    maintenance = plugin.getServer().getScheduler().runTaskTimer(plugin, this::maintain, 20L, 20L);
    configurations.addReloadListener(
        () -> plugin.getServer().getScheduler().runTask(plugin, this::reopenAll));
  }

  public void openHome(Player player, GuiId id) {
    GuiSession session =
        sessions.computeIfAbsent(player.getUniqueId(), key -> new GuiSession(key, clock.instant()));
    session.history().clear();
    open(player, id, GuiContext.EMPTY, true, false);
  }

  public void open(Player player, GuiId id) {
    open(player, id, GuiContext.EMPTY, false, true);
  }

  public void back(Player player) {
    GuiSession session = sessions.get(player.getUniqueId());
    if (session == null) {
      return;
    }
    session
        .history()
        .pop()
        .ifPresentOrElse(
            entry -> open(player, entry.id(), entry.context(), false, false),
            () ->
                session
                    .homeGui()
                    .ifPresent(home -> open(player, home, GuiContext.EMPTY, false, false)));
  }

  public void home(Player player) {
    Optional.ofNullable(sessions.get(player.getUniqueId()))
        .flatMap(GuiSession::homeGui)
        .ifPresent(home -> open(player, home, GuiContext.EMPTY, false, false));
  }

  public void refresh(Player player) {
    GuiSession session = sessions.get(player.getUniqueId());
    if (session == null || session.currentGui().isEmpty()) {
      return;
    }
    Gui gui = registry.find(session.currentGui().orElseThrow()).orElse(null);
    if (gui == null
        || !(player.getOpenInventory().getTopInventory().getHolder()
            instanceof GuiInventoryHolder holder)
        || !holder.token().equals(session.viewToken())) {
      return;
    }
    render(player, session, gui, holder);
  }

  public void confirm(Player player, GuiConfirmation confirmation) {
    GuiSession session = sessions.get(player.getUniqueId());
    if (session == null) {
      return;
    }
    session.confirmation(confirmation);
    open(player, new GuiId("confirmation"));
  }

  @Override
  public void requestInput(Player player, GuiInputRequest request) {
    GuiSession session = sessions.get(player.getUniqueId());
    if (session == null) {
      return;
    }
    session.inputRequest(request);
    player.closeInventory();
    player.sendMessage(request.prompt());
    player.sendMessage(MINI.deserialize("<gray>Écrivez <yellow>annuler</yellow> pour revenir."));
  }

  public boolean submitInput(Player player, String value) {
    GuiSession session = sessions.get(player.getUniqueId());
    if (session == null || session.inputRequest().isEmpty()) {
      return false;
    }
    GuiInputRequest request = session.inputRequest().orElseThrow();
    if (clock.instant().isAfter(request.expiresAt())) {
      session.inputRequest(null);
      request.cancelled().run();
      reopenCurrent(player, session);
      return true;
    }
    if ("annuler".equalsIgnoreCase(value.strip())) {
      session.inputRequest(null);
      request.cancelled().run();
      reopenCurrent(player, session);
      return true;
    }
    GuiInputRequest.Validation validation = request.validator().apply(value);
    if (!validation.accepted()) {
      player.sendMessage(validation.message());
      return true;
    }
    session.inputRequest(null);
    request.accepted().accept(value);
    reopenCurrent(player, session);
    return true;
  }

  public GuiSession session(Player player) {
    return sessions.get(player.getUniqueId());
  }

  public int openSessionCount() {
    return sessions.size();
  }

  public void handleClick(InventoryClickEvent event) {
    if (!(event.getView().getTopInventory().getHolder() instanceof GuiInventoryHolder holder)) {
      return;
    }
    event.setCancelled(true);
    if (!(event.getWhoClicked() instanceof Player player)
        || event.getRawSlot() < 0
        || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
      return;
    }
    GuiSession session = sessions.get(player.getUniqueId());
    if (session == null
        || !holder.playerId().equals(player.getUniqueId())
        || !holder.token().equals(session.viewToken())) {
      return;
    }
    GuiButton button = holder.button(event.getRawSlot());
    if (button == null) {
      return;
    }
    if (!button.permission().isBlank() && !player.hasPermission(button.permission())) {
      player.sendMessage(MINI.deserialize("<red>Vous n'avez pas la permission."));
      return;
    }
    Instant clickedAt = clock.instant();
    if (!session.tryClick(clickedAt, Duration.ofMillis(150))) {
      return;
    }
    session.touch(clickedAt);
    try {
      if (soundsEnabled.getAsBoolean()) {
        button
            .clickSound()
            .ifPresent(sound -> player.playSound(player.getLocation(), sound, 0.7F, 1.0F));
      }
      button
          .action(event.isShiftClick(), event.isRightClick())
          .ifPresent(
              action ->
                  action.execute(
                      new GuiClickContext(
                          player, this, session, event.getClick(), event.getRawSlot())));
    } catch (RuntimeException failure) {
      plugin.getLogger().severe("GUI action failed: " + failure.getMessage());
      player.sendMessage(MINI.deserialize("<red>Cette action a échoué. Consultez la console."));
    }
  }

  public void handleDrag(InventoryDragEvent event) {
    if (event.getView().getTopInventory().getHolder() instanceof GuiInventoryHolder) {
      event.setCancelled(true);
    }
  }

  public void handleClose(InventoryCloseEvent event) {
    if (!(event.getInventory().getHolder() instanceof GuiInventoryHolder holder)) {
      return;
    }
    GuiSession session = sessions.get(event.getPlayer().getUniqueId());
    if (session != null
        && holder.token().equals(session.viewToken())
        && session.inputRequest().isEmpty()) {
      sessions.remove(event.getPlayer().getUniqueId(), session);
      session.clear();
    }
  }

  public void remove(UUID playerId) {
    GuiSession session = sessions.remove(playerId);
    if (session != null) {
      session.inputRequest().ifPresent(request -> request.cancelled().run());
      session.clear();
    }
  }

  public void shutdown() {
    maintenance.cancel();
    for (Player player : plugin.getServer().getOnlinePlayers()) {
      if (player.getOpenInventory().getTopInventory().getHolder() instanceof GuiInventoryHolder) {
        player.closeInventory();
      }
    }
    sessions.values().forEach(GuiSession::clear);
    sessions.clear();
  }

  private void open(
      Player player, GuiId id, GuiContext context, boolean home, boolean rememberCurrent) {
    Gui gui =
        registry
            .find(id)
            .orElseThrow(() -> new IllegalArgumentException("Unknown GUI " + id.value()));
    GuiSession session =
        sessions.computeIfAbsent(player.getUniqueId(), key -> new GuiSession(key, clock.instant()));
    if (rememberCurrent) {
      session
          .currentGui()
          .ifPresent(current -> session.history().push(current, session.currentContext()));
    }
    session.activate(id, context, home);
    session.touch(clock.instant());
    GuiMenuTemplate menu =
        configurations
            .current()
            .menu(id)
            .orElseThrow(() -> new IllegalStateException("Missing GUI template " + id.value()));
    GuiInventoryHolder holder =
        new GuiInventoryHolder(
            player.getUniqueId(), session.viewToken(), id, gui.size(context), gui.title(context));
    render(player, session, gui, holder);
    player.openInventory(holder.getInventory());
  }

  private void render(Player player, GuiSession session, Gui gui, GuiInventoryHolder holder) {
    GuiMenuTemplate menu = configurations.current().menu(gui.id()).orElseThrow();
    holder.reset();
    GuiView view = new GuiView(player, holder, menu, configurations.current().theme(menu), actions);
    view.background();
    gui.render(view, session.currentContext());
    int refresh = gui.refreshTicks(session.currentContext());
    session.nextRefreshTick(refresh <= 0 ? Long.MAX_VALUE : tick + refresh);
  }

  private void maintain() {
    tick += 20;
    Instant now = clock.instant();
    for (GuiSession session : sessions.values()) {
      Player player = plugin.getServer().getPlayer(session.playerId());
      if (player == null || !player.isOnline()) {
        remove(session.playerId());
      } else if (Duration.between(session.lastActivity(), now).compareTo(sessionTimeout) > 0) {
        player.closeInventory();
        remove(session.playerId());
      } else if (session
          .inputRequest()
          .filter(request -> now.isAfter(request.expiresAt()))
          .isPresent()) {
        session.inputRequest().orElseThrow().cancelled().run();
        session.inputRequest(null);
        reopenCurrent(player, session);
      } else if (tick >= session.nextRefreshTick()) {
        refresh(player);
      }
    }
  }

  private void reopenAll() {
    sessions.keySet().stream()
        .map(plugin.getServer()::getPlayer)
        .filter(Objects::nonNull)
        .forEach(
            player -> {
              GuiSession session = sessions.get(player.getUniqueId());
              if (session != null) {
                reopenCurrent(player, session);
              }
            });
  }

  public Instant inputExpiry() {
    return clock.instant().plusSeconds(Math.max(1, inputTimeoutSeconds.getAsInt()));
  }

  private void reopenCurrent(Player player, GuiSession session) {
    session.currentGui().ifPresent(id -> open(player, id, session.currentContext(), false, false));
  }
}
