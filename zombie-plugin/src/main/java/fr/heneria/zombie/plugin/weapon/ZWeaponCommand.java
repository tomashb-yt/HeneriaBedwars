package fr.heneria.zombie.plugin.weapon;

import fr.heneria.zombie.core.weapon.WeaponDefinition;
import fr.heneria.zombie.core.weapon.WeaponInstance;
import fr.heneria.zombie.plugin.game.PaperGameRuntime;
import fr.heneria.zombie.plugin.gui.GuiId;
import fr.heneria.zombie.plugin.gui.GuiService;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Permission-checked in-game administration and diagnostics for the weapon engine. */
public final class ZWeaponCommand implements CommandExecutor, TabCompleter {
  private final JavaPlugin plugin;
  private final PaperWeaponService weapons;
  private final WeaponDefinitionLoader definitions;
  private final PaperGameRuntime games;
  private final GuiService guis;

  public ZWeaponCommand(
      JavaPlugin plugin,
      PaperWeaponService weapons,
      WeaponDefinitionLoader definitions,
      PaperGameRuntime games,
      GuiService guis) {
    this.plugin = plugin;
    this.weapons = weapons;
    this.definitions = definitions;
    this.games = games;
    this.guis = guis;
  }

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] arguments) {
    String action = arguments.length == 0 ? "types" : arguments[0].toLowerCase(Locale.ROOT);
    String permission = "zombies.admin.weapon." + (action.equals("gui") ? "types" : action);
    if (!sender.hasPermission(permission)) {
      sender.sendMessage("Permission " + permission + " requise.");
      return true;
    }
    switch (action) {
      case "types" -> list(sender);
      case "info" -> info(sender, arguments);
      case "give" -> give(sender, arguments);
      case "reload" -> reload(sender);
      case "stats" -> stats(sender, arguments);
      case "gui" -> openGui(sender);
      default -> usage(sender);
    }
    return true;
  }

  private void list(CommandSender sender) {
    sender.sendMessage("Armes chargees : " + definitions.current().size());
    weapons
        .types()
        .forEach(
            weapon ->
                sender.sendMessage(
                    weapon.id()
                        + " - "
                        + weapon.category()
                        + " - degats "
                        + weapon.damage().baseDamage()));
  }

  private void info(CommandSender sender, String[] arguments) {
    if (arguments.length < 2) {
      usage(sender);
      return;
    }
    WeaponDefinition weapon =
        definitions.current().find(arguments[1].toLowerCase(Locale.ROOT)).orElse(null);
    if (weapon == null) {
      sender.sendMessage("Arme inconnue.");
      return;
    }
    sender.sendMessage(
        weapon.displayName()
            + " - "
            + weapon.category()
            + " / "
            + weapon.rarity()
            + " - mode "
            + weapon.fire().mode());
    sender.sendMessage(
        "Degats "
            + weapon.damage().baseDamage()
            + " - chargeur "
            + weapon.ammo().magazineSize()
            + " - reserve "
            + weapon.ammo().maximumReserve()
            + " - PAP "
            + weapon.upgrades().size());
  }

  private void give(CommandSender sender, String[] arguments) {
    if (arguments.length < 3) {
      usage(sender);
      return;
    }
    Player target = Bukkit.getPlayerExact(arguments[1]);
    WeaponDefinition weapon =
        definitions.current().find(arguments[2].toLowerCase(Locale.ROOT)).orElse(null);
    if (target == null || weapon == null) {
      sender.sendMessage("Joueur hors ligne ou arme inconnue.");
      return;
    }
    var game = games.gameFor(target.getUniqueId());
    if (game.isEmpty()
        || weapons.give(game.get(), target, weapon, Bukkit.getCurrentTick()).isEmpty()) {
      sender.sendMessage("Le joueur doit etre dans une partie active.");
      return;
    }
    sender.sendMessage("Arme donnee a " + target.getName() + ".");
  }

  private void reload(CommandSender sender) {
    sender.sendMessage("Rechargement atomique des armes...");
    definitions
        .reloadAsync()
        .whenComplete(
            (registry, failure) ->
                plugin
                    .getServer()
                    .getScheduler()
                    .runTask(
                        plugin,
                        () ->
                            sender.sendMessage(
                                failure == null
                                    ? registry.size() + " definition(s) activee(s)."
                                    : "Rechargement refuse ; le dernier registre valide reste actif.")));
  }

  private void stats(CommandSender sender, String[] arguments) {
    Player target =
        arguments.length >= 2
            ? Bukkit.getPlayerExact(arguments[1])
            : sender instanceof Player player ? player : null;
    if (target == null) {
      sender.sendMessage("Joueur hors ligne.");
      return;
    }
    List<WeaponInstance> values = List.copyOf(weapons.player(target.getUniqueId()));
    sender.sendMessage("Armes actives de " + target.getName() + " : " + values.size());
    values.forEach(
        weapon -> {
          var value = weapon.snapshot();
          sender.sendMessage(
              value.weaponId()
                  + " - "
                  + value.magazine()
                  + "/"
                  + value.reserve()
                  + " - tirs "
                  + value.shots()
                  + " - touches "
                  + value.hits()
                  + " - headshots "
                  + value.headshots());
        });
  }

  private void openGui(CommandSender sender) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("Cette commande doit etre utilisee en jeu.");
      return;
    }
    guis.openHome(player, new GuiId("weapon-browser"));
  }

  private static void usage(CommandSender sender) {
    sender.sendMessage("/zweapon <types|info <id>|give <joueur> <id>|reload|stats [joueur]|gui>");
  }

  @Override
  public @Nullable List<String> onTabComplete(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String alias,
      @NotNull String[] arguments) {
    if (arguments.length == 1) {
      return List.of("types", "info", "give", "reload", "stats", "gui");
    }
    if (arguments.length == 2
        && (arguments[0].equalsIgnoreCase("info") || arguments[0].equalsIgnoreCase("give"))) {
      return arguments[0].equalsIgnoreCase("give")
          ? Bukkit.getOnlinePlayers().stream().map(Player::getName).sorted().toList()
          : definitions.current().all().stream().map(WeaponDefinition::id).sorted().toList();
    }
    if (arguments.length == 3 && arguments[0].equalsIgnoreCase("give")) {
      return definitions.current().all().stream().map(WeaponDefinition::id).sorted().toList();
    }
    return List.of();
  }
}
