package fr.heneria.bedwars.core.game;

import java.util.Objects;
import java.util.UUID;

/** Strong runtime identifier; never reused and never derived from an arena id. */
public record GameId(UUID value) {
  public GameId {
    value = Objects.requireNonNull(value, "value");
  }

  public static GameId random() {
    return new GameId(UUID.randomUUID());
  }

  public static GameId parse(String value) {
    return new GameId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
