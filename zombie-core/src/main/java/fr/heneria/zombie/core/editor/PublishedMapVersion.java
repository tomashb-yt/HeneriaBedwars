package fr.heneria.zombie.core.editor;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Immutable map snapshot published under a monotonically increasing revision. */
public record PublishedMapVersion(
    int version,
    MapDefinition definition,
    Instant publishedAt,
    UUID publishedBy,
    Optional<Integer> restoredFrom) {

  public PublishedMapVersion {
    if (version < 1) {
      throw new IllegalArgumentException("Published version must be positive");
    }
    Objects.requireNonNull(definition, "definition");
    Objects.requireNonNull(publishedAt, "publishedAt");
    Objects.requireNonNull(publishedBy, "publishedBy");
    restoredFrom = Objects.requireNonNull(restoredFrom, "restoredFrom");
    if (restoredFrom.filter(value -> value < 1 || value >= version).isPresent()) {
      throw new IllegalArgumentException("Invalid restored source version");
    }
  }
}
