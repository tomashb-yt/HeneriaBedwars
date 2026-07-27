package fr.heneria.zombie.plugin.economy;

import fr.heneria.zombie.core.economy.EconomyService;
import fr.heneria.zombie.core.economy.GameEconomy;
import fr.heneria.zombie.core.economy.Transaction;
import fr.heneria.zombie.core.economy.TransactionReason;
import fr.heneria.zombie.core.economy.TransactionRequest;
import fr.heneria.zombie.core.economy.TransactionResult;
import fr.heneria.zombie.core.economy.TransactionService;
import fr.heneria.zombie.core.powerup.PowerUpService;
import fr.heneria.zombie.core.powerup.PowerUpType;
import fr.heneria.zombie.plugin.game.PaperGameRuntime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/** Transaction-backed administrative diagnostics and economy controls. */
public final class ZEconomyCommand implements CommandExecutor, TabCompleter {
  private final PaperGameRuntime games;
  private final EconomyService economies;
  private final TransactionService transactions;
  private final PowerUpService powerUps;
  private final PaperPowerUpService paperPowerUps;
  private final AtomicLong sequence = new AtomicLong();

  public ZEconomyCommand(
      PaperGameRuntime games,
      EconomyService economies,
      TransactionService transactions,
      PowerUpService powerUps,
      PaperPowerUpService paperPowerUps) {
    this.games = games;
    this.economies = economies;
    this.transactions = transactions;
    this.powerUps = powerUps;
    this.paperPowerUps = paperPowerUps;
  }

  @Override
  public boolean onCommand(
      CommandSender sender, Command command, String label, String[] arguments) {
    if (arguments.length == 0) {
      sender.sendMessage(
          "/zeconomy balance|set|add|remove|history|transaction|givepowerup|clearpowerups|debug");
      return true;
    }
    try {
      return switch (arguments[0].toLowerCase(java.util.Locale.ROOT)) {
        case "balance" -> playerQuery(sender, arguments, "balance");
        case "history" -> playerQuery(sender, arguments, "history");
        case "set", "add", "remove" -> adjust(sender, arguments);
        case "transaction" -> transaction(sender, arguments);
        case "givepowerup" -> givePowerUp(sender, arguments);
        case "clearpowerups" -> clearPowerUps(sender, arguments);
        case "debug" -> debug(sender, arguments);
        default -> false;
      };
    } catch (IllegalArgumentException failure) {
      sender.sendMessage("§c" + failure.getMessage());
      return true;
    }
  }

  private boolean playerQuery(CommandSender sender, String[] arguments, String action) {
    requirePermission(sender, "zombies.admin.economy." + action);
    Player target = player(arguments, 1);
    UUID gameId =
        games
            .gameFor(target.getUniqueId())
            .orElseThrow(() -> new IllegalArgumentException("Ce joueur n'est pas en partie."));
    var wallet =
        economies
            .wallet(gameId, target.getUniqueId())
            .orElseThrow(() -> new IllegalArgumentException("Portefeuille introuvable."));
    if (action.equals("balance")) {
      sender.sendMessage(
          "§6"
              + target.getName()
              + " §f: §a"
              + wallet.balance()
              + " §7(gagné "
              + wallet.totalEarned()
              + ", dépensé "
              + wallet.totalSpent()
              + ", remboursé "
              + wallet.totalRefunded()
              + ")");
    } else {
      GameEconomy economy = economies.find(gameId).orElseThrow();
      List<Transaction> history = economy.history(target.getUniqueId());
      sender.sendMessage("§6Dernières transactions de " + target.getName() + " :");
      history.stream()
          .skip(Math.max(0, history.size() - 10L))
          .forEach(
              value ->
                  sender.sendMessage(
                      "§7"
                          + value.transactionId()
                          + " §f"
                          + value.type()
                          + " "
                          + value.amount()
                          + " §8("
                          + value.reason()
                          + ")"));
    }
    return true;
  }

  private boolean adjust(CommandSender sender, String[] arguments) {
    String action = arguments[0].toLowerCase(java.util.Locale.ROOT);
    requirePermission(sender, "zombies.admin.economy." + action);
    Player target = player(arguments, 1);
    long amount = amount(arguments, 2);
    UUID gameId =
        games
            .gameFor(target.getUniqueId())
            .orElseThrow(() -> new IllegalArgumentException("Ce joueur n'est pas en partie."));
    String operation =
        "admin:" + action + ":" + sender.getName() + ":" + sequence.incrementAndGet();
    TransactionResult result =
        switch (action) {
          case "set" -> transactions.adjust(gameId, target.getUniqueId(), amount, operation);
          case "add" ->
              transactions.credit(
                  TransactionRequest.of(
                      gameId,
                      target.getUniqueId(),
                      amount,
                      TransactionReason.ADMIN_GRANT,
                      operation));
          case "remove" ->
              transactions.debit(
                  TransactionRequest.of(
                      gameId,
                      target.getUniqueId(),
                      amount,
                      TransactionReason.ADMIN_REMOVE,
                      operation));
          default -> throw new IllegalStateException("Unknown action");
        };
    sender.sendMessage(
        result.successful()
            ? "§aTransaction appliquée. Nouveau solde : "
                + result.transaction().orElseThrow().balanceAfter()
            : "§cTransaction refusée : " + result.status() + " " + result.failureReason());
    return true;
  }

  private boolean transaction(CommandSender sender, String[] arguments) {
    requirePermission(sender, "zombies.admin.economy.history");
    UUID id = uuid(arguments, 1);
    Optional<Transaction> found =
        economies.snapshots().stream()
            .map(
                snapshot -> economies.find(snapshot.gameId()).flatMap(game -> game.transaction(id)))
            .flatMap(Optional::stream)
            .findFirst();
    Transaction value =
        found.orElseThrow(() -> new IllegalArgumentException("Transaction introuvable."));
    sender.sendMessage(
        "§6"
            + value.transactionId()
            + " §f"
            + value.type()
            + " "
            + value.amount()
            + " §7"
            + value.balanceBefore()
            + " -> "
            + value.balanceAfter()
            + " §8"
            + value.reason());
    return true;
  }

  private boolean givePowerUp(CommandSender sender, String[] arguments) {
    requirePermission(sender, "zombies.admin.economy.powerup");
    if (arguments.length < 2) {
      throw new IllegalArgumentException("Usage: /zeconomy givepowerup <type> [game]");
    }
    PowerUpType type = PowerUpType.valueOf(arguments[1].toUpperCase(java.util.Locale.ROOT));
    UUID gameId = gameId(sender, arguments, 2);
    Player collector = sender instanceof Player player ? player : null;
    sender.sendMessage(
        paperPowerUps.activate(gameId, collector, type)
            ? "§aBonus activé : " + type
            : "§cBonus refusé par sa politique de cumul.");
    return true;
  }

  private boolean clearPowerUps(CommandSender sender, String[] arguments) {
    requirePermission(sender, "zombies.admin.economy.powerup");
    UUID gameId = gameId(sender, arguments, 1);
    paperPowerUps.clear(gameId);
    sender.sendMessage("§aBonus et drops supprimés pour " + gameId);
    return true;
  }

  private boolean debug(CommandSender sender, String[] arguments) {
    requirePermission(sender, "zombies.admin.economy.debug");
    UUID gameId = gameId(sender, arguments, 1);
    GameEconomy.Snapshot snapshot =
        economies
            .find(gameId)
            .map(GameEconomy::snapshot)
            .orElseThrow(() -> new IllegalArgumentException("Économie introuvable."));
    sender.sendMessage("§6Économie " + gameId + " §7active=" + snapshot.active());
    sender.sendMessage(
        "§7wallets="
            + snapshot.wallets().size()
            + " journal="
            + snapshot.retainedTransactions()
            + " operations="
            + snapshot.knownOperations()
            + " refus="
            + snapshot.rejectedTransactions());
    sender.sendMessage(
        "§7bonus="
            + powerUps.active(gameId).stream()
                .map(value -> value.definition().type().name())
                .toList());
    snapshot
        .wallets()
        .values()
        .forEach(
            wallet ->
                sender.sendMessage(
                    "§f"
                        + wallet.playerId()
                        + " §a"
                        + wallet.balance()
                        + " §7earned="
                        + wallet.totalEarned()
                        + " spent="
                        + wallet.totalSpent()));
    return true;
  }

  private UUID gameId(CommandSender sender, String[] arguments, int index) {
    if (arguments.length > index) {
      return uuid(arguments, index);
    }
    if (sender instanceof Player player) {
      return games
          .gameFor(player.getUniqueId())
          .orElseThrow(() -> new IllegalArgumentException("Précisez l'UUID de la partie."));
    }
    throw new IllegalArgumentException("Précisez l'UUID de la partie.");
  }

  private static Player player(String[] arguments, int index) {
    if (arguments.length <= index) {
      throw new IllegalArgumentException("Joueur manquant.");
    }
    Player player = Bukkit.getPlayerExact(arguments[index]);
    if (player == null) {
      throw new IllegalArgumentException("Joueur en ligne introuvable.");
    }
    return player;
  }

  private static long amount(String[] arguments, int index) {
    if (arguments.length <= index) {
      throw new IllegalArgumentException("Montant manquant.");
    }
    try {
      long amount = Long.parseLong(arguments[index]);
      if (amount < 0) {
        throw new IllegalArgumentException("Le montant doit être positif.");
      }
      return amount;
    } catch (NumberFormatException failure) {
      throw new IllegalArgumentException("Montant invalide.", failure);
    }
  }

  private static UUID uuid(String[] arguments, int index) {
    if (arguments.length <= index) {
      throw new IllegalArgumentException("UUID manquant.");
    }
    try {
      return UUID.fromString(arguments[index]);
    } catch (IllegalArgumentException failure) {
      throw new IllegalArgumentException("UUID invalide.", failure);
    }
  }

  private static void requirePermission(CommandSender sender, String permission) {
    if (!sender.hasPermission(permission)) {
      throw new IllegalArgumentException("Permission manquante : " + permission);
    }
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String alias, String[] arguments) {
    if (arguments.length == 1) {
      return matches(
          arguments[0],
          List.of(
              "balance",
              "set",
              "add",
              "remove",
              "history",
              "transaction",
              "givepowerup",
              "clearpowerups",
              "debug"));
    }
    if (arguments.length == 2 && arguments[0].equalsIgnoreCase("givepowerup")) {
      return matches(
          arguments[1], java.util.Arrays.stream(PowerUpType.values()).map(Enum::name).toList());
    }
    if (arguments.length == 2
        && List.of("balance", "set", "add", "remove", "history")
            .contains(arguments[0].toLowerCase(java.util.Locale.ROOT))) {
      return matches(
          arguments[1], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
    }
    return List.of();
  }

  private static List<String> matches(String prefix, List<String> values) {
    String normalized = prefix.toLowerCase(java.util.Locale.ROOT);
    ArrayList<String> result = new ArrayList<>();
    values.stream()
        .filter(value -> value.toLowerCase(java.util.Locale.ROOT).startsWith(normalized))
        .forEach(result::add);
    return result;
  }
}
