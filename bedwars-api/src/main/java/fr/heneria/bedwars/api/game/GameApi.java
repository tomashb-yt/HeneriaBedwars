package fr.heneria.bedwars.api.game;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Read-only game lookup API for future addons. Mutations remain controlled by the plugin. */
public interface GameApi {
  List<GameSnapshot> all();

  Optional<GameSnapshot> find(UUID gameId);

  Optional<GameSnapshot> byPlayer(UUID playerId);

  Optional<GameSnapshot> byArena(String arenaId);
}
