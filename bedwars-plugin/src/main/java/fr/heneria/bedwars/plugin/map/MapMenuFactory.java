package fr.heneria.bedwars.plugin.map;

import fr.heneria.bedwars.core.command.AdministrativeCommandPolicy;
import fr.heneria.bedwars.core.config.PlaceholderContext;
import fr.heneria.bedwars.core.gui.ConfirmationGui;
import fr.heneria.bedwars.core.gui.Gui;
import fr.heneria.bedwars.core.gui.GuiButton;
import fr.heneria.bedwars.core.gui.GuiClickType;
import fr.heneria.bedwars.core.gui.TextInputCancelReason;
import fr.heneria.bedwars.core.gui.TextInputRequest;
import fr.heneria.bedwars.core.gui.TextInputService;
import fr.heneria.bedwars.core.logging.ProjectLogger;
import fr.heneria.bedwars.core.map.MapOperationResult;
import fr.heneria.bedwars.core.map.MapState;
import fr.heneria.bedwars.core.map.MapTemplate;
import fr.heneria.bedwars.core.map.MapTemplateService;
import fr.heneria.bedwars.core.map.MapType;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import fr.heneria.bedwars.plugin.gui.GuiService;
import fr.heneria.bedwars.plugin.gui.StandardGuiButtons;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Simple Ticket 007 map list and information surface; all mutations use MapTemplateService. */
public final class MapMenuFactory {
  private static final List<Integer> CONTENT =
      List.of(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34);
  private static final DateTimeFormatter DATE =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
  private final JavaPlugin plugin;
  private final MapTemplateService maps;
  private final BukkitMapWorldService worlds;
  private final ConfigurationService configurations;
  private final GuiService gui;
  private final TextInputService input;
  private final ProjectLogger logger;
  private final StandardGuiButtons standard = new StandardGuiButtons();

  public MapMenuFactory(
      JavaPlugin plugin,
      MapTemplateService maps,
      BukkitMapWorldService worlds,
      ConfigurationService configurations,
      GuiService gui,
      TextInputService input,
      ProjectLogger logger) {
    this.plugin = plugin;
    this.maps = maps;
    this.worlds = worlds;
    this.configurations = configurations;
    this.gui = gui;
    this.input = input;
    this.logger = logger;
  }

  public Gui list(UUID playerId) {
    return list(playerId, 0);
  }

  private Gui list(UUID playerId, int requestedPage) {
    List<MapTemplate> templates = maps.list();
    int maximumPage = Math.max(1, (templates.size() + CONTENT.size() - 1) / CONTENT.size());
    int page = Math.max(0, Math.min(requestedPage, maximumPage - 1));
    Gui.Builder builder =
        Gui.builder()
            .id("map.list")
            .title(message("map.gui.list-title", Map.of("count", templates.size())))
            .rows(6)
            .fillEmptySlots(true);
    for (int index = 0; index < CONTENT.size(); index++) {
      int absolute = page * CONTENT.size() + index;
      if (absolute >= templates.size()) break;
      MapTemplate template = templates.get(absolute);
      builder.button(
          CONTENT.get(index),
          GuiButton.builder()
              .itemKey(entryKey(template))
              .itemPlaceholders(context -> placeholders(template))
              .onLeftClick(context -> context.open(info(playerId, template.id().value())))
              .onRightClick(context -> loadOrTeleport(playerId, template.id().value(), context))
              .on(
                  GuiClickType.SHIFT_RIGHT,
                  context -> context.open(deleteConfirmation(playerId, template.id().value())))
              .build());
    }
    if (page > 0)
      builder.button(46, standard.previous(context -> context.replace(list(playerId, page - 1))));
    if (page + 1 < maximumPage)
      builder.button(52, standard.next(context -> context.replace(list(playerId, page + 1))));
    int currentPage = page;
    return builder
        .button(
            49,
            GuiButton.builder()
                .itemKey("map.list.create")
                .permission(AdministrativeCommandPolicy.MAP_CREATE)
                .onLeftClick(context -> requestCreate(playerId))
                .build())
        .button(
            50,
            GuiButton.builder()
                .itemKey("map.list.refresh")
                .onLeftClick(context -> context.replace(list(playerId, currentPage)))
                .build())
        .button(45, standard.back())
        .button(53, standard.close())
        .build();
  }

  public Gui info(UUID playerId, String id) {
    MapTemplate template = maps.find(id).orElse(null);
    if (template == null) return list(playerId);
    Map<String, Object> values = placeholders(template);
    return Gui.builder()
        .id("map.info." + id)
        .title(message("map.gui.editor-title", values))
        .rows(6)
        .fillEmptySlots(true)
        .button(
            4,
            GuiButton.builder()
                .itemKey("map.editor.information")
                .itemPlaceholders(context -> values)
                .build())
        .button(
            10,
            action(
                "map.editor.load",
                AdministrativeCommandPolicy.MAP_LOAD,
                values,
                context -> mutate(playerId, maps.load(id), context, id)))
        .button(
            12,
            action(
                "map.editor.teleport",
                AdministrativeCommandPolicy.MAP_TELEPORT,
                values,
                context -> loadOrTeleport(playerId, id, context)))
        .button(
            14,
            action(
                "map.editor.save",
                AdministrativeCommandPolicy.MAP_SAVE,
                values,
                context -> mutate(playerId, maps.save(id), context, id)))
        .button(
            16,
            action(
                "map.editor.unload",
                AdministrativeCommandPolicy.MAP_UNLOAD,
                values,
                context -> mutate(playerId, maps.unload(id, false), context, id)))
        .button(
            20,
            action(
                "map.editor.duplicate",
                AdministrativeCommandPolicy.MAP_DUPLICATE,
                values,
                context -> requestDuplicate(playerId, template)))
        .button(
            22,
            action(
                "map.editor.set-spawn",
                AdministrativeCommandPolicy.MAP_EDIT,
                values,
                context -> setSpawn(playerId, template, context)))
        .button(
            24,
            GuiButton.builder()
                .itemKey("map.editor.associations")
                .itemPlaceholders(context -> values)
                .build())
        .button(
            31,
            action(
                "map.editor.delete",
                AdministrativeCommandPolicy.MAP_DELETE,
                values,
                context -> context.open(deleteConfirmation(playerId, id))))
        .button(45, standard.back())
        .button(
            49,
            GuiButton.builder()
                .itemKey("gui.refresh")
                .onLeftClick(context -> context.replace(info(playerId, id)))
                .build())
        .button(53, standard.close())
        .build();
  }

  public Gui deleteConfirmation(UUID playerId, String id) {
    MapTemplate template = maps.find(id).orElse(null);
    if (template == null) return list(playerId);
    return ConfirmationGui.builder()
        .id("map.delete." + id)
        .title(message("map.gui.delete", placeholders(template)))
        .informationKey("map.editor.delete")
        .informationPlaceholders(placeholders(template))
        .confirmItemKey("gui.confirm")
        .cancelItemKey("gui.cancel")
        .permission(AdministrativeCommandPolicy.MAP_DELETE)
        .onConfirm(
            context -> {
              MapOperationResult prepared = maps.prepareDelete(id);
              if (!prepared.successful()) {
                feedback(playerId, prepared);
                return;
              }
              Player player = Bukkit.getPlayer(playerId);
              if (player != null) gui.close(player);
              Bukkit.getScheduler()
                  .runTaskAsynchronously(
                      plugin,
                      () -> {
                        MapOperationResult result = maps.completeDelete(id);
                        Bukkit.getScheduler()
                            .runTask(
                                plugin,
                                () -> {
                                  feedback(playerId, result);
                                  Player onlinePlayer = Bukkit.getPlayer(playerId);
                                  if (onlinePlayer != null) gui.open(onlinePlayer, list(playerId));
                                });
                      });
            })
        .build();
  }

  private void requestCreate(UUID playerId) {
    Player player = Bukkit.getPlayer(playerId);
    if (player == null) return;
    if (!input.begin(
        playerId,
        request(
            playerId,
            "map.gui.create",
            value -> {
              try {
                fr.heneria.bedwars.core.map.MapId.parse(value);
                return maps.find(value).isPresent()
                    ? Optional.of("already exists")
                    : Optional.empty();
              } catch (RuntimeException exception) {
                return Optional.of("invalid id");
              }
            },
            value -> {
              MapOperationResult result = maps.create(value, MapType.GENERIC, player.getName());
              feedback(playerId, result);
              result
                  .template()
                  .ifPresent(template -> gui.open(player, info(playerId, template.id().value())));
            },
            reason -> gui.open(player, list(playerId))))) return;
    gui.close(player);
    send(playerId, "map.gui.create", Map.of());
  }

  private void requestDuplicate(UUID playerId, MapTemplate source) {
    Player player = Bukkit.getPlayer(playerId);
    if (player == null) return;
    if (!input.begin(
        playerId,
        request(
            playerId,
            "map.gui.duplicate",
            value -> {
              try {
                fr.heneria.bedwars.core.map.MapId.parse(value);
                return maps.find(value).isPresent()
                    ? Optional.of("already exists")
                    : Optional.empty();
              } catch (RuntimeException exception) {
                return Optional.of("invalid id");
              }
            },
            value -> duplicateAsync(playerId, source, value),
            reason -> gui.open(player, info(playerId, source.id().value()))))) return;
    gui.close(player);
    send(playerId, "map.gui.duplicate", placeholders(source));
  }

  private void duplicateAsync(UUID playerId, MapTemplate source, String destination) {
    if (source.loaded()) {
      MapOperationResult saved = maps.save(source.id().value());
      if (!saved.successful()) {
        feedback(playerId, saved);
        return;
      }
    }
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            plugin,
            () -> {
              MapOperationResult result =
                  maps.duplicate(source.id().value(), destination, playerName(playerId));
              if (result.successful())
                logger.info(
                    "[Maps] "
                        + playerName(playerId)
                        + " duplicated '"
                        + source.id()
                        + "' to '"
                        + destination
                        + "'.");
              Bukkit.getScheduler()
                  .runTask(
                      plugin,
                      () -> {
                        feedback(playerId, result);
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null)
                          gui.open(
                              player,
                              result.successful()
                                  ? info(playerId, destination)
                                  : info(playerId, source.id().value()));
                      });
            });
  }

  private TextInputRequest request(
      UUID playerId,
      String prompt,
      java.util.function.Function<String, Optional<String>> validator,
      java.util.function.Consumer<String> success,
      java.util.function.Consumer<TextInputCancelReason> cancel) {
    return new TextInputRequest(
        prompt,
        configurations.snapshot().menus().textInput().timeout(),
        32,
        configurations.snapshot().menus().textInput().cancelKeywords(),
        validator,
        success,
        problem -> send(playerId, "map.command.invalid-id", Map.of("detail", problem)),
        reason -> {
          if (reason == TextInputCancelReason.PLAYER || reason == TextInputCancelReason.TIMEOUT)
            cancel.accept(reason);
        });
  }

  private void loadOrTeleport(
      UUID playerId, String id, fr.heneria.bedwars.core.gui.GuiClickContext context) {
    MapTemplate template = maps.find(id).orElse(null);
    if (template == null) return;
    if (!template.loaded()) {
      MapOperationResult load = maps.load(id);
      if (!load.successful()) {
        feedback(playerId, load);
        return;
      }
      template = load.template().orElseThrow();
    }
    Player player = Bukkit.getPlayer(playerId);
    if (player != null) {
      var result = worlds.teleport(player, template);
      send(
          playerId,
          result.successful() ? "map.command.teleported" : "map.command.storage-error",
          Map.of("map_id", id, "detail", result.detail()));
    }
    context.replace(info(playerId, id));
  }

  private void setSpawn(
      UUID playerId, MapTemplate template, fr.heneria.bedwars.core.gui.GuiClickContext context) {
    Player player = Bukkit.getPlayer(playerId);
    if (player == null || !player.getWorld().getName().equals(template.worldName())) {
      send(playerId, "map.command.invalid-world", Map.of("map_id", template.id().value()));
      return;
    }
    var location = player.getLocation();
    MapOperationResult result =
        maps.setSpawn(
            template.id().value(),
            new fr.heneria.bedwars.core.map.MapSpawn(
                true,
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()),
            template.revision());
    mutate(playerId, result, context, template.id().value());
  }

  private GuiButton action(
      String key,
      String permission,
      Map<String, ?> values,
      fr.heneria.bedwars.core.gui.GuiAction action) {
    return GuiButton.builder()
        .itemKey(key)
        .itemPlaceholders(context -> values)
        .permission(permission)
        .onLeftClick(action)
        .build();
  }

  private void mutate(
      UUID playerId,
      MapOperationResult result,
      fr.heneria.bedwars.core.gui.GuiClickContext context,
      String id) {
    feedback(playerId, result);
    context.replace(info(playerId, id));
  }

  private void feedback(UUID playerId, MapOperationResult result) {
    String key = result.successful() ? "map.command.success" : "map.command.failed";
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("code", result.code());
    values.put("detail", result.detail());
    result.template().ifPresent(template -> values.putAll(placeholders(template)));
    send(playerId, key, values);
  }

  private static String entryKey(MapTemplate template) {
    if (template.state() == MapState.ERROR) return "map.list.entry-error";
    return template.loaded() ? "map.list.entry-loaded" : "map.list.entry-unloaded";
  }

  public static Map<String, Object> placeholders(MapTemplate template) {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("map_id", template.id().value());
    values.put("map_name", template.displayName());
    values.put("map_type", template.type());
    values.put("map_state", template.state());
    values.put("world_name", template.worldName());
    values.put("folder_name", template.folderName());
    values.put("loaded", template.loaded());
    values.put(
        "player_count",
        Bukkit.getWorld(template.worldName()) == null
            ? 0
            : Bukkit.getWorld(template.worldName()).getPlayers().size());
    values.put("arena_count", template.linkedArenaIds().size());
    values.put("linked_arenas", String.join(", ", template.linkedArenaIds()));
    values.put("revision", template.revision());
    values.put("last_saved_at", template.lastSavedAt().map(DATE::format).orElse("-"));
    values.put("author", template.author().isBlank() ? "-" : template.author());
    values.put("description", template.description().isBlank() ? "-" : template.description());
    return Map.copyOf(values);
  }

  private String message(String key, Map<String, ?> values) {
    PlaceholderContext.Builder placeholders = PlaceholderContext.builder();
    values.forEach(placeholders::put);
    return configurations
        .language()
        .message(key, configurations.snapshot().plugin().locale(), placeholders.build());
  }

  private void send(UUID playerId, String key, Map<String, ?> values) {
    Player player = Bukkit.getPlayer(playerId);
    if (player != null) player.sendMessage(message(key, values));
  }

  private static String playerName(UUID playerId) {
    Player player = Bukkit.getPlayer(playerId);
    return player == null ? playerId.toString() : player.getName();
  }
}
