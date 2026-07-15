package fr.heneria.bedwars.plugin;

import fr.heneria.bedwars.api.PluginStatus;
import fr.heneria.bedwars.plugin.bootstrap.BedWarsBootstrap;
import fr.heneria.bedwars.plugin.bootstrap.PluginBootstrap;
import fr.heneria.bedwars.plugin.command.BedWarsCommand;
import fr.heneria.bedwars.plugin.config.GeneralConfiguration;
import fr.heneria.bedwars.plugin.logging.PaperProjectLogger;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/** Paper entry point. All construction and lifecycle work is delegated to a bootstrap. */
public final class HeneriaBedWarsPlugin extends JavaPlugin {
  private PluginBootstrap bootstrap;

  @Override
  public void onEnable() {
    saveDefaultConfig();
    GeneralConfiguration configuration;
    try {
      configuration = GeneralConfiguration.from(getConfig());
      PaperProjectLogger projectLogger = new PaperProjectLogger(getLogger(), configuration.debug());
      bootstrap = new BedWarsBootstrap(getPluginMeta().getVersion(), configuration, projectLogger);
      bootstrap.start();
      registerDiagnosticCommand();
      getLogger().info("HeneriaBedWars " + getPluginMeta().getVersion() + " enabled.");
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
    PluginCommand command =
        Objects.requireNonNull(getCommand("bedwars"), "bedwars command missing from plugin.yml");
    BedWarsCommand executor = new BedWarsCommand(this, bootstrap);
    command.setExecutor(executor);
    command.setTabCompleter(executor);
  }
}
