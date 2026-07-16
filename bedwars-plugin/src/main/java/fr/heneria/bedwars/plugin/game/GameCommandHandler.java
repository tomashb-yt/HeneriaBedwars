package fr.heneria.bedwars.plugin.game;

import fr.heneria.bedwars.core.command.AdministrativeCommandPolicy;
import fr.heneria.bedwars.core.config.PlaceholderContext;
import fr.heneria.bedwars.core.game.GameId;
import fr.heneria.bedwars.core.game.GameInstance;
import fr.heneria.bedwars.core.game.GameInstanceManager;
import fr.heneria.bedwars.core.game.GameOperationResult;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Thin administrative command adapter for the Ticket 009 runtime engine. */
public final class GameCommandHandler {
  private final JavaPlugin plugin;
  private final GameInstanceManager games;
  private final ConfigurationService configurations;

  public GameCommandHandler(
      JavaPlugin plugin, GameInstanceManager games, ConfigurationService configurations) {
    this.plugin = plugin;
    this.games = games;
    this.configurations = configurations;
  }

  public boolean execute(CommandSender sender, String[] args) {
    if (!allowed(sender, AdministrativeCommandPolicy.GAME)) return true;
    if (args.length < 2) return send(sender, "game.command.help", Map.of());
    return switch (args[1].toLowerCase(Locale.ROOT)) {
      case "create" -> create(sender, args);
      case "list" -> list(sender);
      case "info" -> info(sender, args);
      case "join" -> join(sender, args);
      case "leave" -> leave(sender);
      case "destroy" -> destroy(sender, args);
      default -> send(sender, "game.command.help", Map.of());
    };
  }

  public List<String> complete(CommandSender sender, String[] args, List<String> arenaIds) {
    if (!sender.hasPermission(AdministrativeCommandPolicy.GAME)) return List.of();
    List<String> values = new java.util.ArrayList<>();
    if (args.length == 2) {
      if (sender.hasPermission(AdministrativeCommandPolicy.GAME_CREATE)) values.add("create");
      if (sender.hasPermission(AdministrativeCommandPolicy.GAME_LIST)) values.add("list");
      if (sender.hasPermission(AdministrativeCommandPolicy.GAME_INFO)) values.add("info");
      if (sender.hasPermission(AdministrativeCommandPolicy.GAME_JOIN)) values.add("join");
      if (sender.hasPermission(AdministrativeCommandPolicy.GAME_LEAVE)) values.add("leave");
      if (sender.hasPermission(AdministrativeCommandPolicy.GAME_DESTROY)) values.add("destroy");
    } else if (args.length == 3 && args[1].equalsIgnoreCase("create")) {
      values.addAll(arenaIds);
    } else if (args.length == 3
        && List.of("info", "join", "destroy").contains(args[1].toLowerCase(Locale.ROOT))) {
      values.addAll(games.all().stream().map(game -> game.id().toString()).toList());
    }
    String input = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
    return values.stream()
        .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(input))
        .toList();
  }

  private boolean create(CommandSender sender, String[] args) {
    if (!allowed(sender, AdministrativeCommandPolicy.GAME_CREATE)) return true;
    if (args.length != 3) return send(sender, "game.command.help", Map.of());
    send(sender, "game.command.creating", Map.of("arena_id", args[2]));
    games
        .create(args[2])
        .whenComplete(
            (result, failure) ->
                main(
                    () -> {
                      if (failure != null) {
                        send(sender, "game.command.failed", Map.of("code", "INTERNAL_ERROR"));
                        return;
                      }
                      if (!result.successful()) {
                        feedback(sender, result);
                        return;
                      }
                      GameInstance instance = result.instance().orElseThrow();
                      send(sender, "game.command.created", values(instance));
                      if (sender instanceof Player player)
                        games
                            .join(instance.id(), player.getUniqueId())
                            .whenComplete(
                                (joined, joinFailure) ->
                                    main(
                                        () -> {
                                          if (joinFailure != null || !joined.successful())
                                            send(
                                                sender,
                                                "game.command.teleport-failed",
                                                values(instance));
                                          else
                                            send(sender, "game.command.joined", values(instance));
                                        }));
                    }));
    return true;
  }

  private boolean list(CommandSender sender) {
    if (!allowed(sender, AdministrativeCommandPolicy.GAME_LIST)) return true;
    String visible =
        String.join(
            ", ",
            games.all().stream().map(game -> game.id() + "=" + game.state()).limit(20).toList());
    return send(
        sender,
        "game.command.list",
        Map.of("count", games.size(), "games", visible.isBlank() ? "-" : visible));
  }

  private boolean info(CommandSender sender, String[] args) {
    if (!allowed(sender, AdministrativeCommandPolicy.GAME_INFO)) return true;
    GameInstance instance = instance(args);
    if (instance == null) return send(sender, "game.command.not-found", Map.of());
    return send(sender, "game.command.info", values(instance));
  }

  private boolean join(CommandSender sender, String[] args) {
    if (!allowed(sender, AdministrativeCommandPolicy.GAME_JOIN)) return true;
    if (!(sender instanceof Player player))
      return send(sender, "game.command.player-only", Map.of());
    GameInstance instance = instance(args);
    if (instance == null) return send(sender, "game.command.not-found", Map.of());
    games
        .join(instance.id(), player.getUniqueId())
        .whenComplete(
            (result, failure) ->
                main(
                    () -> {
                      if (failure != null || !result.successful()) feedback(sender, result);
                      else send(sender, "game.command.joined", values(instance));
                    }));
    return true;
  }

  private boolean leave(CommandSender sender) {
    if (!allowed(sender, AdministrativeCommandPolicy.GAME_LEAVE)) return true;
    if (!(sender instanceof Player player))
      return send(sender, "game.command.player-only", Map.of());
    games
        .leave(player.getUniqueId())
        .whenComplete(
            (result, failure) ->
                main(
                    () ->
                        send(
                            sender,
                            failure == null && result.successful()
                                ? "game.command.left"
                                : "game.command.not-found",
                            Map.of())));
    return true;
  }

  private boolean destroy(CommandSender sender, String[] args) {
    if (!allowed(sender, AdministrativeCommandPolicy.GAME_DESTROY)) return true;
    GameInstance instance = instance(args);
    if (instance == null) return send(sender, "game.command.not-found", Map.of());
    send(sender, "game.command.destroying", values(instance));
    games
        .destroy(instance.id(), "admin-command")
        .whenComplete(
            (result, failure) ->
                main(
                    () -> {
                      if (failure != null || !result.successful()) feedback(sender, result);
                      else send(sender, "game.command.destroyed", values(instance));
                    }));
    return true;
  }

  private GameInstance instance(String[] args) {
    if (args.length != 3) return null;
    try {
      return games.find(GameId.parse(args[2])).orElse(null);
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  private void feedback(CommandSender sender, GameOperationResult result) {
    String code = result == null ? "INTERNAL_ERROR" : result.code().name();
    send(sender, "game.command.failed", Map.of("code", code));
  }

  private Map<String, Object> values(GameInstance instance) {
    var snapshot = instance.snapshot(java.time.Instant.now());
    return Map.of(
        "game_id", snapshot.id(),
        "arena_id", snapshot.arenaId(),
        "map_id", snapshot.mapTemplateId(),
        "game_state", snapshot.state(),
        "world_name", snapshot.worldName().orElse("-"),
        "player_count", snapshot.players().size());
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

  private void main(Runnable action) {
    if (Bukkit.isPrimaryThread()) action.run();
    else plugin.getServer().getScheduler().runTask(plugin, action);
  }
}
