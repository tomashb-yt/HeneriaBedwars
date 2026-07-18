package fr.heneria.bedwars.plugin.game;

import fr.heneria.bedwars.core.game.GameInstance;
import java.time.Instant;
import java.util.Objects;

/** Copies persistent arena generator snapshots into a newly loaded game instance. */
public final class BukkitGameGeneratorRegistry {
  private final BukkitGeneratorCatalog catalog;

  public BukkitGameGeneratorRegistry(BukkitGeneratorCatalog catalog) {
    this.catalog = Objects.requireNonNull(catalog, "catalog");
  }

  public void initialize(GameInstance game) {
    if (!org.bukkit.Bukkit.isPrimaryThread())
      throw new IllegalStateException(
          "Runtime generators must be initialized on the server thread");
    if (game.world().isEmpty()) return;
    for (var generator : game.arena().definition().generators()) {
      if (game.generator(generator.id()).isEmpty())
        game.registerGenerator(generator.runtime(), Instant.now());
    }
  }

  public void activate(GameInstance game) {
    if (!org.bukkit.Bukkit.isPrimaryThread())
      throw new IllegalStateException("Runtime generators must be paced on the server thread");
    game.paceGenerators(catalog.pacingPolicy(), Instant.now());
  }
}
