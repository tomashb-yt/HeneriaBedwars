package fr.heneria.bedwars.plugin.command;

import fr.heneria.bedwars.plugin.bootstrap.PluginBootstrap;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Implements the deliberately small {@code /bedwars version} diagnostic command. */
public final class BedWarsCommand implements CommandExecutor, TabCompleter {
  private final JavaPlugin plugin;
  private final PluginBootstrap bootstrap;

  public BedWarsCommand(JavaPlugin plugin, PluginBootstrap bootstrap) {
    this.plugin = plugin;
    this.bootstrap = bootstrap;
  }

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] args) {
    if (args.length != 1 || !args[0].equalsIgnoreCase("version")) {
      sender.sendMessage("§cUsage: /" + label + " version");
      return true;
    }
    sender.sendMessage("§6HeneriaBedWars §7- diagnostic");
    sender.sendMessage("§ePlugin: §f" + plugin.getPluginMeta().getName());
    sender.sendMessage("§eVersion: §f" + plugin.getPluginMeta().getVersion());
    sender.sendMessage("§eJava: §f" + System.getProperty("java.version"));
    sender.sendMessage("§eServeur: §f" + plugin.getServer().getVersion());
    sender.sendMessage("§eÉtat: §f" + bootstrap.status());
    sender.sendMessage("§eServices internes: §f" + bootstrap.serviceCount());
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String alias,
      @NotNull String[] args) {
    if (args.length == 1 && "version".startsWith(args[0].toLowerCase())) {
      return List.of("version");
    }
    return List.of();
  }
}
