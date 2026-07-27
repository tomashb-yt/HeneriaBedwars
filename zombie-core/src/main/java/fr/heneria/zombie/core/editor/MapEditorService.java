package fr.heneria.zombie.core.editor;

import java.time.Clock;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

/** Application service for creation, sessions, mutations, autosave and undo/redo. */
public final class MapEditorService {
  private final MapRegistry registry;
  private final MapPersistence persistence;
  private final Clock clock;
  private final ConcurrentHashMap<UUID, MapEditorSession> sessions = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, UUID> mapEditors = new ConcurrentHashMap<>();

  public MapEditorService(MapRegistry registry, MapPersistence persistence, Clock clock) {
    this.registry = Objects.requireNonNull(registry, "registry");
    this.persistence = Objects.requireNonNull(persistence, "persistence");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public CompletableFuture<Integer> initialize() {
    return persistence
        .loadAll()
        .thenApply(
            definitions -> {
              definitions.forEach(registry::register);
              return definitions.size();
            });
  }

  public CompletableFuture<MapDefinition> create(
      String id, String displayName, UUID creator, String world) {
    MapDefinition definition =
        MapDefinition.create(id, displayName, creator, clock.instant(), world);
    if (!registry.register(definition)) {
      return CompletableFuture.failedFuture(new IllegalArgumentException("Map already exists"));
    }
    return persistence.save(definition).thenApply(ignored -> definition);
  }

  /** Registers and persists a complete copy after its physical world has been duplicated. */
  public CompletableFuture<MapDefinition> duplicate(
      String sourceId, String newId, UUID creator, String newWorld) {
    MapDefinition source =
        registry
            .find(sourceId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown source map"));
    MapDefinition definition = source.duplicatedAs(newId, creator, clock.instant(), newWorld);
    if (!registry.register(definition)) {
      return CompletableFuture.failedFuture(new IllegalArgumentException("Map already exists"));
    }
    return persistence.save(definition).thenApply(ignored -> definition);
  }

  /**
   * Permanently deletes an unlocked map after its persistent artifacts have been removed.
   *
   * <p>The registry remains unchanged when persistence fails.
   */
  public CompletableFuture<Boolean> delete(String mapId) {
    MapDefinition definition =
        registry.find(mapId).orElseThrow(() -> new IllegalArgumentException("Unknown map"));
    if (mapEditors.containsKey(mapId)) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("Map is currently being edited"));
    }
    return persistence
        .delete(definition)
        .thenApply(
            ignored -> {
              if (!registry.remove(definition)) {
                throw new IllegalStateException("Map changed during deletion");
              }
              return true;
            });
  }

  public Optional<MapEditorSession> open(UUID playerId, String mapId) {
    MapDefinition definition = registry.find(mapId).orElse(null);
    if (definition == null || sessions.containsKey(playerId)) {
      return Optional.empty();
    }
    UUID currentEditor = mapEditors.putIfAbsent(mapId, playerId);
    if (currentEditor != null && !currentEditor.equals(playerId)) {
      return Optional.empty();
    }
    MapEditorSession session = new MapEditorSession(playerId, definition, clock.instant());
    if (sessions.putIfAbsent(playerId, session) == null) {
      return Optional.of(session);
    }
    mapEditors.remove(mapId, playerId);
    return Optional.empty();
  }

  public Optional<MapEditorSession> session(UUID playerId) {
    return Optional.ofNullable(sessions.get(playerId));
  }

  public CompletableFuture<Boolean> leave(UUID playerId) {
    MapEditorSession removed = sessions.remove(playerId);
    if (removed == null) {
      return CompletableFuture.completedFuture(false);
    }
    mapEditors.remove(removed.definition().id(), playerId);
    return saveWorkingCopy(removed).thenApply(ignored -> true);
  }

  public CompletableFuture<MapDefinition> mutate(
      UUID playerId, UnaryOperator<MapDefinition> mutation) {
    MapEditorSession session =
        session(playerId).orElseThrow(() -> new IllegalStateException("No editor session"));
    MapDefinition changed = mutation.apply(session.definition());
    session.change(changed, clock.instant());
    registry.update(changed);
    return persistence.save(changed).thenApply(ignored -> changed);
  }

  public CompletableFuture<Boolean> undo(UUID playerId) {
    MapEditorSession session =
        session(playerId).orElseThrow(() -> new IllegalStateException("No editor session"));
    if (!session.undo(clock.instant())) {
      return CompletableFuture.completedFuture(false);
    }
    registry.update(session.definition());
    return saveWorkingCopy(session).thenApply(ignored -> true);
  }

  public CompletableFuture<Boolean> redo(UUID playerId) {
    MapEditorSession session =
        session(playerId).orElseThrow(() -> new IllegalStateException("No editor session"));
    if (!session.redo(clock.instant())) {
      return CompletableFuture.completedFuture(false);
    }
    registry.update(session.definition());
    return saveWorkingCopy(session).thenApply(ignored -> true);
  }

  public CompletableFuture<Void> save(UUID playerId) {
    return saveWorkingCopy(
        session(playerId).orElseThrow(() -> new IllegalStateException("No editor session")));
  }

  public MapRegistry registry() {
    return registry;
  }

  public Map<UUID, MapEditorSession> sessions() {
    return Map.copyOf(sessions);
  }

  /** Returns the administrator currently holding a map edit lock. */
  public Optional<UUID> editorOf(String mapId) {
    return Optional.ofNullable(mapEditors.get(mapId));
  }

  private CompletableFuture<Void> saveWorkingCopy(MapEditorSession session) {
    return persistence.save(session.definition());
  }
}
