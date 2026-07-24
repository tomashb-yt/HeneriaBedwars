package fr.heneria.zombie.plugin.command;

import fr.heneria.zombie.api.PluginState;
import fr.heneria.zombie.api.ZombieApi;
import fr.heneria.zombie.core.command.ZombieCommandParser;
import fr.heneria.zombie.plugin.config.ConfigurationManager;
import fr.heneria.zombie.plugin.config.ReloadResult;
import fr.heneria.zombie.plugin.message.MessageService;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Paper adapter for the minimal Ticket 001 command surface. */
public final class ZombieCommand implements CommandExecutor, TabCompleter {

  private final String version;
  private final ZombieApi api;
  private final AtomicReference<PluginState> state;
  private final ZombieCommandParser parser;
  private final ConfigurationManager configurations;
  private final MessageService messages;

  /**
   * Creates the command adapter.
   *
   * @param version plugin version
   * @param api public status API
   * @param state shared lifecycle state
   * @param configurations configuration manager
   * @param messages message renderer
   */
  public ZombieCommand(
      String version,
      ZombieApi api,
      AtomicReference<PluginState> state,
      ConfigurationManager configurations,
      MessageService messages) {
    this.version = Objects.requireNonNull(version, "version");
    this.api = Objects.requireNonNull(api, "api");
    this.state = Objects.requireNonNull(state, "state");
    this.configurations = Objects.requireNonNull(configurations, "configurations");
    this.messages = Objects.requireNonNull(messages, "messages");
    this.parser = new ZombieCommandParser();
  }

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] arguments) {
    if (!sender.hasPermission("zombie.command.use")) {
      sender.sendMessage(messages.render("command.no-permission"));
      return true;
    }
    switch (parser.parse(arguments)) {
      case INFORMATION -> showInformation(sender);
      case HELP -> sender.sendMessage(messages.render("command.help"));
      case RELOAD -> reload(sender);
      case UNKNOWN -> sender.sendMessage(messages.render("command.usage"));
    }
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String alias,
      @NotNull String[] arguments) {
    if (arguments.length != 1) {
      return List.of();
    }
    String prefix = arguments[0].toLowerCase(java.util.Locale.ROOT);
    return List.of("help", "reload").stream()
        .filter(value -> value.startsWith(prefix))
        .filter(value -> !value.equals("reload") || sender.hasPermission("zombie.command.reload"))
        .toList();
  }

  private void showInformation(CommandSender sender) {
    sender.sendMessage(
        messages.render(
            "command.information",
            "version",
            version,
            "state",
            api.state().name(),
            "maps",
            Integer.toString(api.registeredMapCount()),
            "instances",
            Integer.toString(api.activeInstanceCount())));
  }

  private void reload(CommandSender sender) {
    if (!sender.hasPermission("zombie.command.reload")) {
      sender.sendMessage(messages.render("command.no-permission"));
      return;
    }
    if (!state.compareAndSet(PluginState.RUNNING, PluginState.RELOADING)) {
      sender.sendMessage(messages.render("command.reload-busy"));
      return;
    }
    try {
      ReloadResult result = configurations.reload();
      if (result.successful()) {
        sender.sendMessage(
            messages.render(
                "command.reload-success", "warnings", Integer.toString(result.issues().size())));
      } else {
        sender.sendMessage(
            messages.render(
                "command.reload-failure", "errors", Integer.toString(result.issues().size())));
      }
    } finally {
      state.compareAndSet(PluginState.RELOADING, PluginState.RUNNING);
    }
  }
}
