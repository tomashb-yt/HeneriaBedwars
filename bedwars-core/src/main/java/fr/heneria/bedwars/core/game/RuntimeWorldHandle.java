package fr.heneria.bedwars.core.game;

import java.util.Objects;

/** Platform-neutral reference to a cloned, temporary world. */
public record RuntimeWorldHandle(GameId gameId, String worldName, String storageKey) {
  public RuntimeWorldHandle {
    gameId = Objects.requireNonNull(gameId, "gameId");
    worldName = text(worldName, "worldName");
    storageKey = text(storageKey, "storageKey");
  }

  private static String text(String value, String field) {
    String clean = Objects.requireNonNull(value, field).trim();
    if (clean.isEmpty()) throw new IllegalArgumentException(field + " is blank");
    return clean;
  }
}
