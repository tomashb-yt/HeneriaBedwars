package fr.heneria.zombie.core.editor;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Durable publication history and current administrative status of one map. */
public record MapPublication(
    String mapId,
    MapStatus status,
    Optional<Integer> activeVersion,
    List<PublishedMapVersion> versions) {

  public MapPublication {
    if (!MapDefinition.safeId(mapId)) {
      throw new IllegalArgumentException("Invalid map id");
    }
    Objects.requireNonNull(status, "status");
    activeVersion = Objects.requireNonNull(activeVersion, "activeVersion");
    versions =
        versions.stream()
            .sorted(java.util.Comparator.comparingInt(PublishedMapVersion::version))
            .toList();
    int selectedVersion = activeVersion.orElse(0);
    if (activeVersion.isPresent()
        && versions.stream().noneMatch(value -> value.version() == selectedVersion)) {
      throw new IllegalArgumentException("Active published version is missing");
    }
  }

  /** Creates an unpublished lifecycle for a new editable map. */
  public static MapPublication draft(String mapId) {
    return new MapPublication(mapId, MapStatus.DRAFT, Optional.empty(), List.of());
  }

  /** Resolves the active immutable snapshot, if one exists. */
  public Optional<PublishedMapVersion> active() {
    return activeVersion.flatMap(
        selected -> versions.stream().filter(value -> value.version() == selected).findFirst());
  }
}
