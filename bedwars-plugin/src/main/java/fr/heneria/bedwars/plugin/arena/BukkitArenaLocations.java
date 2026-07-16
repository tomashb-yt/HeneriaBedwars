package fr.heneria.bedwars.plugin.arena;

import fr.heneria.bedwars.core.arena.ArenaLocation;
import fr.heneria.bedwars.core.arena.ArenaVector;
import org.bukkit.Location;

public final class BukkitArenaLocations {
  private BukkitArenaLocations() {}

  public static ArenaLocation from(Location location) {
    if (location.getWorld() == null) throw new IllegalArgumentException("Location has no world");
    return new ArenaLocation(
        location.getWorld().getName(),
        new ArenaVector(location.getX(), location.getY(), location.getZ()),
        location.getYaw(),
        location.getPitch());
  }
}
