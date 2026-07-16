package fr.heneria.bedwars.plugin.arena;

import fr.heneria.bedwars.core.arena.ArenaReloadResult;
import fr.heneria.bedwars.core.arena.ArenaService;
import fr.heneria.bedwars.core.lifecycle.LifecycleComponent;
import fr.heneria.bedwars.core.logging.ProjectLogger;
import java.util.Objects;

/** Loads arena definitions without allowing one broken YAML to disable the plugin. */
public final class ArenaLifecycleComponent implements LifecycleComponent {
  private final ArenaService arenas;
  private final ProjectLogger logger;

  public ArenaLifecycleComponent(ArenaService arenas, ProjectLogger logger) {
    this.arenas = Objects.requireNonNull(arenas, "arenas");
    this.logger = Objects.requireNonNull(logger, "logger");
  }

  @Override
  public String name() {
    return "arenas";
  }

  @Override
  public void start() throws Exception {
    ArenaReloadResult result = arenas.reload();
    logger.info("Loaded " + result.loaded() + " arena definition(s).");
    result.failures().forEach(failure -> logger.warning("Arena file ignored: " + failure));
  }

  @Override
  public void stop() {}
}
