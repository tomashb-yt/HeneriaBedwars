package fr.heneria.zombie.plugin.game;

import fr.heneria.zombie.core.game.GameEndReason;
import fr.heneria.zombie.core.game.ZombieGame;
import fr.heneria.zombie.core.instance.GameInstanceSnapshot;
import fr.heneria.zombie.plugin.instance.InstanceCoordinator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Permission-checked diagnostics and safe game lifecycle administration. */
public final class ZGameCommand implements CommandExecutor, TabCompleter {
  private final PaperGameRuntime games;
  private final InstanceCoordinator instances;

  public ZGameCommand(PaperGameRuntime games, InstanceCoordinator instances) {
    this.games = games;
    this.instances = instances;
  }

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] arguments) {
    if (!sender.hasPermission("zombies.admin.game.list")) {
      sender.sendMessage("Permission insuffisante.");
      return true;
    }
    if (arguments.length == 0 || arguments[0].equalsIgnoreCase("list")) {
      sender.sendMessage("Parties actives : " + games.snapshots().size());
      games.snapshots().forEach(game -> sender.sendMessage(line(game)));
      return true;
    }
    if (arguments.length < 2) {
      usage(sender);
      return true;
    }
    Optional<UUID> resolved = resolve(arguments[1]);
    if (resolved.isEmpty()) {
      sender.sendMessage("Instance introuvable ou identifiant ambigu.");
      return true;
    }
    UUID id = resolved.get();
    String requiredPermission =
        switch (arguments[0].toLowerCase(Locale.ROOT)) {
          case "start" -> "zombies.admin.game.start";
          case "stop" -> "zombies.admin.game.stop";
          case "nextround", "setround" -> "zombies.admin.game.round";
          default -> "zombies.admin.game.list";
        };
    if (!sender.hasPermission(requiredPermission)) {
      sender.sendMessage("Permission " + requiredPermission + " requise.");
      return true;
    }
    switch (arguments[0].toLowerCase(Locale.ROOT)) {
      case "info", "debug" ->
          sender.sendMessage(
              games
                  .snapshot(id)
                  .map(ZGameCommand::line)
                  .orElse("Aucune partie sur cette instance."));
      case "start" -> {
        sender.sendMessage(games.start(id) ? "Compte à rebours lancé." : "Partie déjà active.");
      }
      case "stop" -> {
        games.end(id, GameEndReason.ADMIN_STOP);
        sender.sendMessage("Arrêt contrôlé demandé.");
      }
      case "nextround" -> {
        sender.sendMessage(
            games.forceNextRound(id)
                ? "Transition accélérée."
                : "La partie n'est pas en transition.");
      }
      case "setround" -> {
        if (arguments.length != 3) {
          usage(sender);
        } else {
          try {
            sender.sendMessage(
                games.setRound(id, Integer.parseInt(arguments[2]))
                    ? "Manche appliquée."
                    : "Modification refusée hors transition.");
          } catch (NumberFormatException invalid) {
            sender.sendMessage("La manche doit être un entier positif.");
          }
        }
      }
      default -> usage(sender);
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
      return List.of("list", "info", "start", "stop", "nextround", "setround", "debug");
    }
    if (arguments.length == 2) {
      return instances.activeInstances().stream().map(value -> shortId(value.id())).toList();
    }
    return List.of();
  }

  private Optional<UUID> resolve(String prefix) {
    List<UUID> matches =
        instances.activeInstances().stream()
            .map(GameInstanceSnapshot::id)
            .filter(id -> id.toString().startsWith(prefix.toLowerCase(Locale.ROOT)))
            .limit(2)
            .toList();
    return matches.size() == 1 ? Optional.of(matches.getFirst()) : Optional.empty();
  }

  private static String line(ZombieGame.Snapshot game) {
    String round =
        game.round()
            .map(
                value ->
                    value.number()
                        + " • "
                        + value.phase()
                        + " • "
                        + value.aliveEnemies()
                        + "/"
                        + value.totalEnemies())
            .orElse("0");
    return shortId(game.gameId())
        + " • "
        + game.mapId()
        + " • "
        + game.state()
        + " • manche "
        + round
        + " • joueurs "
        + game.players().size();
  }

  private static String shortId(UUID id) {
    return id.toString().substring(0, 8);
  }

  private static void usage(CommandSender sender) {
    sender.sendMessage("/zgame <list|info|start|stop|nextround|setround|debug> [instance] [round]");
  }
}
