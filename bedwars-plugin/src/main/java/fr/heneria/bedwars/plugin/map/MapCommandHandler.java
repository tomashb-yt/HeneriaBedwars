package fr.heneria.bedwars.plugin.map;

import fr.heneria.bedwars.core.command.AdministrativeCommandPolicy;
import fr.heneria.bedwars.core.config.PlaceholderContext;
import fr.heneria.bedwars.core.logging.ProjectLogger;
import fr.heneria.bedwars.core.map.MapOperationResult;
import fr.heneria.bedwars.core.map.MapSpawn;
import fr.heneria.bedwars.core.map.MapTemplate;
import fr.heneria.bedwars.core.map.MapTemplateService;
import fr.heneria.bedwars.core.map.MapType;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import fr.heneria.bedwars.plugin.gui.GuiService;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Permission-aware Bukkit command adapter for autonomous map templates. */
public final class MapCommandHandler {
  private final JavaPlugin plugin;
  private final MapTemplateService maps;
  private final BukkitMapWorldService worlds;
  private final ConfigurationService configurations;
  private final GuiService gui;
  private final MapMenuFactory menus;
  private final ProjectLogger logger;

  public MapCommandHandler(
      JavaPlugin plugin,
      MapTemplateService maps,
      BukkitMapWorldService worlds,
      ConfigurationService configurations,
      GuiService gui,
      MapMenuFactory menus,
      ProjectLogger logger) {
    this.plugin = plugin;
    this.maps = maps;
    this.worlds = worlds;
    this.configurations = configurations;
    this.gui = gui;
    this.menus = menus;
    this.logger = logger;
  }

  public boolean execute(CommandSender sender, String[] args) {
    if (args.length < 2 || args[1].equalsIgnoreCase("menu")) return menu(sender);
    if (!allowed(sender, AdministrativeCommandPolicy.MAP)) return true;
    return switch (args[1].toLowerCase(Locale.ROOT)) {
      case "create" -> create(sender, args);
      case "list" -> list(sender);
      case "info" -> info(sender, args);
      case "load" -> load(sender, args);
      case "unload" -> unload(sender, args);
      case "save" -> save(sender, args);
      case "teleport", "tp" -> teleport(sender, args);
      case "setspawn" -> setSpawn(sender, args);
      case "duplicate" -> duplicate(sender, args);
      case "setdisplayname" -> setDisplayName(sender, args);
      case "delete" -> delete(sender, args);
      default -> send(sender, "map.command.help", Map.of());
    };
  }

  private boolean menu(CommandSender sender) {
    if (!allowed(sender, AdministrativeCommandPolicy.MAP_MENU)) return true;
    if (!(sender instanceof Player player))
      return send(sender, "map.command.player-only", Map.of());
    gui.open(player, menus.list(player.getUniqueId()));
    return true;
  }

  private boolean create(CommandSender sender, String[] args) {
    if (!allowed(sender, AdministrativeCommandPolicy.MAP_CREATE)) return true;
    if (args.length < 3 || args.length > 4) return send(sender, "map.command.help", Map.of());
    MapType type;
    try {
      type = args.length == 4 ? MapType.valueOf(args[3].toUpperCase(Locale.ROOT)) : MapType.GENERIC;
    } catch (IllegalArgumentException exception) {
      return send(sender, "map.command.invalid-type", Map.of("map_type", args[3]));
    }
    MapOperationResult result = maps.create(args[2], type, sender.getName());
    if (result.successful() && sender instanceof Player player) {
      MapTemplate template = result.template().orElseThrow();
      var teleport = worlds.teleport(player, template);
      if (!teleport.successful())
        return send(
            sender, "map.command.failed", Map.of("code", "TELEPORT", "detail", teleport.detail()));
    }
    if (result.successful())
      logger.info("[Maps] " + sender.getName() + " created '" + args[2] + "'.");
    return result(sender, result, "map.command.created");
  }

  private boolean list(CommandSender sender) {
    if (!allowed(sender, AdministrativeCommandPolicy.MAP_MENU)) return true;
    String values =
        String.join(
            ", ",
            maps.list().stream()
                .map(map -> map.id() + "[" + map.type() + "/" + map.state() + "]")
                .toList());
    return send(
        sender,
        "map.command.list",
        Map.of("count", maps.list().size(), "maps", values.isEmpty() ? "-" : values));
  }

  private boolean info(CommandSender sender, String[] args) {
    if (!allowed(sender, AdministrativeCommandPolicy.MAP_MENU)) return true;
    MapTemplate template = template(args, 2);
    if (template == null) return notFound(sender, args.length > 2 ? args[2] : "?");
    return send(sender, "map.command.info", MapMenuFactory.placeholders(template));
  }

  private boolean load(CommandSender sender, String[] args) {
    if (!allowed(sender, AdministrativeCommandPolicy.MAP_LOAD)) return true;
    if (args.length != 3) return send(sender, "map.command.help", Map.of());
    return result(sender, maps.load(args[2]), "map.command.loaded");
  }

  private boolean unload(CommandSender sender, String[] args) {
    if (!allowed(sender, AdministrativeCommandPolicy.MAP_UNLOAD)) return true;
    boolean force = args.length == 4 && args[3].equalsIgnoreCase("--force");
    if (args.length < 3 || args.length > 4 || (args.length == 4 && !force))
      return send(sender, "map.command.help", Map.of());
    if (force && !allowed(sender, AdministrativeCommandPolicy.MAP_FORCE)) return true;
    return result(sender, maps.unload(args[2], force), "map.command.unloaded");
  }

  private boolean save(CommandSender sender, String[] args) {
    if (!allowed(sender, AdministrativeCommandPolicy.MAP_SAVE)) return true;
    if (args.length != 3) return send(sender, "map.command.help", Map.of());
    return result(sender, maps.save(args[2]), "map.command.saved");
  }

  private boolean teleport(CommandSender sender, String[] args) {
    if (!allowed(sender, AdministrativeCommandPolicy.MAP_TELEPORT)) return true;
    if (!(sender instanceof Player player))
      return send(sender, "map.command.player-only", Map.of());
    if (args.length != 3) return send(sender, "map.command.help", Map.of());
    MapTemplate template = maps.find(args[2]).orElse(null);
    if (template == null) return notFound(sender, args[2]);
    if (!template.loaded()) {
      MapOperationResult load = maps.load(args[2]);
      if (!load.successful()) return result(sender, load, "map.command.loaded");
      template = load.template().orElseThrow();
    }
    var result = worlds.teleport(player, template);
    return send(
        sender,
        result.successful() ? "map.command.teleported" : "map.command.failed",
        Map.of("map_id", template.id().value(), "code", "TELEPORT", "detail", result.detail()));
  }

  private boolean setSpawn(CommandSender sender, String[] args) {
    if (!allowed(sender, AdministrativeCommandPolicy.MAP_EDIT)) return true;
    if (!(sender instanceof Player player))
      return send(sender, "map.command.player-only", Map.of());
    if (args.length != 3) return send(sender, "map.command.help", Map.of());
    MapTemplate template = maps.find(args[2]).orElse(null);
    if (template == null) return notFound(sender, args[2]);
    if (!player.getWorld().getName().equals(template.worldName()))
      return send(sender, "map.command.invalid-world", Map.of("map_id", template.id().value()));
    var location = player.getLocation();
    return result(
        sender,
        maps.setSpawn(
            template.id().value(),
            new MapSpawn(
                true,
                template.worldName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()),
            template.revision()),
        "map.command.set-spawn");
  }

  private boolean duplicate(CommandSender sender, String[] args) {
    if (!allowed(sender, AdministrativeCommandPolicy.MAP_DUPLICATE)) return true;
    if (args.length != 4) return send(sender, "map.command.help", Map.of());
    MapTemplate source = maps.find(args[2]).orElse(null);
    if (source == null) return notFound(sender, args[2]);
    if (source.loaded()) {
      MapOperationResult saved = maps.save(source.id().value());
      if (!saved.successful()) return result(sender, saved, "map.command.saved");
    }
    send(
        sender,
        "map.command.copy-started",
        Map.of("map_id", source.id().value(), "destination", args[3]));
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            plugin,
            () -> {
              MapOperationResult operation = maps.duplicate(args[2], args[3], sender.getName());
              Bukkit.getScheduler()
                  .runTask(plugin, () -> result(sender, operation, "map.command.duplicated"));
            });
    return true;
  }

  private boolean setDisplayName(CommandSender sender, String[] args) {
    if (!allowed(sender, AdministrativeCommandPolicy.MAP_EDIT)) return true;
    if (args.length < 4) return send(sender, "map.command.help", Map.of());
    MapTemplate template = maps.find(args[2]).orElse(null);
    if (template == null) return notFound(sender, args[2]);
    return result(
        sender,
        maps.setDisplayName(
            args[2],
            String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length)),
            template.revision()),
        "map.command.updated");
  }

  private boolean delete(CommandSender sender, String[] args) {
    if (!allowed(sender, AdministrativeCommandPolicy.MAP_DELETE)) return true;
    MapTemplate template = template(args, 2);
    if (template == null) return notFound(sender, args.length > 2 ? args[2] : "?");
    if (!(sender instanceof Player player))
      return send(sender, "map.command.player-only", Map.of());
    gui.open(player, menus.deleteConfirmation(player.getUniqueId(), template.id().value()));
    return true;
  }

  private MapTemplate template(String[] args, int index) {
    return args.length == index + 1 ? maps.find(args[index]).orElse(null) : null;
  }

  private boolean result(CommandSender sender, MapOperationResult result, String successKey) {
    if (result.successful()) {
      Map<String, Object> values =
          new LinkedHashMap<>(MapMenuFactory.placeholders(result.template().orElseThrow()));
      values.put("code", result.code());
      values.put("detail", result.detail());
      return send(sender, successKey, values);
    }
    String key =
        switch (result.code()) {
          case NOT_FOUND -> "map.command.not-found";
          case ALREADY_EXISTS -> "map.command.already-exists";
          case INVALID_ID -> "map.command.invalid-id";
          case ALREADY_LOADED -> "map.command.already-loaded";
          case NOT_LOADED -> "map.command.already-unloaded";
          case PLAYERS_PRESENT -> "map.command.players-present";
          case MAP_LINKED -> "map.command.linked-arena";
          case BACKUP_FAILED -> "map.command.backup-error";
          case COPY_FAILED -> "map.command.copy-error";
          case INVALID_TYPE -> "map.command.invalid-type";
          default -> "map.command.failed";
        };
    return send(
        sender,
        key,
        Map.of(
            "map_id",
            result.template().map(t -> t.id().value()).orElse(result.detail()),
            "code",
            result.code(),
            "detail",
            result.detail()));
  }

  private boolean notFound(CommandSender sender, String id) {
    return send(sender, "map.command.not-found", Map.of("map_id", id));
  }

  private boolean allowed(CommandSender sender, String permission) {
    if (sender.hasPermission(permission)) return true;
    return send(sender, "general.no-permission", Map.of());
  }

  private boolean send(CommandSender sender, String key, Map<String, ?> values) {
    PlaceholderContext.Builder placeholders = PlaceholderContext.builder();
    values.forEach(placeholders::put);
    sender.sendMessage(
        configurations
            .language()
            .message(key, configurations.snapshot().plugin().locale(), placeholders.build()));
    return true;
  }
}
