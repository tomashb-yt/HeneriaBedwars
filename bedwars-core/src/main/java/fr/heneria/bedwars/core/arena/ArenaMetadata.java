package fr.heneria.bedwars.core.arena;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/** Administrative audit metadata. */
public record ArenaMetadata(Instant createdAt, Instant updatedAt, Map<String, String> attributes) {
  public ArenaMetadata {
    Objects.requireNonNull(createdAt, "createdAt");
    Objects.requireNonNull(updatedAt, "updatedAt");
    attributes = Map.copyOf(Objects.requireNonNull(attributes, "attributes"));
  }

  public static ArenaMetadata created(Instant now) {
    return new ArenaMetadata(now, now, Map.of());
  }

  public ArenaMetadata touched(Instant now) {
    return new ArenaMetadata(createdAt, now, attributes);
  }
}
