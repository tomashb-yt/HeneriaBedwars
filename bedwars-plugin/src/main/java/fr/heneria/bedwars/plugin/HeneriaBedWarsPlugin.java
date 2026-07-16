package fr.heneria.bedwars.plugin;

import fr.heneria.bedwars.api.PluginStatus;
import fr.heneria.bedwars.core.arena.ArenaRegistry;
import fr.heneria.bedwars.core.arena.ArenaService;
import fr.heneria.bedwars.core.arena.ArenaValidator;
import fr.heneria.bedwars.plugin.arena.ArenaLifecycleComponent;
import fr.heneria.bedwars.plugin.arena.YamlArenaRepository;
import fr.heneria.bedwars.plugin.bootstrap.BedWarsBootstrap;
import fr.heneria.bedwars.plugin.bootstrap.PluginBootstrap;
import fr.heneria.bedwars.plugin.command.BedWarsCommand;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import fr.heneria.bedwars.plugin.gui.BukkitGuiService;
import fr.heneria.bedwars.plugin.item.BukkitItemService;
import fr.heneria.bedwars.plugin.logging.BukkitProjectLogger;
import java.time.Clock;
import java.util.logging.Level;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/** Paper entry point. All construction and lifecycle work is delegated to a bootstrap. */
public final class HeneriaBedWarsPlugin extends JavaPlugin {
  private PluginBootstrap bootstrap;
  private ConfigurationService configurations;
  private BukkitGuiService guiService;
  private BukkitItemService itemService;
  private ArenaService arenaService;

  @Override
  public void onEnable() {
    try {
      BukkitProjectLogger projectLogger = new BukkitProjectLogger(getLogger(), false);
      configurations =
          new ConfigurationService(
              getDataFolder().toPath(),
              this::getResource,
              projectLogger,
              Clock.systemDefaultZone());
      configurations.initialize();
      projectLogger.setDebug(configurations.snapshot().plugin().debug());
      Clock clock = Clock.systemDefaultZone();
      arenaService =
          new ArenaService(
              new YamlArenaRepository(getDataFolder().toPath(), clock),
              new ArenaRegistry(),
              new ArenaValidator(world -> getServer().getWorld(world) != null),
              clock);
      itemService = new BukkitItemService(this, configurations, projectLogger);
      guiService = new BukkitGuiService(this, configurations, itemService, projectLogger);
      bootstrap =
          new BedWarsBootstrap(
              getDescription().getVersion(),
              configurations,
              arenaService,
              new ArenaLifecycleComponent(arenaService, projectLogger),
              itemService,
              guiService,
              projectLogger);
      bootstrap.start();
      registerDiagnosticCommand();
      getLogger().info("HeneriaBedWars " + getDescription().getVersion() + " enabled.");
    } catch (Exception exception) {
      getLogger()
          .log(Level.SEVERE, "Critical startup failure; disabling HeneriaBedWars.", exception);
      getServer().getPluginManager().disablePlugin(this);
    }
  }

  @Override
  public void onDisable() {
    if (bootstrap == null || bootstrap.status() != PluginStatus.RUNNING) {
      return;
    }
    try {
      bootstrap.stop();
      getLogger().info("HeneriaBedWars disabled cleanly.");
    } catch (Exception exception) {
      getLogger().log(Level.SEVERE, "HeneriaBedWars failed during shutdown.", exception);
    }
  }

  private void registerDiagnosticCommand() {
    PluginCommand command = getCommand("bedwars");
    if (command == null) {
      throw new IllegalStateException("The 'bedwars' command is not declared in plugin.yml");
    }
    BedWarsCommand executor =
        new BedWarsCommand(this, bootstrap, configurations, arenaService, itemService, guiService);
    command.setExecutor(executor);
    command.setTabCompleter(executor);
  }
}
