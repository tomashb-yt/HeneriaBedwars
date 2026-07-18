package fr.heneria.bedwars.plugin.game.shop;

import fr.heneria.bedwars.core.config.PlaceholderContext;
import fr.heneria.bedwars.core.game.GameInstance;
import fr.heneria.bedwars.core.logging.ProjectLogger;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import fr.heneria.bedwars.plugin.game.upgrade.BukkitUpgradeCatalog;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** Spawns invulnerable shop villagers inside the disposable runtime world. */
public final class BukkitShopNpcService {
  private final ConfigurationService configurations;
  private final BukkitShopCatalog catalogs;
  private final ProjectLogger logger;
  private final BukkitUpgradeCatalog upgrades;
  private final NamespacedKey gameKey;
  private final NamespacedKey teamKey;
  private final NamespacedKey typeKey;

  public BukkitShopNpcService(
      JavaPlugin plugin,
      ConfigurationService configurations,
      BukkitShopCatalog catalogs,
      BukkitUpgradeCatalog upgrades,
      ProjectLogger logger) {
    this.configurations = Objects.requireNonNull(configurations, "configurations");
    this.catalogs = Objects.requireNonNull(catalogs, "catalogs");
    this.upgrades = Objects.requireNonNull(upgrades, "upgrades");
    this.logger = Objects.requireNonNull(logger, "logger");
    gameKey = new NamespacedKey(plugin, "shop_game_id");
    teamKey = new NamespacedKey(plugin, "shop_team_id");
    typeKey = new NamespacedKey(plugin, "shop_type");
  }

  public int initialize(GameInstance game) {
    if (!Bukkit.isPrimaryThread())
      throw new IllegalStateException("Shop NPC creation must run on the server thread");
    if (!catalogs.enabled() && !upgrades.enabled()) return 0;
    var world = game.world().map(handle -> Bukkit.getWorld(handle.worldName())).orElse(null);
    if (world == null) return 0;
    world.getEntities().stream()
        .filter(
            entity ->
                token(entity)
                    .map(value -> value.gameId().equals(game.id().toString()))
                    .orElse(false))
        .forEach(Entity::remove);
    int spawned = 0;
    for (var team : game.arena().definition().teams()) {
      if (catalogs.enabled() && team.shopLocation().isPresent()) {
        if (spawn(world, game, team, team.shopLocation().orElseThrow(), NpcType.ITEM)) spawned++;
      }
      if (upgrades.enabled() && team.upgradeShopLocation().isPresent()) {
        if (spawn(world, game, team, team.upgradeShopLocation().orElseThrow(), NpcType.UPGRADE))
          spawned++;
      }
    }
    return spawned;
  }

  private boolean spawn(
      org.bukkit.World world,
      GameInstance game,
      fr.heneria.bedwars.core.arena.ArenaTeamDefinition team,
      fr.heneria.bedwars.core.arena.ArenaLocation shop,
      NpcType type) {
    Location location =
        new Location(
            world,
            shop.position().x(),
            shop.position().y(),
            shop.position().z(),
            shop.yaw(),
            shop.pitch());
    try {
      location.getChunk().load();
      world.spawn(
          location,
          Villager.class,
          villager -> {
            villager.setAI(false);
            villager.setInvulnerable(true);
            villager.setSilent(true);
            villager.setCollidable(false);
            villager.setRemoveWhenFarAway(false);
            villager.setPersistent(false);
            villager.setAdult();
            villager.setProfession(
                type == NpcType.ITEM ? Villager.Profession.ARMORER : Villager.Profession.TOOLSMITH);
            villager.setCustomName(
                configurations
                    .language()
                    .message(
                        type == NpcType.ITEM ? "shop.npc.name" : "upgrade.npc.name",
                        configurations.snapshot().plugin().locale(),
                        PlaceholderContext.builder().put("team", team.displayName()).build()));
            villager.setCustomNameVisible(true);
            villager
                .getPersistentDataContainer()
                .set(gameKey, PersistentDataType.STRING, game.id().toString());
            villager
                .getPersistentDataContainer()
                .set(teamKey, PersistentDataType.STRING, team.id().value());
            villager
                .getPersistentDataContainer()
                .set(typeKey, PersistentDataType.STRING, type.name());
          });
      return true;
    } catch (RuntimeException exception) {
      logger.warning(
          "[Shop] Unable to spawn "
              + type.name().toLowerCase(java.util.Locale.ROOT)
              + " NPC for team '"
              + team.id().value()
              + "' in game "
              + game.id().shortId()
              + ": "
              + exception.getMessage());
      return false;
    }
  }

  public Optional<Token> token(Entity entity) {
    String game = entity.getPersistentDataContainer().get(gameKey, PersistentDataType.STRING);
    String team = entity.getPersistentDataContainer().get(teamKey, PersistentDataType.STRING);
    String rawType = entity.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
    if (game == null || team == null) return Optional.empty();
    try {
      return Optional.of(
          new Token(game, team, rawType == null ? NpcType.ITEM : NpcType.valueOf(rawType)));
    } catch (IllegalArgumentException ignored) {
      return Optional.empty();
    }
  }

  public enum NpcType {
    ITEM,
    UPGRADE
  }

  public record Token(String gameId, String teamId, NpcType type) {}
}
