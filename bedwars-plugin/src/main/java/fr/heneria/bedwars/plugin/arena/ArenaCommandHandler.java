package fr.heneria.bedwars.plugin.arena;

import fr.heneria.bedwars.core.arena.ArenaDefinition;
import fr.heneria.bedwars.core.arena.ArenaLocation;
import fr.heneria.bedwars.core.arena.ArenaOperationCode;
import fr.heneria.bedwars.core.arena.ArenaOperationResult;
import fr.heneria.bedwars.core.arena.ArenaService;
import fr.heneria.bedwars.core.arena.ArenaValidationResult;
import fr.heneria.bedwars.core.arena.TeamId;
import fr.heneria.bedwars.core.command.AdministrativeCommandPolicy;
import fr.heneria.bedwars.core.config.PlaceholderContext;
import fr.heneria.bedwars.core.config.TranslationKey;
import fr.heneria.bedwars.core.map.MapTemplateService;
import fr.heneria.bedwars.core.map.MapType;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import fr.heneria.bedwars.plugin.gui.GuiService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Thin Bukkit command adapter for the pure arena service. */
public final class ArenaCommandHandler {
  private final ArenaService arenas;
  private final ConfigurationService configurations;
  private final GuiService gui;
  private final ArenaEditorMenuFactory menus;
  private final MapTemplateService maps;

  public ArenaCommandHandler(
      ArenaService arenas,
      ConfigurationService configurations,
      GuiService gui,
      ArenaEditorMenuFactory menus,
      MapTemplateService maps) {
    this.arenas = arenas;
    this.configurations = configurations;
    this.gui = gui;
    this.menus = menus;
    this.maps = maps;
  }

  public boolean execute(CommandSender sender, String[] args) {
    if (args.length < 2 || args[1].equalsIgnoreCase("menu")) return menu(sender);
    if (!allowed(sender, AdministrativeCommandPolicy.ARENA)) return true;
    return switch (args[1].toLowerCase(Locale.ROOT)) {
      case "create" -> create(sender, args);
      case "list" -> list(sender);
      case "info" -> info(sender, args);
      case "setworld" -> setWorld(sender, args);
      case "setmap" -> setMap(sender, args);
      case "setwaiting" -> setLocation(sender, args, true);
      case "setspectator" -> setLocation(sender, args, false);
      case "setplayers" -> setPlayers(sender, args);
      case "setteams" -> setTeams(sender, args);
      case "team" -> team(sender, args);
      case "validate" -> validate(sender, args);
      case "enable" -> status(sender, args, true);
      case "disable" -> status(sender, args, false);
      case "delete" -> delete(sender, args);
      default -> send(sender, TranslationKey.ARENA_HELP);
    };
  }

  /** Completes the full nested arena command, including the team setup actions. */
  public List<String> complete(CommandSender sender, String[] args) {
    List<String> choices = new ArrayList<>();
    if (args.length == 2 && sender.hasPermission(AdministrativeCommandPolicy.ARENA)) {
      choices.addAll(
          List.of(
              "create",
              "list",
              "info",
              "menu",
              "setworld",
              "setmap",
              "setwaiting",
              "setspectator",
              "setplayers",
              "setteams",
              "team",
              "validate",
              "enable",
              "disable",
              "delete"));
    } else if (args.length == 3
        && args[1].equalsIgnoreCase("team")
        && sender.hasPermission(AdministrativeCommandPolicy.ARENA_EDIT)) {
      choices.addAll(
          List.of(
              "list", "setspawn", "clearspawn", "teleport", "setbed", "clearbed", "teleportbed"));
    } else if (args.length == 3
        && !args[1].equalsIgnoreCase("create")
        && !args[1].equalsIgnoreCase("list")
        && !args[1].equalsIgnoreCase("menu")) {
      choices.addAll(arenaIds());
    } else if (args.length == 4 && args[1].equalsIgnoreCase("team")) {
      choices.addAll(arenaIds());
    } else if (args.length == 4 && args[1].equalsIgnoreCase("setworld")) {
      choices.addAll(
          org.bukkit.Bukkit.getWorlds().stream().map(org.bukkit.World::getName).sorted().toList());
    } else if (args.length == 4 && args[1].equalsIgnoreCase("setmap")) {
      choices.addAll(maps.list().stream().map(map -> map.id().value()).sorted().toList());
    } else if (args.length == 5 && args[1].equalsIgnoreCase("team")) {
      arenas
          .find(args[3])
          .ifPresent(
              arena ->
                  choices.addAll(
                      arena.teams().stream().map(team -> team.id().value()).sorted().toList()));
    }
    String input = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
    return choices.stream()
        .filter(choice -> choice.toLowerCase(Locale.ROOT).startsWith(input))
        .toList();
  }

  private List<String> arenaIds() {
    return arenas.list().stream().map(arena -> arena.id().value()).sorted().toList();
  }

  private boolean create(CommandSender sender, String[] args) {
    if (!allowed(sender, AdministrativeCommandPolicy.ARENA_CREATE)) return true;
    if (args.length != 3) return send(sender, TranslationKey.ARENA_HELP);
    return result(sender, arenas.create(args[2]), TranslationKey.ARENA_CREATED);
  }

  private boolean list(CommandSender sender) {
    if (!allowed(sender, AdministrativeCommandPolicy.ARENA_LIST)) return true;
    String ids =
        String.join(
            ", ",
            arenas.list().stream().map(arena -> arena.id() + "[" + arena.status() + "]").toList());
    return send(
        sender,
        TranslationKey.ARENA_LIST,
        PlaceholderContext.builder()
            .put("count", arenas.list().size())
            .put("arenas", ids.isEmpty() ? "-" : ids)
            .build());
  }

  private boolean info(CommandSender sender, String[] args) {
    if (!allowed(sender, AdministrativeCommandPolicy.ARENA_INFO)) return true;
    ArenaDefinition arena = arena(args);
    if (arena == null) return notFound(sender, args);
    return send(sender, TranslationKey.ARENA_INFO, context(arena));
  }

  private boolean menu(CommandSender sender) {
    if (!allowed(sender, AdministrativeCommandPolicy.ARENA_MENU)) return true;
    if (!(sender instanceof Player player)) return send(sender, TranslationKey.ARENA_MENU_CONSOLE);
    gui.open(player, menus.list(player.getUniqueId()));
    return true;
  }

  private boolean setWorld(CommandSender sender, String[] args) {
    if (!allowed(sender, AdministrativeCommandPolicy.ARENA_EDIT)) return true;
    if (args.length < 3 || args.length > 4) return send(sender, TranslationKey.ARENA_HELP);
    String world =
        args.length == 4
            ? args[3]
            : sender instanceof Player player ? player.getWorld().getName() : null;
    if (world == null) return send(sender, TranslationKey.ARENA_INVALID_ARGUMENT);
    return result(sender, arenas.setWorld(args[2], world), TranslationKey.ARENA_UPDATED);
  }

  private boolean setMap(CommandSender sender, String[] args) {
    if (!allowed(sender, AdministrativeCommandPolicy.ARENA_EDIT)) return true;
    if (args.length != 4) return send(sender, TranslationKey.ARENA_HELP);
    var map = maps.find(args[3]).orElse(null);
    if (map == null || map.type() != MapType.BEDWARS || !maps.validBedWarsTemplate(args[3]))
      return send(sender, TranslationKey.ARENA_INVALID_ARGUMENT);
    ArenaOperationResult operation =
        arenas.setMapTemplate(args[2], map.id().value(), map.worldName());
    if (operation.successful()) maps.synchronizeLinks(arenas.list());
    return result(sender, operation, TranslationKey.ARENA_UPDATED);
  }

  private boolean setLocation(CommandSender sender, String[] args, boolean waiting) {
    if (!allowed(sender, AdministrativeCommandPolicy.ARENA_EDIT)) return true;
    if (args.length != 3 || !(sender instanceof Player player))
      return send(
          sender, args.length == 3 ? TranslationKey.PLAYER_ONLY : TranslationKey.ARENA_HELP);
    ArenaOperationResult result =
        waiting
            ? arenas.setWaiting(args[2], BukkitArenaLocations.from(player.getLocation()))
            : arenas.setSpectator(args[2], BukkitArenaLocations.from(player.getLocation()));
    return result(sender, result, TranslationKey.ARENA_UPDATED);
  }

  private boolean setPlayers(CommandSender sender, String[] args) {
    if (!allowed(sender, AdministrativeCommandPolicy.ARENA_EDIT)) return true;
    if (args.length != 5) return send(sender, TranslationKey.ARENA_HELP);
    try {
      return result(
          sender,
          arenas.setPlayers(args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4])),
          TranslationKey.ARENA_UPDATED);
    } catch (NumberFormatException exception) {
      return send(sender, TranslationKey.ARENA_INVALID_ARGUMENT);
    }
  }

  private boolean setTeams(CommandSender sender, String[] args) {
    if (!allowed(sender, AdministrativeCommandPolicy.ARENA_EDIT)) return true;
    if (args.length != 5) return send(sender, TranslationKey.ARENA_HELP);
    try {
      return result(
          sender,
          arenas.setTeams(args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4])),
          TranslationKey.ARENA_UPDATED);
    } catch (NumberFormatException exception) {
      return send(sender, TranslationKey.ARENA_INVALID_ARGUMENT);
    }
  }

  private boolean team(CommandSender sender, String[] args) {
    if (!allowed(sender, AdministrativeCommandPolicy.ARENA_EDIT)) return true;
    if (args.length < 4) return send(sender, "command.arena.team-help", Map.of());
    ArenaDefinition arena = arenas.find(args[3]).orElse(null);
    if (arena == null) return notFound(sender, new String[] {"arena", "info", args[3]});
    if (args[2].equalsIgnoreCase("list")) {
      if (args.length != 4) return send(sender, "command.arena.team-help", Map.of());
      String values =
          String.join(
              ", ",
              arena.teams().stream()
                  .map(
                      team ->
                          team.id().value()
                              + " [spawn="
                              + (team.spawn().isPresent() ? "ok" : "-")
                              + ", bed="
                              + (team.bedDefinition().isPresent() ? "ok" : "-")
                              + "]")
                  .toList());
      return send(
          sender, "command.arena.team-list", Map.of("arena", arena.id().value(), "teams", values));
    }
    if (args.length != 5) return send(sender, "command.arena.team-help", Map.of());
    TeamId teamId;
    try {
      teamId = new TeamId(args[4]);
    } catch (IllegalArgumentException exception) {
      return send(sender, TranslationKey.ARENA_INVALID_ARGUMENT);
    }
    return switch (args[2].toLowerCase(Locale.ROOT)) {
      case "setspawn" -> {
        Player player = requirePlayer(sender);
        if (player == null) yield true;
        if (!correctWorld(sender, player, arena)) yield true;
        yield result(
            sender,
            arenas.setTeamSpawn(
                arena.id().value(),
                teamId,
                BukkitArenaLocations.from(player.getLocation()),
                arena.revision()),
            TranslationKey.ARENA_UPDATED);
      }
      case "clearspawn" ->
          result(
              sender,
              arenas.clearTeamSpawn(arena.id().value(), teamId, arena.revision()),
              TranslationKey.ARENA_UPDATED);
      case "teleport" -> {
        Player player = requirePlayer(sender);
        if (player == null) yield true;
        if (!allowed(sender, AdministrativeCommandPolicy.ARENA_TELEPORT)) yield true;
        var team =
            arena.teams().stream()
                .filter(value -> value.id().equals(teamId))
                .findFirst()
                .orElse(null);
        if (team == null || team.spawn().isEmpty())
          yield send(sender, "arena.team.spawn.missing", Map.of("team", teamId.value()));
        yield teleport(sender, player, team.spawn().orElseThrow(), false);
      }
      case "setbed" -> {
        Player player = requirePlayer(sender);
        if (player == null) yield true;
        BukkitArenaBeds.Selection selection = BukkitArenaBeds.select(player, arena);
        if (!selection.successful()) yield bedSelectionFailure(sender, selection.code(), arena);
        ArenaOperationResult operation =
            arenas.setTeamBed(
                arena.id().value(), teamId, selection.bed().orElseThrow(), arena.revision());
        if (!operation.successful()) yield bedOperationFailure(sender, operation);
        yield send(
            sender,
            "arena.team.bed.selected",
            Map.of("team", teamId.value(), "arena", arena.id().value()));
      }
      case "clearbed" ->
          result(
              sender,
              arenas.clearTeamBed(arena.id().value(), teamId, arena.revision()),
              TranslationKey.ARENA_UPDATED);
      case "teleportbed" -> {
        Player player = requirePlayer(sender);
        if (player == null) yield true;
        if (!allowed(sender, AdministrativeCommandPolicy.ARENA_TELEPORT)) yield true;
        var team =
            arena.teams().stream()
                .filter(value -> value.id().equals(teamId))
                .findFirst()
                .orElse(null);
        if (team == null || team.bedDefinition().isEmpty())
          yield send(sender, "arena.team.bed.missing", Map.of("team", teamId.value()));
        yield teleport(sender, player, team.bedLocation().orElseThrow(), true);
      }
      default -> send(sender, "command.arena.team-help", Map.of());
    };
  }

  private Player requirePlayer(CommandSender sender) {
    if (sender instanceof Player player) return player;
    send(sender, TranslationKey.PLAYER_ONLY);
    return null;
  }

  private boolean correctWorld(CommandSender sender, Player player, ArenaDefinition arena) {
    String expected = arena.worldName().orElse("-");
    if (player.getWorld().getName().equals(expected)) return true;
    send(
        sender,
        "arena.team.wrong-world",
        Map.of("world", expected, "current_world", player.getWorld().getName()));
    return false;
  }

  private boolean bedSelectionFailure(
      CommandSender sender, BukkitArenaBeds.SelectionCode code, ArenaDefinition arena) {
    String key =
        switch (code) {
          case WRONG_WORLD -> "arena.team.wrong-world";
          case NOT_A_BED -> "arena.team.bed.invalid-block";
          case INCOMPLETE_BED -> "arena.team.bed.incomplete";
          case SUCCESS ->
              throw new IllegalArgumentException("Successful selection is not a failure");
        };
    return send(
        sender,
        key,
        Map.of(
            "world",
            arena.worldName().orElse("-"),
            "current_world",
            sender instanceof Player player ? player.getWorld().getName() : "-"));
  }

  private boolean bedOperationFailure(CommandSender sender, ArenaOperationResult operation) {
    if (operation.code() == ArenaOperationCode.INVALID_ARGUMENT
        && operation.detail().startsWith("Bed already belongs to team "))
      return send(
          sender,
          "arena.team.bed.duplicate",
          Map.of("team", operation.detail().substring("Bed already belongs to team ".length())));
    return result(sender, operation, TranslationKey.ARENA_UPDATED);
  }

  private boolean teleport(
      CommandSender sender, Player player, ArenaLocation location, boolean aboveBlock) {
    org.bukkit.World world = org.bukkit.Bukkit.getWorld(location.world());
    if (world == null)
      return send(sender, "arena.world.not-found", Map.of("world", location.world()));
    double offset = aboveBlock ? 0.5 : 0.0;
    player.teleport(
        new org.bukkit.Location(
            world,
            location.position().x() + offset,
            location.position().y() + (aboveBlock ? 1.0 : 0.0),
            location.position().z() + offset,
            location.yaw(),
            location.pitch()));
    return send(sender, "arena.world.teleport-success", Map.of("world", world.getName()));
  }

  private boolean validate(CommandSender sender, String[] args) {
    if (!allowed(sender, AdministrativeCommandPolicy.ARENA_EDIT)) return true;
    ArenaDefinition arena = arena(args);
    if (arena == null) return notFound(sender, args);
    ArenaValidationResult validation = arenas.validate(arena);
    send(
        sender,
        validation.valid() ? TranslationKey.ARENA_VALID : TranslationKey.ARENA_INVALID,
        context(arena));
    validation
        .problems()
        .forEach(
            problem ->
                send(
                    sender,
                    TranslationKey.ARENA_PROBLEM,
                    PlaceholderContext.builder()
                        .put("severity", problem.severity())
                        .put("code", problem.code())
                        .put("field", problem.field())
                        .put("problem", problem.message())
                        .build()));
    return true;
  }

  private boolean status(CommandSender sender, String[] args, boolean enable) {
    if (!allowed(
        sender,
        enable
            ? AdministrativeCommandPolicy.ARENA_ENABLE
            : AdministrativeCommandPolicy.ARENA_DISABLE)) return true;
    if (args.length != 3) return send(sender, TranslationKey.ARENA_HELP);
    return result(
        sender,
        enable ? arenas.enable(args[2]) : arenas.disable(args[2]),
        enable ? TranslationKey.ARENA_ENABLED : TranslationKey.ARENA_DISABLED);
  }

  private boolean delete(CommandSender sender, String[] args) {
    if (!allowed(sender, AdministrativeCommandPolicy.ARENA_DELETE)) return true;
    ArenaDefinition arena = arena(args);
    if (arena == null) return notFound(sender, args);
    if (!(sender instanceof Player player)) return send(sender, TranslationKey.PLAYER_ONLY);
    gui.open(player, menus.deleteMenu(player.getUniqueId(), arena.id().value()));
    return true;
  }

  private ArenaDefinition arena(String[] args) {
    return args.length == 3 ? arenas.find(args[2]).orElse(null) : null;
  }

  private boolean result(
      CommandSender sender, ArenaOperationResult result, TranslationKey success) {
    if (result.successful()) return send(sender, success, context(result.arena().orElseThrow()));
    TranslationKey failure =
        switch (result.code()) {
          case ALREADY_EXISTS -> TranslationKey.ARENA_EXISTS;
          case INVALID_ID -> TranslationKey.ARENA_INVALID_ID;
          case NOT_FOUND -> TranslationKey.ARENA_NOT_FOUND;
          case INVALID_ARGUMENT -> TranslationKey.ARENA_INVALID_ARGUMENT;
          case VALIDATION_FAILED -> TranslationKey.ARENA_INVALID;
          case CONFLICT -> TranslationKey.ARENA_STORAGE_ERROR;
          case STORAGE_FAILED -> TranslationKey.ARENA_STORAGE_ERROR;
          default -> TranslationKey.INTERNAL_ERROR;
        };
    String id = result.arena().map(arena -> arena.id().value()).orElse(result.detail());
    send(
        sender,
        failure,
        PlaceholderContext.builder().put("arena", id).put("detail", result.detail()).build());
    if (result.code() == ArenaOperationCode.VALIDATION_FAILED)
      result
          .problems()
          .forEach(
              problem ->
                  send(
                      sender,
                      TranslationKey.ARENA_PROBLEM,
                      PlaceholderContext.builder()
                          .put("severity", problem.severity())
                          .put("code", problem.code())
                          .put("field", problem.field())
                          .put("problem", problem.message())
                          .build()));
    return true;
  }

  private boolean notFound(CommandSender sender, String[] args) {
    return send(
        sender,
        TranslationKey.ARENA_NOT_FOUND,
        PlaceholderContext.builder().put("arena", args.length >= 3 ? args[2] : "?").build());
  }

  private boolean allowed(CommandSender sender, String permission) {
    if (sender.hasPermission(permission)) return true;
    send(sender, TranslationKey.NO_PERMISSION);
    return false;
  }

  private PlaceholderContext context(ArenaDefinition arena) {
    return PlaceholderContext.builder()
        .put("arena", arena.id().value())
        .put("arena_id", arena.id().value())
        .put("arena_name", arena.displayName())
        .put("arena_status", arena.status())
        .put("world", arena.worldName().orElse("-"))
        .put("min_players", arena.minimumPlayers())
        .put("max_players", arena.maximumPlayers())
        .put("teams", arena.teamCount())
        .put("players_per_team", arena.playersPerTeam())
        .build();
  }

  private boolean send(CommandSender sender, TranslationKey key) {
    sender.sendMessage(configurations.language().message(key));
    return true;
  }

  private boolean send(CommandSender sender, TranslationKey key, PlaceholderContext context) {
    sender.sendMessage(configurations.language().message(key, context));
    return true;
  }

  private boolean send(CommandSender sender, String key, Map<String, ?> values) {
    PlaceholderContext.Builder context = PlaceholderContext.builder();
    values.forEach(context::put);
    sender.sendMessage(
        configurations
            .language()
            .message(key, configurations.snapshot().plugin().locale(), context.build()));
    return true;
  }
}
