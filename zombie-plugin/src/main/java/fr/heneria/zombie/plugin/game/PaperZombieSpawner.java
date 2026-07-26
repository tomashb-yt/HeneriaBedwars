package fr.heneria.zombie.plugin.game;

import fr.heneria.zombie.core.game.ZombieSpawner;
import fr.heneria.zombie.plugin.player.PaperAttributeResolver;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Zombie;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** Minimal Paper zombie adapter used until the advanced AI ticket. */
public final class PaperZombieSpawner implements ZombieSpawner {
  private final NamespacedKey gameKey;
  private final NamespacedKey roundKey;

  public PaperZombieSpawner(JavaPlugin plugin) {
    gameKey = new NamespacedKey(plugin, "zombie_game");
    roundKey = new NamespacedKey(plugin, "zombie_round");
  }

  @Override
  public Optional<UUID> spawn(SpawnRequest request) {
    var world = Bukkit.getWorld(request.worldName());
    if (world == null) {
      return Optional.empty();
    }
    var point = request.point();
    Zombie zombie =
        world.spawn(
            new Location(world, point.x(), point.y(), point.z(), point.yaw(), point.pitch()),
            Zombie.class,
            entity -> {
              entity.setRemoveWhenFarAway(false);
              entity.setCanPickupItems(false);
              entity.setShouldBurnInDay(false);
              entity.setFireTicks(0);
              entity
                  .getPersistentDataContainer()
                  .set(gameKey, PersistentDataType.STRING, request.gameId().toString());
              entity
                  .getPersistentDataContainer()
                  .set(roundKey, PersistentDataType.INTEGER, request.round());
              AttributeInstance health = entity.getAttribute(PaperAttributeResolver.maxHealth());
              if (health != null) {
                health.setBaseValue(request.health());
                entity.setHealth(request.health());
              }
            });
    return Optional.of(zombie.getUniqueId());
  }

  @Override
  public void remove(UUID entityId) {
    Entity entity = Bukkit.getEntity(entityId);
    if (entity != null) {
      entity.remove();
    }
  }
}
