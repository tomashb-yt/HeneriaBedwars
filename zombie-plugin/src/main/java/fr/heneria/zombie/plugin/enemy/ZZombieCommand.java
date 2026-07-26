package fr.heneria.zombie.plugin.enemy;

import fr.heneria.zombie.core.editor.MapPoint;
import fr.heneria.zombie.core.enemy.ZombieDamageType;
import fr.heneria.zombie.core.enemy.ZombieInstance;
import fr.heneria.zombie.core.enemy.ZombieRemovalReason;
import fr.heneria.zombie.core.game.ZombieSpawner;
import fr.heneria.zombie.core.instance.GameInstanceSnapshot;
import fr.heneria.zombie.plugin.game.PaperGameRuntime;
import fr.heneria.zombie.plugin.instance.InstanceCoordinator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Permission-checked diagnostics and operations for the enemy engine. */
public final class ZZombieCommand implements CommandExecutor, TabCompleter {
  private final PaperZombieEngine engine;
  private final ZombieDefinitionLoader definitions;
  private final InstanceCoordinator instances;
  private final PaperGameRuntime games;
  private final JavaPlugin plugin;
  private final Logger logger;

  public ZZombieCommand(
      PaperZombieEngine engine,
      ZombieDefinitionLoader definitions,
      InstanceCoordinator instances,
      PaperGameRuntime games,
      JavaPlugin plugin) {
    this.engine = engine;
    this.definitions = definitions;
    this.instances = instances;
    this.games = games;
    this.plugin = plugin;
    this.logger = plugin.getLogger();
  }

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] arguments) {
    String action = arguments.length == 0 ? "list" : arguments[0].toLowerCase(Locale.ROOT);
    String permission = "zombies.admin.zombie." + permissionSuffix(action);
    if (!sender.hasPermission(permission)) {
      sender.sendMessage("Permission " + permission + " requise.");
      return true;
    }
    switch (action) {
      case "list" -> list(sender);
      case "types" -> types(sender);
      case "info", "debug" -> info(sender, arguments);
      case "spawn" -> spawn(sender, arguments);
      case "kill" -> kill(sender, arguments);
      case "removeall" -> removeAll(sender, arguments);
      case "reload" -> reload(sender);
      default -> usage(sender);
    }
    return true;
  }

  private void list(CommandSender sender) {
    sender.sendMessage("Zombies actifs : " + engine.activeCount());
    instances
        .activeInstances()
        .forEach(
            instance ->
                sender.sendMessage(
                    shortId(instance.id())
                        + " : "
                        + engine.game(instance.id()).size()
                        + " zombie(s)"));
  }

  private void types(CommandSender sender) {
    sender.sendMessage("Types chargés : " + engine.types().size());
    engine
        .types()
        .forEach(
            type ->
                sender.sendMessage(
                    type.id()
                        + " • "
                        + type.category()
                        + " • entité "
                        + type.entityType()
                        + " • manche "
                        + type.spawnRules().minimumRound()
                        + "+"));
  }

  private void info(CommandSender sender, String[] arguments) {
    if (arguments.length < 2) {
      usage(sender);
      return;
    }
    Optional<ZombieInstance> found = resolveZombie(arguments[1]);
    if (found.isEmpty()) {
      sender.sendMessage("Zombie introuvable ou identifiant ambigu.");
      return;
    }
    ZombieInstance.Snapshot value = found.get().snapshot();
    sender.sendMessage(
        "Zombie " + value.id() + " • type " + value.typeId() + " • entité " + value.entityId());
    sender.sendMessage(
        "Partie "
            + shortId(value.gameId())
            + " • manche "
            + value.round()
            + " • état "
            + value.state());
    sender.sendMessage(
        "Vie "
            + decimal(value.health())
            + "/"
            + decimal(value.attributes().maximumHealth())
            + " • dégâts "
            + decimal(value.attributes().attackDamage())
            + " • vitesse "
            + decimal(value.attributes().movementSpeed()));
    sender.sendMessage(
        "Cible "
            + value.targetPlayerId().map(UUID::toString).orElse("aucune")
            + " • spawn "
            + value.spawnId()
            + " • dernier mouvement "
            + value.lastMovementTick());
    sender.sendMessage(
        "Capacités " + found.get().definition().abilities() + " • cooldowns " + value.cooldowns());
  }

  private void spawn(CommandSender sender, String[] arguments) {
    if (!(sender instanceof Player player) || arguments.length < 2) {
      sender.sendMessage("/zzombie spawn <type> [game] doit être utilisé en jeu.");
      return;
    }
    String type = arguments[1].toLowerCase(Locale.ROOT);
    if (definitions.current().find(type).isEmpty()) {
      sender.sendMessage("Type inconnu : " + type);
      return;
    }
    Optional<UUID> gameId =
        arguments.length >= 3 ? resolveGame(arguments[2]) : games.gameFor(player.getUniqueId());
    if (gameId.isEmpty()) {
      sender.sendMessage("Partie introuvable ; rejoignez une partie ou indiquez son UUID.");
      return;
    }
    var snapshot = games.snapshot(gameId.get());
    if (snapshot.isEmpty() || snapshot.get().round().isEmpty()) {
      sender.sendMessage("La partie doit avoir une manche active.");
      return;
    }
    Location location = player.getLocation();
    Optional<UUID> spawned =
        engine.spawn(
            new ZombieSpawner.SpawnRequest(
                gameId.get(),
                snapshot.get().round().orElseThrow().number(),
                location.getWorld().getName(),
                "admin",
                new MapPoint(
                    location.getWorld().getName(),
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    location.getYaw(),
                    location.getPitch()),
                Optional.empty(),
                Set.of(type)));
    sender.sendMessage(
        spawned
            .map(value -> "Zombie créé : " + value)
            .orElse("Création refusée à cette position."));
  }

  private void kill(CommandSender sender, String[] arguments) {
    if (arguments.length < 2) {
      usage(sender);
      return;
    }
    Optional<ZombieInstance> zombie = resolveZombie(arguments[1]);
    if (zombie.isEmpty()) {
      sender.sendMessage("Zombie introuvable ou identifiant ambigu.");
      return;
    }
    engine.damage(
        zombie.get().entityId(),
        sender instanceof Player player ? player.getUniqueId() : null,
        ZombieDamageType.CUSTOM,
        zombie.get().snapshot().health(),
        false);
    sender.sendMessage("Élimination demandée.");
  }

  private void removeAll(CommandSender sender, String[] arguments) {
    if (arguments.length < 2) {
      usage(sender);
      return;
    }
    Optional<UUID> game = resolveGame(arguments[1]);
    if (game.isEmpty()) {
      sender.sendMessage("Partie introuvable ou identifiant ambigu.");
      return;
    }
    int count = engine.game(game.get()).size();
    engine.removeAll(game.get(), ZombieRemovalReason.DESPAWNED_ADMIN);
    sender.sendMessage(count + " zombie(s) supprimé(s), sans récompense.");
  }

  private void reload(CommandSender sender) {
    sender.sendMessage("Rechargement des définitions en arrière-plan…");
    definitions
        .reloadAsync()
        .whenComplete(
            (registry, failure) ->
                plugin
                    .getServer()
                    .getScheduler()
                    .runTask(
                        plugin,
                        () -> {
                          if (failure == null) {
                            sender.sendMessage(
                                registry.size()
                                    + " type(s) activé(s). Les zombies existants conservent leur snapshot.");
                          } else {
                            logger.warning(failure.getMessage());
                            sender.sendMessage(
                                "Rechargement refusé ; le dernier registre valide reste actif.");
                          }
                        }));
  }

  private Optional<ZombieInstance> resolveZombie(String prefix) {
    try {
      UUID exact = UUID.fromString(prefix);
      return engine.find(exact);
    } catch (IllegalArgumentException ignored) {
      List<ZombieInstance> matches =
          instances.activeInstances().stream()
              .flatMap(instance -> engine.game(instance.id()).stream())
              .filter(
                  zombie ->
                      zombie.id().toString().startsWith(prefix.toLowerCase(Locale.ROOT))
                          || zombie
                              .entityId()
                              .toString()
                              .startsWith(prefix.toLowerCase(Locale.ROOT)))
              .limit(2)
              .toList();
      return matches.size() == 1 ? Optional.of(matches.getFirst()) : Optional.empty();
    }
  }

  private Optional<UUID> resolveGame(String prefix) {
    List<UUID> matches =
        instances.activeInstances().stream()
            .map(GameInstanceSnapshot::id)
            .filter(id -> id.toString().startsWith(prefix.toLowerCase(Locale.ROOT)))
            .limit(2)
            .toList();
    return matches.size() == 1 ? Optional.of(matches.getFirst()) : Optional.empty();
  }

  private static String permissionSuffix(String action) {
    return switch (action) {
      case "types" -> "types";
      case "info" -> "info";
      case "spawn" -> "spawn";
      case "kill" -> "kill";
      case "removeall" -> "remove";
      case "debug" -> "debug";
      case "reload" -> "reload";
      default -> "list";
    };
  }

  @Override
  public @Nullable List<String> onTabComplete(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String alias,
      @NotNull String[] arguments) {
    if (arguments.length == 1) {
      return List.of("list", "types", "info", "spawn", "kill", "removeall", "debug", "reload");
    }
    if (arguments.length == 2 && arguments[0].equalsIgnoreCase("spawn")) {
      return engine.types().stream().map(value -> value.id()).sorted().toList();
    }
    if (arguments.length == 2
        && (arguments[0].equalsIgnoreCase("info")
            || arguments[0].equalsIgnoreCase("debug")
            || arguments[0].equalsIgnoreCase("kill"))) {
      return instances.activeInstances().stream()
          .flatMap(instance -> engine.game(instance.id()).stream())
          .map(zombie -> shortId(zombie.id()))
          .toList();
    }
    if ((arguments.length == 2 && arguments[0].equalsIgnoreCase("removeall"))
        || (arguments.length == 3 && arguments[0].equalsIgnoreCase("spawn"))) {
      return instances.activeInstances().stream().map(value -> shortId(value.id())).toList();
    }
    return List.of();
  }

  private static String shortId(UUID id) {
    return id.toString().substring(0, 8);
  }

  private static String decimal(double value) {
    return String.format(Locale.ROOT, "%.2f", value);
  }

  private static void usage(CommandSender sender) {
    sender.sendMessage(
        "/zzombie <list|types|info|spawn|kill|removeall|debug|reload> [identifiant]");
  }
}
