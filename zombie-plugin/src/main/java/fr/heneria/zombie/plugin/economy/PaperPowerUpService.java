package fr.heneria.zombie.plugin.economy;

import fr.heneria.zombie.core.powerup.PowerUpDrop;
import fr.heneria.zombie.core.powerup.PowerUpDropService;
import fr.heneria.zombie.core.powerup.PowerUpService;
import fr.heneria.zombie.core.powerup.PowerUpType;
import fr.heneria.zombie.plugin.enemy.PaperZombieEngine;
import fr.heneria.zombie.plugin.weapon.PaperWeaponService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Paper presentation and instant-effect adapter for the core drop and power-up engines.
 *
 * <p>Collection and expiry are processed by the existing grouped game tick; no task is allocated
 * per drop.
 */
public final class PaperPowerUpService {
  private static final MiniMessage MINI = MiniMessage.miniMessage();
  private final PowerUpService powerUps;
  private final PowerUpDropService drops;
  private final PaperZombieEngine zombies;
  private final Function<UUID, Collection<UUID>> gamePlayers;
  private final Map<UUID, UUID> entities = new ConcurrentHashMap<>();
  private final Map<UUID, Location> anchors = new ConcurrentHashMap<>();
  private final Map<UUID, Map<UUID, Long>> collected = new ConcurrentHashMap<>();
  private PaperWeaponService weapons;

  public PaperPowerUpService(
      PowerUpService powerUps,
      PowerUpDropService drops,
      PaperZombieEngine zombies,
      Function<UUID, Collection<UUID>> gamePlayers) {
    this.powerUps = powerUps;
    this.drops = drops;
    this.zombies = zombies;
    this.gamePlayers = gamePlayers;
  }

  public void weapons(PaperWeaponService weapons) {
    if (this.weapons != null) {
      throw new IllegalStateException("Weapon service already attached");
    }
    this.weapons = java.util.Objects.requireNonNull(weapons, "weapons");
  }

  public void zombieDefeated(UUID gameId, int round, Location location) {
    if (location == null || location.getWorld() == null) {
      return;
    }
    drops
        .roll(gameId, round)
        .ifPresent(
            drop -> {
              Location anchor = safeGround(location);
              Item item =
                  anchor
                      .getWorld()
                      .spawn(
                          anchor, Item.class, spawned -> spawned.setItemStack(item(drop.type())));
              item.setCanMobPickup(false);
              item.setCanPlayerPickup(false);
              item.setGlowing(true);
              item.setGravity(false);
              item.setVelocity(new org.bukkit.util.Vector());
              item.setUnlimitedLifetime(true);
              entities.put(drop.id(), item.getUniqueId());
              anchors.put(drop.id(), anchor);
            });
  }

  public void tick() {
    powerUps.tick();
    drops.tick().forEach(drop -> removeEntity(drop.id()));
    for (PowerUpDrop drop : new ArrayList<>(allActiveDrops())) {
      Entity entity = Bukkit.getEntity(entities.get(drop.id()));
      if (entity == null || !entity.isValid()) {
        continue;
      }
      Location anchor = anchors.get(drop.id());
      if (anchor != null) {
        entity.setVelocity(new org.bukkit.util.Vector());
        if (!entity.getWorld().equals(anchor.getWorld())
            || entity.getLocation().distanceSquared(anchor) > 0.01) {
          entity.teleport(anchor);
        }
      }
      for (UUID playerId : gamePlayers.apply(drop.gameId())) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null
            || !player.isOnline()
            || !player.getWorld().equals(entity.getWorld())
            || player.getLocation().distanceSquared(entity.getLocation()) > 2.25) {
          continue;
        }
        PowerUpDropService.CollectResult result = drops.collect(drop.id(), drop.gameId(), playerId);
        if (result.collected()) {
          collected
              .computeIfAbsent(drop.gameId(), ignored -> new ConcurrentHashMap<>())
              .merge(playerId, 1L, Math::addExact);
          removeEntity(drop.id());
          activate(drop.gameId(), player, drop.type());
        }
        break;
      }
    }
  }

  public boolean activate(UUID gameId, Player collector, PowerUpType type) {
    PowerUpService.ActivationResult result =
        powerUps.activate(gameId, type, collector == null ? null : collector.getUniqueId(), "drop");
    if (!result.activated()) {
      return false;
    }
    switch (type) {
      case MAX_AMMO -> {
        if (weapons != null) {
          weapons.refillGame(gameId);
        }
      }
      case NUKE ->
          zombies.game(gameId).stream()
              .map(value -> value.entityId())
              .toList()
              .forEach(
                  entityId ->
                      zombies.damage(
                          entityId,
                          collector == null ? null : collector.getUniqueId(),
                          fr.heneria.zombie.core.enemy.ZombieDamageType.EXPLOSIVE,
                          1_000_000,
                          false));
      case DOUBLE_POINTS, INSTA_KILL -> {
        // Their timed behavior is read by RewardService and PaperWeaponService.
      }
    }
    for (UUID playerId : gamePlayers.apply(gameId)) {
      Player player = Bukkit.getPlayer(playerId);
      if (player != null) {
        player.sendActionBar(MINI.deserialize("<gold>Bonus activé : <yellow>" + displayName(type)));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1.2f);
      }
    }
    return true;
  }

  public boolean instaKill(UUID gameId) {
    return powerUps.active(gameId, PowerUpType.INSTA_KILL);
  }

  public void clear(UUID gameId) {
    drops.active(gameId).forEach(drop -> removeEntity(drop.id()));
    drops.clear(gameId);
    powerUps.clear(gameId);
    collected.remove(gameId);
  }

  public long collected(UUID gameId, UUID playerId) {
    return collected.getOrDefault(gameId, Map.of()).getOrDefault(playerId, 0L);
  }

  private Collection<PowerUpDrop> allActiveDrops() {
    return drops.active();
  }

  private void removeEntity(UUID dropId) {
    UUID entityId = entities.remove(dropId);
    anchors.remove(dropId);
    Entity entity = entityId == null ? null : Bukkit.getEntity(entityId);
    if (entity != null) {
      entity.remove();
    }
  }

  private static Location safeGround(Location death) {
    var world = death.getWorld();
    int originX = death.getBlockX();
    int originZ = death.getBlockZ();
    int startY =
        Math.min(world.getMaxHeight() - 2, Math.max(world.getMinHeight(), death.getBlockY()));
    for (int radius = 0; radius <= 2; radius++) {
      for (int x = originX - radius; x <= originX + radius; x++) {
        for (int z = originZ - radius; z <= originZ + radius; z++) {
          if (radius > 0
              && x > originX - radius
              && x < originX + radius
              && z > originZ - radius
              && z < originZ + radius) {
            continue;
          }
          for (int y = startY; y >= Math.max(world.getMinHeight(), startY - 24); y--) {
            Block ground = world.getBlockAt(x, y, z);
            if (ground.getType().isSolid()
                && ground.getRelative(0, 1, 0).isPassable()
                && ground.getRelative(0, 2, 0).isPassable()) {
              return new Location(world, x + 0.5, y + 1.05, z + 0.5);
            }
          }
        }
      }
    }
    Block highest = world.getHighestBlockAt(originX, originZ);
    return new Location(world, originX + 0.5, highest.getY() + 1.05, originZ + 0.5);
  }

  private static ItemStack item(PowerUpType type) {
    Material material =
        switch (type) {
          case DOUBLE_POINTS -> Material.GOLD_NUGGET;
          case MAX_AMMO -> Material.AMETHYST_SHARD;
          case NUKE -> Material.TNT;
          case INSTA_KILL -> Material.IRON_SWORD;
        };
    ItemStack stack = new ItemStack(material);
    var meta = stack.getItemMeta();
    meta.displayName(MINI.deserialize("<gold>" + displayName(type)));
    stack.setItemMeta(meta);
    return stack;
  }

  private static String displayName(PowerUpType type) {
    return switch (type) {
      case DOUBLE_POINTS -> "Points doubles";
      case MAX_AMMO -> "Munitions max";
      case NUKE -> "Nuke";
      case INSTA_KILL -> "Mort instantanée";
    };
  }
}
