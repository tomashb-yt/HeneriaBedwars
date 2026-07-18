package fr.heneria.bedwars.plugin.game;

import fr.heneria.bedwars.api.game.GameState;
import fr.heneria.bedwars.core.game.GameInstance;
import fr.heneria.bedwars.core.game.GameInstanceManager;
import fr.heneria.bedwars.core.game.generator.GeneratorCapacityView;
import fr.heneria.bedwars.core.game.generator.GeneratorDefinition;
import fr.heneria.bedwars.core.game.generator.GeneratorEmission;
import fr.heneria.bedwars.core.game.generator.GeneratorStackingStrategy;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

/** Main-thread Bukkit adapter that counts, merges and creates resource item entities. */
public final class BukkitGameGeneratorAdapter implements GeneratorCapacityView {
  private final GameInstanceManager games;
  private final BukkitGeneratorCatalog catalog;

  public BukkitGameGeneratorAdapter(GameInstanceManager games, BukkitGeneratorCatalog catalog) {
    this.games = Objects.requireNonNull(games, "games");
    this.catalog = Objects.requireNonNull(catalog, "catalog");
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
        if (!(entity instanceof Item item) || item.getItemStack().getType() != material) continue;
        ItemStack stack = item.getItemStack();
        int accepted = Math.min(remaining, stack.getMaxStackSize() - stack.getAmount());
        if (accepted < 1) continue;
        stack.setAmount(stack.getAmount() + accepted);
        item.setItemStack(stack);
        remaining -= accepted;
        if (remaining == 0) return;
      }
    }
    while (remaining > 0) {
      int amount = Math.min(remaining, material.getMaxStackSize());
      world.dropItem(location, new ItemStack(material, amount));
      remaining -= amount;
    }
  }

  private static World world(GameInstance game) {
    if (game == null || game.state() != GameState.PLAYING) return null;
    return game.world().map(handle -> Bukkit.getWorld(handle.worldName())).orElse(null);
  }

  private static Location location(World world, GeneratorDefinition generator) {
    var value = generator.location();
    return new Location(world, value.x(), value.y(), value.z(), value.yaw(), value.pitch());
  }

  private static void requireMainThread() {
    if (!Bukkit.isPrimaryThread())
      throw new IllegalStateException("Generator world access must run on the server thread");
  }
}
