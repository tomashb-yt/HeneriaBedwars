package fr.heneria.bedwars.plugin.command;

import fr.heneria.bedwars.core.command.BedWarsCommandLogic;
import fr.heneria.bedwars.core.command.CommandDiagnostics;
import fr.heneria.bedwars.plugin.bootstrap.PluginBootstrap;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Implements the Bukkit-compatible {@code /bedwars} administrative command. */
public final class BedWarsCommand implements CommandExecutor, TabCompleter {
  private final JavaPlugin plugin;
  private final PluginBootstrap bootstrap;
  private final BedWarsCommandLogic logic = new BedWarsCommandLogic();

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
    CommandDiagnostics diagnostics =
        new CommandDiagnostics(
            plugin.getDescription().getName(),
            plugin.getDescription().getVersion(),
            System.getProperty("java.version"),
            plugin.getServer().getVersion(),
            bootstrap.status().name().toLowerCase(),
            bootstrap.serviceCount());
    for (String message :
        logic.execute(
            sender.hasPermission(BedWarsCommandLogic.PERMISSION), label, args, diagnostics)) {
      sender.sendMessage(message);
    }
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String alias,
      @NotNull String[] args) {
    return logic.complete(sender.hasPermission(BedWarsCommandLogic.PERMISSION), args);
  }
}
