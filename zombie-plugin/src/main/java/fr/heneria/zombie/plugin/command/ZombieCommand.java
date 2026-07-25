package fr.heneria.zombie.plugin.command;

import fr.heneria.zombie.api.PluginState;
import fr.heneria.zombie.api.ZombieApi;
import fr.heneria.zombie.core.command.ZombieCommandAction;
import fr.heneria.zombie.core.command.ZombieCommandParser;
import fr.heneria.zombie.core.instance.GameInstanceSnapshot;
import fr.heneria.zombie.plugin.config.ConfigurationManager;
import fr.heneria.zombie.plugin.config.ReloadResult;
import fr.heneria.zombie.plugin.instance.InstanceCoordinator;
import fr.heneria.zombie.plugin.instance.PlayerInstanceResult;
import fr.heneria.zombie.plugin.map.MapPreviewService;
import fr.heneria.zombie.plugin.map.MapTemplateCatalog;
import fr.heneria.zombie.plugin.message.MessageService;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Paper adapter for lobby and isolated-instance commands. */
public final class ZombieCommand implements CommandExecutor, TabCompleter {

  private static final String ADMIN_PERMISSION = "zombie.instance.admin";
  private final String version;
  private final ZombieApi api;
  private final AtomicReference<PluginState> state;
  private final ZombieCommandParser parser = new ZombieCommandParser();
  private final ConfigurationManager configurations;
  private final MessageService messages;
  private final InstanceCoordinator coordinator;
  private final MapTemplateCatalog templates;
  private final MapPreviewService previews;
  private final Executor mainThread;

  /**
   * Creates the command adapter.
   *
   * @param version plugin version
   * @param api public status API
   * @param state shared lifecycle state
   * @param configurations configuration manager
   * @param messages message renderer
   * @param coordinator instance coordinator
   * @param templates uploaded map catalog
   * @param previews map preview lifecycle
   * @param mainThread Paper main-thread executor
   */
  public ZombieCommand(
      String version,
      ZombieApi api,
      AtomicReference<PluginState> state,
      ConfigurationManager configurations,
      MessageService messages,
      InstanceCoordinator coordinator,
      MapTemplateCatalog templates,
      MapPreviewService previews,
      Executor mainThread) {
    this.version = Objects.requireNonNull(version, "version");
    this.api = Objects.requireNonNull(api, "api");
    this.state = Objects.requireNonNull(state, "state");
    this.configurations = Objects.requireNonNull(configurations, "configurations");
    this.messages = Objects.requireNonNull(messages, "messages");
    this.coordinator = Objects.requireNonNull(coordinator, "coordinator");
    this.templates = Objects.requireNonNull(templates, "templates");
    this.previews = Objects.requireNonNull(previews, "previews");
    this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
  }

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] arguments) {
    if (!sender.hasPermission("zombie.command.use")) {
      sender.sendMessage(messages.render("command.no-permission"));
      return true;
    }
    ZombieCommandAction action = parser.parse(arguments);
    if (isAdministrative(action) && !sender.hasPermission(ADMIN_PERMISSION)) {
      sender.sendMessage(messages.render("command.no-permission"));
      return true;
    }
    switch (action) {
      case INFORMATION -> showInformation(sender);
      case HELP -> sender.sendMessage(messages.render("command.help"));
      case RELOAD -> reload(sender);
      case LOBBY -> withPlayer(sender, player -> returnToLobby(sender, player, false));
      case MAP_LIST -> listMaps(sender);
      case MAP_PREVIEW -> preview(sender, arguments[2]);
      case MAP_LEAVE -> withPlayer(sender, player -> returnToLobby(sender, player, true));
      case INSTANCE_CREATE -> create(sender, arguments[2]);
      case INSTANCE_LIST -> list(sender);
      case INSTANCE_JOIN -> join(sender, arguments[2]);
      case INSTANCE_LEAVE -> withPlayer(sender, player -> returnToLobby(sender, player, false));
      case INSTANCE_STOP -> stop(sender, arguments[2]);
      case INSTANCE_INFO -> info(sender, arguments[2]);
      case UNKNOWN -> sender.sendMessage(messages.render("command.usage"));
    }
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String alias,
      @NotNull String[] arguments) {
    if (arguments.length == 1) {
      return filter(
          sender.hasPermission(ADMIN_PERMISSION)
              ? List.of("help", "lobby", "map", "instance", "reload")
              : List.of("help", "lobby", "instance"),
          arguments[0]);
    }
    if (arguments.length == 2 && arguments[0].equalsIgnoreCase("map")) {
      return filter(List.of("list", "preview", "leave"), arguments[1]);
    }
    if (arguments.length == 2 && arguments[0].equalsIgnoreCase("instance")) {
      return filter(
          sender.hasPermission(ADMIN_PERMISSION)
              ? List.of("create", "list", "join", "leave", "stop", "info")
              : List.of("join", "leave"),
          arguments[1]);
    }
    if (arguments.length == 3
        && arguments[0].equalsIgnoreCase("map")
        && arguments[1].equalsIgnoreCase("preview")) {
      return filter(templates.knownMapIds().stream().sorted().toList(), arguments[2]);
    }
    if (arguments.length == 3
        && arguments[0].equalsIgnoreCase("instance")
        && List.of("join", "stop", "info").contains(arguments[1].toLowerCase(Locale.ROOT))) {
      return filter(
          coordinator.activeInstances().stream().map(snapshot -> shortId(snapshot.id())).toList(),
          arguments[2]);
    }
    return List.of();
  }

  private void create(CommandSender sender, String mapId) {
    coordinator
        .create(mapId.toLowerCase(Locale.ROOT), Optional.empty())
        .whenCompleteAsync(
            (created, failure) -> {
              if (failure != null) {
                sender.sendMessage(
                    messages.render(
                        "command.instance-create-failure", "reason", safeFailureMessage(failure)));
                return;
              }
              sender.sendMessage(
                  messages.render(
                      "command.instance-created",
                      "id",
                      shortId(created.id()),
                      "map",
                      created.mapId()));
            },
            mainThread);
  }

  private void listMaps(CommandSender sender) {
    templates
        .discover()
        .whenCompleteAsync(
            (maps, failure) -> {
              if (failure != null) {
                sender.sendMessage(
                    messages.render(
                        "command.map-list-failure", "reason", safeFailureMessage(failure)));
                return;
              }
              sender.sendMessage(
                  messages.render(
                      "command.map-list-header", "count", Integer.toString(maps.size())));
              maps.forEach(
                  mapId ->
                      sender.sendMessage(messages.render("command.map-list-entry", "map", mapId)));
            },
            mainThread);
  }

  private void preview(CommandSender sender, String mapId) {
    withPlayer(
        sender,
        player -> {
          coordinator.leave(player);
          previews
              .open(player, mapId.toLowerCase(Locale.ROOT))
              .whenCompleteAsync(
                  (opened, failure) ->
                      sender.sendMessage(
                          failure == null
                              ? messages.render("command.map-preview-opened", "map", opened)
                              : messages.render(
                                  "command.map-preview-failure",
                                  "reason",
                                  safeFailureMessage(failure))),
                  mainThread);
        });
  }

  private void returnToLobby(CommandSender sender, Player player, boolean mapCommand) {
    previews
        .leave(player)
        .whenCompleteAsync(
            (leftPreview, failure) -> {
              if (failure != null) {
                sender.sendMessage(
                    messages.render(
                        "command.map-preview-failure", "reason", safeFailureMessage(failure)));
                return;
              }
              if (!leftPreview) {
                coordinator.leave(player);
              }
              sender.sendMessage(
                  messages.render(
                      mapCommand && leftPreview
                          ? "command.map-preview-left"
                          : "command.lobby-success"));
            },
            mainThread);
  }

  private void list(CommandSender sender) {
    Collection<GameInstanceSnapshot> active = coordinator.activeInstances();
    sender.sendMessage(
        messages.render("command.instance-list-header", "count", Integer.toString(active.size())));
    for (GameInstanceSnapshot instance : active) {
      sender.sendMessage(renderInstance("command.instance-list-entry", instance));
    }
  }

  private void join(CommandSender sender, String identifier) {
    withPlayer(
        sender,
        player ->
            previews
                .leave(player)
                .whenCompleteAsync(
                    (ignored, previewFailure) -> {
                      if (previewFailure != null) {
                        sender.sendMessage(
                            messages.render(
                                "command.instance-join-failure",
                                "reason",
                                safeFailureMessage(previewFailure)));
                        return;
                      }
                      joinResolved(sender, player, identifier);
                    },
                    mainThread));
  }

  private void joinResolved(CommandSender sender, Player player, String identifier) {
    Optional<GameInstanceSnapshot> resolved = resolve(identifier);
    if (resolved.isEmpty()) {
      sender.sendMessage(messages.render("command.instance-not-found"));
      return;
    }
    coordinator
        .join(player, resolved.get().id())
        .whenCompleteAsync(
            (result, failure) -> {
              if (failure != null) {
                sender.sendMessage(
                    messages.render(
                        "command.instance-join-failure", "reason", safeFailureMessage(failure)));
              } else if (result == PlayerInstanceResult.SUCCESS) {
                sender.sendMessage(
                    messages.render("command.instance-joined", "id", shortId(resolved.get().id())));
              } else {
                sender.sendMessage(
                    messages.render("command.instance-join-refused", "reason", result.name()));
              }
            },
            mainThread);
  }

  private void stop(CommandSender sender, String identifier) {
    Optional<GameInstanceSnapshot> resolved = resolve(identifier);
    if (resolved.isEmpty()) {
      sender.sendMessage(messages.render("command.instance-not-found"));
      return;
    }
    coordinator
        .stop(resolved.get().id())
        .whenCompleteAsync(
            (stopped, failure) -> {
              if (failure != null || !Boolean.TRUE.equals(stopped)) {
                sender.sendMessage(
                    messages.render(
                        "command.instance-stop-failure",
                        "reason",
                        failure == null ? "cleanup not confirmed" : safeFailureMessage(failure)));
              } else {
                sender.sendMessage(
                    messages.render(
                        "command.instance-stopped", "id", shortId(resolved.get().id())));
              }
            },
            mainThread);
  }

  private void info(CommandSender sender, String identifier) {
    Optional<GameInstanceSnapshot> resolved = resolve(identifier);
    if (resolved.isEmpty()) {
      sender.sendMessage(messages.render("command.instance-not-found"));
      return;
    }
    sender.sendMessage(renderInstance("command.instance-info", resolved.get()));
  }

  private net.kyori.adventure.text.Component renderInstance(
      String key, GameInstanceSnapshot instance) {
    return messages.render(
        key,
        "id",
        shortId(instance.id()),
        "map",
        instance.mapId(),
        "state",
        instance.state().name(),
        "players",
        Integer.toString(instance.players().size()),
        "maximum",
        Integer.toString(instance.maximumPlayers()));
  }

  private Optional<GameInstanceSnapshot> resolve(String identifier) {
    String normalized = identifier.toLowerCase(Locale.ROOT);
    List<GameInstanceSnapshot> matches =
        coordinator.activeInstances().stream()
            .filter(instance -> instance.id().toString().startsWith(normalized))
            .limit(2)
            .toList();
    return matches.size() == 1 ? Optional.of(matches.getFirst()) : Optional.empty();
  }

  private void showInformation(CommandSender sender) {
    sender.sendMessage(
        messages.render(
            "command.information",
            "version",
            version,
            "state",
            api.state().name(),
            "maps",
            Integer.toString(api.registeredMapCount()),
            "instances",
            Integer.toString(api.activeInstanceCount())));
  }

  private void reload(CommandSender sender) {
    if (!sender.hasPermission("zombie.command.reload")) {
      sender.sendMessage(messages.render("command.no-permission"));
      return;
    }
    if (!coordinator.activeInstances().isEmpty()
        || previews.hasActivePreviews()
        || !state.compareAndSet(PluginState.RUNNING, PluginState.RELOADING)) {
      sender.sendMessage(messages.render("command.reload-busy"));
      return;
    }
    try {
      ReloadResult result = configurations.reload();
      sender.sendMessage(
          messages.render(
              result.successful() ? "command.reload-success" : "command.reload-failure",
              result.successful() ? "warnings" : "errors",
              Integer.toString(result.issues().size())));
    } finally {
      state.compareAndSet(PluginState.RELOADING, PluginState.RUNNING);
    }
  }

  private void withPlayer(CommandSender sender, java.util.function.Consumer<Player> action) {
    if (sender instanceof Player player) {
      if (!player.hasPermission("zombie.play")) {
        sender.sendMessage(messages.render("command.no-permission"));
        return;
      }
      action.accept(player);
    } else {
      sender.sendMessage(messages.render("command.player-only"));
    }
  }

  private static boolean isAdministrative(ZombieCommandAction action) {
    return switch (action) {
      case INSTANCE_CREATE, INSTANCE_LIST, INSTANCE_STOP, INSTANCE_INFO -> true;
      case MAP_LIST, MAP_PREVIEW, MAP_LEAVE -> true;
      default -> false;
    };
  }

  private static List<String> filter(List<String> values, String input) {
    String prefix = input.toLowerCase(Locale.ROOT);
    return values.stream().filter(value -> value.startsWith(prefix)).toList();
  }

  private static String shortId(UUID id) {
    return id.toString().substring(0, 8);
  }

  private static String safeFailureMessage(Throwable failure) {
    Throwable cause =
        failure instanceof CompletionException && failure.getCause() != null
            ? failure.getCause()
            : failure;
    String message = cause.getMessage();
    return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
  }
}
