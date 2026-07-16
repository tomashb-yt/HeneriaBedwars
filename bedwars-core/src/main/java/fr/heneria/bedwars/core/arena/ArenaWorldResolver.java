package fr.heneria.bedwars.core.arena;

/** Port implemented by Bukkit to keep world lookup outside the core. */
@FunctionalInterface
public interface ArenaWorldResolver {
  boolean exists(String worldName);
}
