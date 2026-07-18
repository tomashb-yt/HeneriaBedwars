package fr.heneria.bedwars.plugin.game.shop;

import fr.heneria.bedwars.core.config.PlaceholderContext;
import fr.heneria.bedwars.core.game.GameInstance;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
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
  private final NamespacedKey gameKey;
  private final NamespacedKey teamKey;

  public BukkitShopNpcService(
      JavaPlugin plugin, ConfigurationService configurations, BukkitShopCatalog catalogs) {
    this.configurations = Objects.requireNonNull(configurations, "configurations");
    this.catalogs = Objects.requireNonNull(catalogs, "catalogs");
    gameKey = new NamespacedKey(plugin, "shop_game_id");
    teamKey = new NamespacedKey(plugin, "shop_team_id");
  }

  public void initialize(GameInstance game) {
    if (!Bukkit.isPrimaryThread())
      throw new IllegalStateException("Shop NPC creation must run on the server thread");
    if (catalogs.current().size() == 0) return;
    var world = game.world().map(handle -> Bukkit.getWorld(handle.worldName())).orElse(null);
    if (world == null) return;
    world.getEntities().stream()
        .filter(
            entity ->
                token(entity)
                    .map(value -> value.gameId().equals(game.id().toString()))
                    .orElse(false))
        .forEach(Entity::remove);
    for (var team : game.arena().definition().teams()) {
      var shop = team.shopLocation().orElse(null);
      if (shop == null) continue;
      Location location =
          new Location(
              world,
              shop.position().x(),
              shop.position().y(),
              shop.position().z(),
              shop.yaw(),
              shop.pitch());
      world.spawn(
          location,
          Villager.class,
          villager -> {
            villager.setAI(false);
            villager.setInvulnerable(true);
            villager.setSilent(true);
            villager.setCollidable(false);
            villager.setRemoveWhenFarAway(false);
            villager.setAdult();
            villager.setProfession(Villager.Profession.ARMORER);
            villager.setCustomName(
                configurations
                    .language()
                    .message(
                        "shop.npc.name",
                        configurations.snapshot().plugin().locale(),
                        PlaceholderContext.builder().put("team", team.displayName()).build()));
            villager.setCustomNameVisible(true);
            villager
                .getPersistentDataContainer()
                .set(gameKey, PersistentDataType.STRING, game.id().toString());
            villager
                .getPersistentDataContainer()
                .set(teamKey, PersistentDataType.STRING, team.id().value());
          });
    }
  }

  public Optional<Token> token(Entity entity) {
    String game = entity.getPersistentDataContainer().get(gameKey, PersistentDataType.STRING);
    String team = entity.getPersistentDataContainer().get(teamKey, PersistentDataType.STRING);
    return game == null || team == null ? Optional.empty() : Optional.of(new Token(game, team));
  }

  public record Token(String gameId, String teamId) {}
}
