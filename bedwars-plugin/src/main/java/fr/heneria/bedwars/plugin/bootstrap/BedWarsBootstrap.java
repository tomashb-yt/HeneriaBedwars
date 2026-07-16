package fr.heneria.bedwars.plugin.bootstrap;

import fr.heneria.bedwars.api.HeneriaBedWarsApi;
import fr.heneria.bedwars.api.PluginStatus;
import fr.heneria.bedwars.core.arena.ArenaService;
import fr.heneria.bedwars.core.gui.TextInputService;
import fr.heneria.bedwars.core.lifecycle.LifecycleComponent;
import fr.heneria.bedwars.core.lifecycle.LifecycleManager;
import fr.heneria.bedwars.core.logging.ProjectLogger;
import fr.heneria.bedwars.core.service.ServiceRegistry;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import fr.heneria.bedwars.plugin.gui.BukkitGuiService;
import fr.heneria.bedwars.plugin.gui.GuiService;
import fr.heneria.bedwars.plugin.item.ItemService;
import java.util.List;
import java.util.Objects;

/** Default composition root for the Ticket 001 foundation. */
public final class BedWarsBootstrap implements PluginBootstrap, HeneriaBedWarsApi {
  private final String version;
  private final ProjectLogger logger;
  private final ServiceRegistry services = new ServiceRegistry();
  private final LifecycleManager lifecycle;
  private PluginStatus status = PluginStatus.STOPPED;

  public BedWarsBootstrap(
      String version,
      ConfigurationService configuration,
      ArenaService arenaService,
      LifecycleComponent arenaLifecycle,
      TextInputService textInputService,
      LifecycleComponent textInputLifecycle,
      ItemService itemService,
      BukkitGuiService guiService,
      ProjectLogger logger) {
    this.version = Objects.requireNonNull(version, "version");
    this.logger = Objects.requireNonNull(logger, "logger");
    services.register(ConfigurationService.class, configuration);
    services.register(ArenaService.class, arenaService);
    services.register(TextInputService.class, textInputService);
    services.register(ItemService.class, itemService);
    services.register(GuiService.class, guiService);
    services.register(HeneriaBedWarsApi.class, this);
    lifecycle =
        new LifecycleManager(
            List.of(new FoundationComponent(), arenaLifecycle, textInputLifecycle, guiService),
            logger);
  }

  @Override
  public void start() throws Exception {
    status = PluginStatus.STARTING;
    try {
      lifecycle.startAll();
      status = PluginStatus.RUNNING;
    } catch (Exception exception) {
      status = PluginStatus.FAILED;
      throw exception;
    }
  }

  @Override
  public void stop() throws Exception {
    status = PluginStatus.STOPPING;
    try {
      lifecycle.stopAll();
      status = PluginStatus.STOPPED;
    } catch (Exception exception) {
      status = PluginStatus.FAILED;
      throw exception;
    }
  }

  @Override
  public String version() {
    return version;
  }

  @Override
  public PluginStatus status() {
    return status;
  }

  @Override
  public int serviceCount() {
    return services.size();
  }

  private final class FoundationComponent implements LifecycleComponent {
    @Override
    public String name() {
      return "foundation";
    }

    @Override
    public void start() {
      logger.info("Core foundation initialized with " + services.size() + " services.");
    }

    @Override
    public void stop() {
      logger.info("Core foundation stopped.");
    }
  }
}
