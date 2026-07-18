package fr.heneria.bedwars.plugin.game;

import fr.heneria.bedwars.core.game.GameInstance;
import java.time.Instant;

/** Copies persistent arena generator snapshots into a newly loaded game instance. */
public final class BukkitGameGeneratorRegistry {
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
}
