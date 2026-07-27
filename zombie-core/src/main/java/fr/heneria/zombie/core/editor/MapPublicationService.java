package fr.heneria.zombie.core.editor;

import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/** Publishes validated immutable map revisions and atomically exposes only persisted candidates. */
public final class MapPublicationService {
  private final MapRegistry maps;
  private final MapValidator validator;
  private final MapPublicationPersistence persistence;
  private final Clock clock;
  private final ConcurrentHashMap<String, MapPublication> publications = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, CompletableFuture<Void>> operations =
      new ConcurrentHashMap<>();
  private final java.util.Set<String> deleting = ConcurrentHashMap.newKeySet();

  public MapPublicationService(
      MapRegistry maps,
      MapValidator validator,
      MapPublicationPersistence persistence,
      Clock clock) {
    this.maps = java.util.Objects.requireNonNull(maps, "maps");
    this.validator = java.util.Objects.requireNonNull(validator, "validator");
    this.persistence = java.util.Objects.requireNonNull(persistence, "persistence");
    this.clock = java.util.Objects.requireNonNull(clock, "clock");
  }

  /** Loads publication histories before they become visible. */
  public CompletableFuture<Integer> initialize() {
    return persistence
        .loadAll()
        .thenApply(
            loaded -> {
              publications.clear();
              loaded.forEach(value -> publications.put(value.mapId(), value));
              return loaded.size();
            });
  }

  public MapPublication publication(String mapId) {
    return publications.getOrDefault(mapId, MapPublication.draft(mapId));
  }

  public Collection<MapPublication> all() {
    return maps.all().stream().map(value -> publication(value.id())).toList();
  }

  public Collection<PublishedMapVersion> published() {
    return publications.values().stream()
        .filter(value -> value.status().playerVisible())
        .filter(value -> maps.find(value.mapId()).isPresent())
        .map(MapPublication::active)
        .flatMap(Optional::stream)
        .sorted(java.util.Comparator.comparing(value -> value.definition().displayName()))
        .toList();
  }

  public Optional<MapDefinition> publishedDefinition(String mapId) {
    MapPublication publication = publications.get(mapId);
    if (publication == null || !publication.status().playerVisible()) {
      return Optional.empty();
    }
    return publication.active().map(PublishedMapVersion::definition);
  }

  /** Serializes a permanent deletion after pending publication writes and rejects new revisions. */
  public CompletableFuture<Void> delete(
      String mapId, java.util.function.Supplier<CompletableFuture<?>> deletion) {
    if (!deleting.add(mapId)) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("Map deletion is already in progress"));
    }
    CompletableFuture<Void> result = new CompletableFuture<>();
    operations.compute(
        mapId,
        (ignored, previous) -> {
          CompletableFuture<Void> prerequisite =
              previous == null
                  ? CompletableFuture.completedFuture(null)
                  : previous.handle((value, failure) -> null);
          CompletableFuture<Void> operation =
              prerequisite
                  .thenCompose(value -> deletion.get())
                  .thenRun(
                      () -> {
                        publications.remove(mapId);
                        result.complete(null);
                      })
                  .whenComplete(
                      (value, failure) -> {
                        deleting.remove(mapId);
                        if (failure != null) {
                          result.completeExceptionally(failure);
                        }
                      });
          operation.whenComplete((value, failure) -> operations.remove(mapId, operation));
          return operation;
        });
    return result;
  }

  /** Validates, persists and then atomically exposes a new immutable revision. */
  public CompletableFuture<MapPublication> publish(String mapId, UUID actor) {
    if (deleting.contains(mapId)) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("Map deletion is in progress"));
    }
    MapDefinition definition =
        maps.find(mapId).orElseThrow(() -> new IllegalArgumentException("Unknown map " + mapId));
    ValidationReport report = validator.validate(definition);
    if (!report.valid()) {
      return CompletableFuture.failedFuture(
          new IllegalStateException(
              "Map validation failed with " + report.errors().size() + " error(s)"));
    }
    return serialized(
        mapId,
        () -> {
          MapPublication current = publication(mapId);
          int next =
              current.versions().stream().mapToInt(PublishedMapVersion::version).max().orElse(0)
                  + 1;
          PublishedMapVersion version =
              new PublishedMapVersion(next, definition, clock.instant(), actor, Optional.empty());
          java.util.ArrayList<PublishedMapVersion> history =
              new java.util.ArrayList<>(current.versions());
          history.add(version);
          return new MapPublication(
              mapId, MapStatus.PUBLISHED, Optional.of(next), List.copyOf(history));
        });
  }

  /** Hides a map from players while preserving every revision. */
  public CompletableFuture<MapPublication> unpublish(String mapId) {
    return changeStatus(mapId, MapStatus.READY);
  }

  public CompletableFuture<MapPublication> changeStatus(String mapId, MapStatus status) {
    if (deleting.contains(mapId)) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("Map deletion is in progress"));
    }
    if (status == MapStatus.PUBLISHED) {
      return CompletableFuture.failedFuture(
          new IllegalArgumentException("Use publish to expose a validated revision"));
    }
    return serialized(
        mapId,
        () -> {
          MapPublication current = publication(mapId);
          return new MapPublication(mapId, status, current.activeVersion(), current.versions());
        });
  }

  /** Re-publishes a historical snapshot as a new auditable version. */
  public CompletableFuture<MapPublication> rollback(String mapId, int version, UUID actor) {
    if (deleting.contains(mapId)) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("Map deletion is in progress"));
    }
    MapPublication current = publication(mapId);
    MapDefinition selected =
        current.versions().stream()
            .filter(value -> value.version() == version)
            .map(PublishedMapVersion::definition)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown published version"));
    return serialized(
        mapId,
        () -> {
          MapPublication latest = publication(mapId);
          int next =
              latest.versions().stream().mapToInt(PublishedMapVersion::version).max().orElse(0) + 1;
          java.util.ArrayList<PublishedMapVersion> history =
              new java.util.ArrayList<>(latest.versions());
          history.add(
              new PublishedMapVersion(
                  next, selected, clock.instant(), actor, Optional.of(version)));
          return new MapPublication(
              mapId, MapStatus.PUBLISHED, Optional.of(next), List.copyOf(history));
        });
  }

  private CompletableFuture<MapPublication> serialized(
      String mapId, java.util.function.Supplier<MapPublication> candidateFactory) {
    CompletableFuture<MapPublication> result = new CompletableFuture<>();
    operations.compute(
        mapId,
        (ignored, previous) -> {
          if (deleting.contains(mapId)) {
            result.completeExceptionally(new IllegalStateException("Map deletion is in progress"));
            return previous;
          }
          CompletableFuture<Void> prerequisite =
              previous == null
                  ? CompletableFuture.completedFuture(null)
                  : previous.handle((value, failure) -> null);
          CompletableFuture<Void> operation =
              prerequisite
                  .thenCompose(
                      value -> {
                        MapPublication candidate = candidateFactory.get();
                        return persistence
                            .save(candidate)
                            .thenRun(
                                () -> {
                                  publications.put(mapId, candidate);
                                  result.complete(candidate);
                                });
                      })
                  .whenComplete(
                      (value, failure) -> {
                        if (failure != null) {
                          result.completeExceptionally(failure);
                        }
                      });
          operation.whenComplete((value, failure) -> operations.remove(mapId, operation));
          return operation;
        });
    return result;
  }
}
