package fr.heneria.bedwars.core.item;

import java.util.Objects;

/** Offline-safe player-head source. Texture-network resolution is deliberately excluded. */
public record HeadDefinition(Type type, String value) {
  public HeadDefinition {
    Objects.requireNonNull(type, "type");
    value = Objects.requireNonNullElse(value, "");
  }

  public enum Type {
    PLAYER,
    CONTEXT_PLAYER
  }
}
