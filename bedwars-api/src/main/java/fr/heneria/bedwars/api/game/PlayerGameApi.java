package fr.heneria.bedwars.api.game;

import java.util.Optional;
import java.util.UUID;

/** Player-oriented read-only runtime lookup. */
public interface PlayerGameApi {
  Optional<GameSnapshot> game(UUID playerId);

  Optional<RuntimePlayerSnapshot> runtime(UUID playerId);
}
