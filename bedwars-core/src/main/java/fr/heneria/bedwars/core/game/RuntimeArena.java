package fr.heneria.bedwars.core.game;

import fr.heneria.bedwars.core.arena.ArenaDefinition;
import fr.heneria.bedwars.core.map.MapTemplate;
import java.util.Objects;

/** Immutable bridge between administrative definitions and one live runtime. */
public record RuntimeArena(GameId gameId, ArenaDefinition definition, MapTemplate template) {
  public RuntimeArena {
    gameId = Objects.requireNonNull(gameId, "gameId");
    definition = Objects.requireNonNull(definition, "definition");
    template = Objects.requireNonNull(template, "template");
  }
}
