package fr.heneria.bedwars.plugin.game;

import fr.heneria.bedwars.core.config.WorldManagerSettings;
import fr.heneria.bedwars.core.game.RuntimeLocation;
import fr.heneria.bedwars.core.game.RuntimePlayerGateway;
import fr.heneria.bedwars.core.game.RuntimeWorldHandle;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Local-server player movement adapter; a proxy implementation can replace this port later. */
public final class BukkitRuntimePlayerGateway implements RuntimePlayerGateway {
  private final JavaPlugin plugin;
  private final WorldManagerSettings settings;

  public BukkitRuntimePlayerGateway(JavaPlugin plugin, WorldManagerSettings settings) {
    this.plugin = plugin;
    this.settings = settings;
  }

  @Override
  public CompletionStage<Boolean> enter(
      UUID playerId, RuntimeWorldHandle handle, RuntimeLocation destination) {
    return main(
        () -> {
          Player player = Bukkit.getPlayer(playerId);
          World world = Bukkit.getWorld(handle.worldName());
          if (player == null || world == null) return false;
          Location target =
              new Location(
                  world,
                  destination.x(),
                  destination.y(),
                  destination.z(),
                  destination.yaw(),
                  destination.pitch());
          world.getChunkAt(target).load();
          return player.teleport(target);
        });
  }

  @Override
  public CompletionStage<Boolean> leave(UUID playerId) {
    return main(
        () -> {
          Player player = Bukkit.getPlayer(playerId);
          if (player == null) return true;
          World fallback = Bukkit.getWorld(settings.fallbackWorld());
          return fallback != null && player.teleport(fallback.getSpawnLocation());
        });
  }

  private CompletionStage<Boolean> main(java.util.concurrent.Callable<Boolean> action) {
    CompletableFuture<Boolean> result = new CompletableFuture<>();
    Runnable task =
        () -> {
          try {
            result.complete(action.call());
          } catch (Exception exception) {
            result.completeExceptionally(exception);
          }
        };
    if (Bukkit.isPrimaryThread()) task.run();
    else plugin.getServer().getScheduler().runTask(plugin, task);
    return result;
  }
}
