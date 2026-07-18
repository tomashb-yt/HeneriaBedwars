package fr.heneria.bedwars.plugin.game;

import fr.heneria.bedwars.core.arena.ArenaDefinition;
import fr.heneria.bedwars.core.arena.ArenaService;
import fr.heneria.bedwars.core.command.AdministrativeCommandPolicy;
import fr.heneria.bedwars.core.config.PlaceholderContext;
import fr.heneria.bedwars.core.game.GameInstance;
import fr.heneria.bedwars.core.game.GameInstanceManager;
import fr.heneria.bedwars.core.game.GameLookupResult;
import fr.heneria.bedwars.core.game.GameLookupStatus;
import fr.heneria.bedwars.core.game.GameOperationResult;
import fr.heneria.bedwars.core.game.countdown.CountdownOperationResult;
import fr.heneria.bedwars.core.game.countdown.GameCountdownService;
import fr.heneria.bedwars.core.game.lobby.GameJoinResult;
import fr.heneria.bedwars.core.game.lobby.GameLobbyService;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Thin administrative command adapter for runtime lobby and countdown use cases. */
public final class GameCommandHandler {
  private final JavaPlugin plugin;
  private final GameInstanceManager games;
  private final GameCountdownService countdowns;
  private final GameLobbyService lobby;
  private final ArenaService arenas;
  private final ConfigurationService configurations;

  public GameCommandHandler(
      JavaPlugin plugin,
      GameInstanceManager games,
      GameCountdownService countdowns,
      GameLobbyService lobby,
      ArenaService arenas,
      ConfigurationService configurations) {
    this.plugin = plugin;
    this.games = games;
    this.countdowns = countdowns;
    this.lobby = lobby;
    this.arenas = arenas;
    this.configurations = configurations;
  }

  public boolean execute(CommandSender sender, String[] args) {
    if (args.length < 2) return send(sender, "game.command.help", Map.of());
    return switch (args[1].toLowerCase(Locale.ROOT)) {
      case "create" -> create(sender, args);
      case "list" -> list(sender);
      case "info" -> info(sender, args);
      case "join" -> join(sender, args);
      case "leave" -> leave(sender);
      case "start" -> start(sender, args);
      case "stop" -> stop(sender, args, AdministrativeCommandPolicy.GAME_STOP);
      case "destroy" -> stop(sender, args, AdministrativeCommandPolicy.GAME_DESTROY);
      default -> send(sender, "game.command.help", Map.of());
    };
  }

  /** Public player surface: stable map names only, never runtime instance ids. */
  public boolean executePublic(CommandSender sender, String[] args) {
    if (!(sender instanceof Player player))
      return send(sender, "game.command.player-only", Map.of());
    if (!allowed(sender, AdministrativeCommandPolicy.GAME_JOIN)) return true;
    if (args.length == 0 || args[0].equalsIgnoreCase("play")) return publicHelp(sender);
    return switch (args[0].toLowerCase(Locale.ROOT)) {
      case "join" -> publicJoin(player, args);
      case "leave" -> leave(sender);
      default -> publicHelp(sender);
    };
  }

  public List<String> completePublic(CommandSender sender, String[] args) {
    if (!sender.hasPermission(AdministrativeCommandPolicy.GAME_JOIN)) return List.of();
    List<String> values =
        args.length <= 1
            ? List.of("play", "join", "leave")
            : args.length == 2 && args[0].equalsIgnoreCase("join") ? publicMapIds() : List.of();
    String input = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
    return values.stream().filter(value -> value.startsWith(input)).toList();
  }

  public List<String> complete(CommandSender sender, String[] args, List<String> arenaIds) {
    boolean administrator = sender.hasPermission(AdministrativeCommandPolicy.GAME);
    boolean canJoin = sender.hasPermission(AdministrativeCommandPolicy.GAME_JOIN);
    boolean canLeave = sender.hasPermission(AdministrativeCommandPolicy.GAME_LEAVE);
    if (!administrator && !canJoin && !canLeave) return List.of();
    List<String> values = new ArrayList<>();
    if (args.length == 2) {
      if (administrator && sender.hasPermission(AdministrativeCommandPolicy.GAME_CREATE))
        values.add("create");
      if (administrator && sender.hasPermission(AdministrativeCommandPolicy.GAME_LIST))
        values.add("list");
      if (administrator && sender.hasPermission(AdministrativeCommandPolicy.GAME_INFO))
        values.add("info");
      if (canJoin) values.add("join");
      if (canLeave) values.add("leave");
      if (administrator && sender.hasPermission(AdministrativeCommandPolicy.GAME_START))
        values.add("start");
      if (administrator && sender.hasPermission(AdministrativeCommandPolicy.GAME_STOP))
        values.add("stop");
      if (administrator && sender.hasPermission(AdministrativeCommandPolicy.GAME_DESTROY))
        values.add("destroy");
    } else if (args.length == 3 && args[1].equalsIgnoreCase("create")) {
      values.addAll(arenaIds);
    } else if (args.length == 3 && args[1].equalsIgnoreCase("join")) {
      values.addAll(publicMapIds());
    } else if (args.length == 3
        && List.of("info", "start", "stop", "destroy").contains(args[1].toLowerCase(Locale.ROOT))) {
      values.addAll(shortIds());
    } else if (args.length == 4 && args[1].equalsIgnoreCase("start")) {
      values.add("--force");
    }
    String input = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
    return values.stream()
        .distinct()
        .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(input))
        .toList();
  }

  private boolean create(CommandSender sender, String[] args) {
    if (!allowedAdmin(sender, AdministrativeCommandPolicy.GAME_CREATE)) return true;
    if (args.length != 3) return send(sender, "game.command.help", Map.of());
    send(sender, "game.command.creating", Map.of("arena_id", args[2]));
    games
        .create(args[2])
        .whenComplete(
            (result, failure) ->
                main(
                    () -> {
                      if (failure != null || result == null || !result.successful()) {
                        feedback(sender, result);
                        return;
                      }
                      GameInstance instance = result.instance().orElseThrow();
                      send(sender, "game.command.created", values(instance));
                      if (sender instanceof Player player) join(sender, player, instance);
                    }));
    return true;
  }

  private boolean list(CommandSender sender) {
    if (!allowedAdmin(sender, AdministrativeCommandPolicy.GAME_LIST)) return true;
    String visible = String.join(" | ", games.all().stream().map(this::summary).limit(20).toList());
    return send(
        sender,
        "game.command.list",
        Map.of("count", games.size(), "games", visible.isBlank() ? "-" : visible));
  }

  private boolean info(CommandSender sender, String[] args) {
    if (!allowedAdmin(sender, AdministrativeCommandPolicy.GAME_INFO)) return true;
    GameInstance instance = resolve(sender, args, 3);
    if (instance == null) return true;
    return send(sender, "game.command.info", values(instance));
  }

  private boolean join(CommandSender sender, String[] args) {
    if (!allowed(sender, AdministrativeCommandPolicy.GAME_JOIN)) return true;
    if (!(sender instanceof Player player))
      return send(sender, "game.command.player-only", Map.of());
    if (args.length != 3) return send(sender, "game.command.help", Map.of());
    GameInstance publicMatch = bestPublicGame(args[2]);
    if (publicMatch != null) {
      join(sender, player, publicMatch);
      return true;
    }
    GameLookupResult lookup = games.lookup(args[2]);
    if (lookup.status() == GameLookupStatus.AMBIGUOUS)
      return send(sender, "game.command.ambiguous", Map.of());
    GameInstance instance = lookup.instance().orElseGet(() -> games.byArena(args[2]).orElse(null));
    if (instance == null) {
      if (!sender.hasPermission(AdministrativeCommandPolicy.GAME_CREATE))
        return send(sender, "game.command.not-found", Map.of());
      send(sender, "game.command.creating", Map.of("arena_id", args[2]));
      games
          .create(args[2])
          .whenComplete(
              (created, failure) ->
                  main(
                      () -> {
                        if (failure != null || created == null || !created.successful()) {
                          feedback(sender, created);
                          return;
                        }
                        join(sender, player, created.instance().orElseThrow());
                      }));
      return true;
    }
    join(sender, player, instance);
    return true;
  }

  private boolean publicJoin(Player player, String[] args) {
    if (args.length != 2)
      return send(
          player, "game.help.usage.join", Map.of("maps", String.join(", ", publicMapIds())));
    GameInstance instance = bestPublicGame(args[1]);
    if (instance != null) {
      join(player, player, instance);
      return true;
    }
    ArenaDefinition arena = publicArena(args[1]);
    if (arena == null)
      return send(player, "game.error.no-available-instance", Map.of("map", args[1]));
    send(player, "game.command.creating", Map.of("arena_id", arena.id().value()));
    games
        .create(arena.id().value())
        .whenComplete(
            (created, failure) ->
                main(
                    () -> {
                      GameInstance available =
                          created != null && created.successful()
                              ? created.instance().orElse(null)
                              : games.byArena(arena.id().value()).orElse(null);
                      if (failure != null || available == null) {
                        send(player, "game.error.no-available-instance", Map.of("map", args[1]));
                        return;
                      }
                      join(player, player, available);
                    }));
    return true;
  }

  private boolean publicHelp(CommandSender sender) {
    return send(sender, "game.help.public", Map.of("maps", String.join(", ", publicMapIds())));
  }

  private void join(CommandSender sender, Player player, GameInstance instance) {
    lobby
        .join(instance.id(), player.getUniqueId())
        .whenComplete(
            (result, failure) ->
                main(
                    () -> {
                      if (failure != null || result == null || !result.successful())
                        feedback(sender, result);
                      else send(sender, "game.command.joined", values(instance));
                    }));
  }

  private boolean leave(CommandSender sender) {
    if (!allowed(sender, AdministrativeCommandPolicy.GAME_LEAVE)) return true;
    if (!(sender instanceof Player player))
      return send(sender, "game.command.player-only", Map.of());
    lobby
        .leave(player.getUniqueId())
        .whenComplete(
            (result, failure) ->
                main(
                    () ->
                        send(
                            sender,
                            failure == null && result != null && result.successful()
                                ? "game.command.left"
                                : "game.leave.not-in-game",
                            Map.of())));
    return true;
  }

  private boolean start(CommandSender sender, String[] args) {
    if (!allowedAdmin(sender, AdministrativeCommandPolicy.GAME_START)) return true;
    if (args.length < 3 || args.length > 4) return send(sender, "game.command.help", Map.of());
    boolean force = args.length == 4 && args[3].equalsIgnoreCase("--force");
    if (args.length == 4 && !force) return send(sender, "game.command.help", Map.of());
    if (force && !allowed(sender, AdministrativeCommandPolicy.GAME_FORCE_START)) return true;
    GameInstance instance = resolve(sender, args, -1);
    if (instance == null) return true;
    CountdownOperationResult result = lobby.start(instance.id(), force);
    return result.successful()
        ? send(sender, "game.command.started", values(instance))
        : send(sender, "game.command.failed", Map.of("code", result.code().name()));
  }

  private boolean stop(CommandSender sender, String[] args, String permission) {
    if (!allowedAdmin(sender, permission)) return true;
    GameInstance instance = resolve(sender, args, 3);
    if (instance == null) return true;
    send(sender, "game.command.destroying", values(instance));
    lobby
        .stopGame(instance.id(), "admin-command")
        .whenComplete(
            (result, failure) ->
                main(
                    () -> {
                      if (failure != null || result == null || !result.successful())
                        feedback(sender, result);
                      else send(sender, "game.command.stopped", values(instance));
                    }));
    return true;
  }

  private GameInstance resolve(CommandSender sender, String[] args, int expectedLength) {
    if ((expectedLength > 0 && args.length != expectedLength) || args.length < 3) {
      send(sender, "game.command.help", Map.of());
      return null;
    }
    GameLookupResult lookup = games.lookup(args[2]);
    if (lookup.status() == GameLookupStatus.AMBIGUOUS) {
      send(sender, "game.command.ambiguous", Map.of());
      return null;
    }
    if (lookup.status() == GameLookupStatus.NOT_FOUND) {
      send(sender, "game.command.not-found", Map.of());
      return null;
    }
    return lookup.instance().orElseThrow();
  }

  private void feedback(CommandSender sender, GameOperationResult result) {
    String code = result == null ? "INTERNAL_ERROR" : result.code().name();
    send(sender, "game.command.failed", Map.of("code", code));
  }

  private void feedback(CommandSender sender, GameJoinResult result) {
    String code = result == null ? "INTERNAL_ERROR" : result.code().name();
    send(sender, "game.command.failed", Map.of("code", code));
  }

  private String summary(GameInstance instance) {
    String status =
        countdowns
            .snapshot(instance.id())
            .map(value -> "start " + value.remainingSeconds() + "s")
            .orElse("minimum non atteint");
    return instance.id().shortId()
        + " — "
        + instance.arena().definition().id().value()
        + " — "
        + instance.state()
        + " — "
        + instance.playerIds().size()
        + "/"
        + instance.arena().definition().maximumPlayers()
        + " — "
        + status;
  }

  private Map<String, Object> values(GameInstance instance) {
    var snapshot = instance.snapshot(Instant.now());
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("game_id", instance.id().shortId());
    values.put("full_game_id", snapshot.id());
    values.put("arena_id", snapshot.arenaId());
    values.put("map_id", snapshot.mapTemplateId());
    values.put("game_state", snapshot.state());
    values.put("state", snapshot.state());
    values.put("world_name", snapshot.worldName().orElse("-"));
    values.put("player_count", snapshot.players().size());
    values.put("players", snapshot.players().size());
    values.put("minimum_players", instance.arena().definition().minimumPlayers());
    values.put("maximum_players", instance.arena().definition().maximumPlayers());
    values.put(
        "countdown",
        countdowns.snapshot(instance.id()).map(value -> value.remainingSeconds()).orElse(0));
    values.put(
        "age_seconds",
        Math.max(0, Duration.between(snapshot.createdAt(), Instant.now()).toSeconds()));
    return Map.copyOf(values);
  }

  private List<String> shortIds() {
    return games.all().stream().map(game -> game.id().shortId()).toList();
  }

  private List<String> publicMapIds() {
    return arenas.list().stream()
        .filter(ArenaDefinition::enabled)
        .filter(arena -> arenas.validate(arena).valid())
        .flatMap(arena -> arena.template().stream())
        .distinct()
        .sorted()
        .toList();
  }

  private ArenaDefinition publicArena(String reference) {
    String wanted = reference.trim();
    return arenas.list().stream()
        .filter(ArenaDefinition::enabled)
        .filter(arena -> arenas.validate(arena).valid())
        .filter(
            arena ->
                arena.id().value().equalsIgnoreCase(wanted)
                    || arena.displayName().equalsIgnoreCase(wanted)
                    || arena.template().filter(value -> value.equalsIgnoreCase(wanted)).isPresent())
        .findFirst()
        .orElse(null);
  }

  private GameInstance bestPublicGame(String reference) {
    String wanted = reference.trim();
    return games.all().stream()
        .filter(
            game ->
                game.state() == fr.heneria.bedwars.api.game.GameState.WAITING
                    || game.state() == fr.heneria.bedwars.api.game.GameState.STARTING)
        .filter(game -> game.playerIds().size() < game.arena().definition().maximumPlayers())
        .filter(
            game ->
                game.arena().template().id().value().equalsIgnoreCase(wanted)
                    || game.arena().template().displayName().equalsIgnoreCase(wanted)
                    || game.arena().definition().id().value().equalsIgnoreCase(wanted))
        .sorted(
            java.util.Comparator.comparingInt(
                    (GameInstance game) ->
                        game.state() == fr.heneria.bedwars.api.game.GameState.WAITING ? 0 : 1)
                .thenComparing(
                    java.util.Comparator.comparingInt(
                            (GameInstance game) -> game.playerIds().size())
                        .reversed()))
        .findFirst()
        .orElse(null);
  }

  private boolean allowed(CommandSender sender, String permission) {
    if (sender.hasPermission(permission)) return true;
    return send(sender, "general.no-permission", Map.of());
  }

  private boolean allowedAdmin(CommandSender sender, String permission) {
    return allowed(sender, AdministrativeCommandPolicy.GAME) && allowed(sender, permission);
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

  private void main(Runnable action) {
    if (Bukkit.isPrimaryThread()) action.run();
    else plugin.getServer().getScheduler().runTask(plugin, action);
  }
}
