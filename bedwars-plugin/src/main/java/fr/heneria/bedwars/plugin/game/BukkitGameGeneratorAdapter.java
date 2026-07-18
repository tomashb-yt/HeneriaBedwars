package fr.heneria.bedwars.plugin.game;

import fr.heneria.bedwars.api.game.GameState;
import fr.heneria.bedwars.core.game.GameId;
import fr.heneria.bedwars.core.game.GameInstance;
import fr.heneria.bedwars.core.game.GameInstanceManager;
import fr.heneria.bedwars.core.game.generator.GeneratorCapacityView;
import fr.heneria.bedwars.core.game.generator.GeneratorDefinition;
import fr.heneria.bedwars.core.game.generator.GeneratorEmission;
import fr.heneria.bedwars.core.game.generator.GeneratorId;
import fr.heneria.bedwars.core.game.generator.GeneratorStackingStrategy;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

/** Main-thread Bukkit adapter that counts, merges and creates resource item entities. */
public final class BukkitGameGeneratorAdapter implements GeneratorCapacityView {
  private final GameInstanceManager games;
  private final BukkitGeneratorCatalog catalog;
  private final NamespacedKey gameKey;
  private final NamespacedKey generatorKey;
  private final Map<UUID, AnchoredDrop> anchored = new LinkedHashMap<>();

  public BukkitGameGeneratorAdapter(
      JavaPlugin plugin, GameInstanceManager games, BukkitGeneratorCatalog catalog) {
    this.games = Objects.requireNonNull(games, "games");
    this.catalog = Objects.requireNonNull(catalog, "catalog");
    gameKey = new NamespacedKey(plugin, "generator_game_id");
    generatorKey = new NamespacedKey(plugin, "generator_id");
  }

  @Override
  public int nearbyAmount(
      fr.heneria.bedwars.core.game.GameId gameId, GeneratorDefinition generator) {
    requireMainThread();
    GameInstance game = games.find(gameId).orElse(null);
    World world = world(game);
    if (world == null) return generator.localCapacity();
    Material material = catalog.material(generator.resource());
    double radius = catalog.mergeRadius();
    return world.getNearbyEntities(location(world, generator), radius, radius, radius).stream()
        .filter(Item.class::isInstance)
        .map(Item.class::cast)
        .filter(item -> belongsTo(item, gameId, generator.id()))
        .map(Item::getItemStack)
        .filter(stack -> stack.getType() == material)
        .mapToInt(ItemStack::getAmount)
        .sum();
  }

  public void emit(GeneratorEmission emission) {
    requireMainThread();
    GameInstance game = games.find(emission.gameId()).orElse(null);
    World world = world(game);
    if (world == null) return;
    Material material = catalog.material(emission.generator().resource());
    Location location = location(world, emission.generator());
    int remaining = emission.amount();
    if (emission.generator().stackingStrategy() == GeneratorStackingStrategy.MERGE_NEARBY) {
      double radius = catalog.mergeRadius();
      for (var entity : world.getNearbyEntities(location, radius, radius, radius)) {
        if (!(entity instanceof Item item)
            || item.getItemStack().getType() != material
            || !belongsTo(item, emission.gameId(), emission.generator().id())) continue;
        ItemStack stack = item.getItemStack();
        int accepted = Math.min(remaining, stack.getMaxStackSize() - stack.getAmount());
        if (accepted < 1) continue;
        stack.setAmount(stack.getAmount() + accepted);
        item.setItemStack(stack);
        anchor(item, location, emission.gameId(), emission.generator().id());
        remaining -= accepted;
        if (remaining == 0) return;
      }
    }
    while (remaining > 0) {
      int amount = Math.min(remaining, material.getMaxStackSize());
      Item item = world.dropItem(location, new ItemStack(material, amount));
      anchor(item, location, emission.gameId(), emission.generator().id());
      remaining -= amount;
    }
  }

  /** Keeps owned resource entities centered without creating one task per generator. */
  public void stabilize() {
    requireMainThread();
    Iterator<AnchoredDrop> iterator = anchored.values().iterator();
    while (iterator.hasNext()) {
      AnchoredDrop drop = iterator.next();
      Item item = drop.item();
      if (!item.isValid() || item.isDead()) {
        iterator.remove();
        continue;
      }
      Location current = item.getLocation();
      if (current.getWorld() != drop.anchor().getWorld()
          || current.distanceSquared(drop.anchor()) > 0.0025) item.teleport(drop.anchor());
      if (item.getVelocity().lengthSquared() > 0.0001) item.setVelocity(new Vector());
      item.setGravity(false);
      item.setTicksLived(Math.min(item.getTicksLived(), 100));
    }
  }

  public void clear() {
    anchored.clear();
  }

  private void anchor(Item item, Location location, GameId gameId, GeneratorId generatorId) {
    item.setVelocity(new Vector());
    item.setGravity(false);
    item.setPickupDelay(0);
    item.getPersistentDataContainer().set(gameKey, PersistentDataType.STRING, gameId.toString());
    item.getPersistentDataContainer()
        .set(generatorKey, PersistentDataType.STRING, generatorId.value());
    Location anchor = location.clone();
    if (item.getLocation().distanceSquared(anchor) > 0.0025) item.teleport(anchor);
    anchored.put(item.getUniqueId(), new AnchoredDrop(item, anchor));
  }

  private boolean belongsTo(Item item, GameId gameId, GeneratorId generatorId) {
    var data = item.getPersistentDataContainer();
    return gameId.toString().equals(data.get(gameKey, PersistentDataType.STRING))
        && generatorId.value().equals(data.get(generatorKey, PersistentDataType.STRING));
  }

  private static World world(GameInstance game) {
    if (game == null || game.state() != GameState.PLAYING) return null;
    return game.world().map(handle -> Bukkit.getWorld(handle.worldName())).orElse(null);
  }

  private static Location location(World world, GeneratorDefinition generator) {
    var value = generator.location();
    return new Location(
        world,
        Math.floor(value.x()) + 0.5,
        value.y() + 0.15,
        Math.floor(value.z()) + 0.5,
        value.yaw(),
        value.pitch());
  }

  private static void requireMainThread() {
    if (!Bukkit.isPrimaryThread())
      throw new IllegalStateException("Generator world access must run on the server thread");
  }

  private record AnchoredDrop(Item item, Location anchor) {}
}
