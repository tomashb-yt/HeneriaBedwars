package fr.heneria.zombie.plugin.bootstrap;

import fr.heneria.zombie.api.PluginState;
import fr.heneria.zombie.api.ZombieApi;
import fr.heneria.zombie.core.bootstrap.ServiceRegistry;
import fr.heneria.zombie.core.config.ConfigurationIssue;
import fr.heneria.zombie.core.config.ZombieSettingsValidator;
import fr.heneria.zombie.plugin.api.PaperZombieApi;
import fr.heneria.zombie.plugin.command.ZombieCommand;
import fr.heneria.zombie.plugin.config.ConfigurationManager;
import fr.heneria.zombie.plugin.message.MessageService;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/** Explicit composition root for all Ticket 001 services and adapters. */
public final class ZombieBootstrap {

  private final JavaPlugin plugin;
  private final AtomicReference<PluginState> state;
  private final ServiceRegistry services = new ServiceRegistry();
  private ZombieApi api;

  /**
   * Creates the bootstrap.
   *
   * @param plugin owning Paper plugin
   * @param state shared lifecycle state
   */
  public ZombieBootstrap(JavaPlugin plugin, AtomicReference<PluginState> state) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.state = Objects.requireNonNull(state, "state");
  }

  /**
   * Builds and registers the initial service graph.
   *
   * @throws Exception when a critical service cannot initialize
   */
  public void start() throws Exception {
    ConfigurationManager configurations =
        new ConfigurationManager(
            plugin.getDataFolder().toPath(),
            plugin.getClass().getClassLoader(),
            new ZombieSettingsValidator());
    List<ConfigurationIssue> issues = configurations.initialize();
    issues.forEach(
        issue ->
            plugin
                .getLogger()
                .warning(
                    "Configuration "
                        + issue.severity()
                        + " ["
                        + issue.path()
                        + "]: "
                        + issue.message()));

    MessageService messages = new MessageService(configurations);
    api = new PaperZombieApi(state, () -> 0, () -> 0);
    services.register(ConfigurationManager.class, configurations);
    services.register(MessageService.class, messages);
    services.register(ZombieApi.class, api);

    PluginCommand command = plugin.getCommand("zombie");
    if (command == null) {
      throw new IllegalStateException("Command zombie is missing from plugin.yml");
    }
    ZombieCommand executor =
        new ZombieCommand(
            plugin.getPluginMeta().getVersion(), api, state, configurations, messages);
    command.setExecutor(executor);
    command.setTabCompleter(executor);

    plugin
        .getServer()
        .getServicesManager()
        .register(ZombieApi.class, api, plugin, ServicePriority.Normal);
    plugin
        .getLogger()
        .info("Services initialized; no gameplay listener is registered by Ticket 001.");
  }

  /** Releases API registration and all references owned by the bootstrap. */
  public void stop() {
    if (api != null) {
      plugin.getServer().getServicesManager().unregister(ZombieApi.class, api);
      api = null;
    }
    services.clear();
  }
}
