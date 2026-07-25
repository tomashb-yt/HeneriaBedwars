package fr.heneria.zombie.plugin.world;

import java.util.Objects;
import java.util.concurrent.Executor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/** Executor that marshals platform operations onto the Paper server thread. */
public final class PaperMainThreadExecutor implements Executor {

  private final Plugin plugin;

  /**
   * Creates the executor.
   *
   * @param plugin owning plugin
   */
  public PaperMainThreadExecutor(Plugin plugin) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
  }

  @Override
  public void execute(Runnable command) {
    Objects.requireNonNull(command, "command");
    if (Bukkit.isPrimaryThread()) {
      command.run();
    } else {
      Bukkit.getScheduler().runTask(plugin, command);
    }
  }
}
