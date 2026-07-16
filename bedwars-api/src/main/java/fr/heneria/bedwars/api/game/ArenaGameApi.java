package fr.heneria.bedwars.api.game;

import java.util.Optional;

/** Arena-oriented read-only runtime lookup. */
public interface ArenaGameApi {
  Optional<GameSnapshot> game(String arenaId);

  boolean occupied(String arenaId);
}
