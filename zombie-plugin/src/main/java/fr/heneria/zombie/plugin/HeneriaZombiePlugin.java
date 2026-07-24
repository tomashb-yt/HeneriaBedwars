package fr.heneria.zombie.plugin;

import fr.heneria.zombie.api.PluginState;
import fr.heneria.zombie.plugin.bootstrap.ZombieBootstrap;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.plugin.java.JavaPlugin;

/** Paper entry point; orchestration is delegated to {@link ZombieBootstrap}. */
public final class HeneriaZombiePlugin extends JavaPlugin {

  private final AtomicReference<PluginState> state = new AtomicReference<>(PluginState.STOPPED);
  private ZombieBootstrap bootstrap;

  @Override
  public void onEnable() {
    state.set(PluginState.STARTING);
    getLogger().info("Starting HeneriaZombie " + getPluginMeta().getVersion() + "...");
    try {
      requireJava21();
      bootstrap = new ZombieBootstrap(this, state);
      bootstrap.start();
      state.set(PluginState.RUNNING);
      getLogger().info("HeneriaZombie is running.");
    } catch (Exception criticalFailure) {
      state.set(PluginState.FAILED);
      getLogger().log(java.util.logging.Level.SEVERE, "Critical startup failure", criticalFailure);
      if (bootstrap != null) {
        bootstrap.stop();
      }
      getServer().getPluginManager().disablePlugin(this);
    }
  }

  @Override
  public void onDisable() {
    PluginState previous = state.getAndSet(PluginState.STOPPING);
    if (bootstrap != null) {
      bootstrap.stop();
      bootstrap = null;
    }
    state.set(PluginState.STOPPED);
    if (previous != PluginState.STOPPED) {
      getLogger().info("HeneriaZombie stopped cleanly.");
    }
  }

  private static void requireJava21() {
    if (Runtime.version().feature() < 21) {
      throw new IllegalStateException("HeneriaZombie requires Java 21 or newer");
    }
  }
}
