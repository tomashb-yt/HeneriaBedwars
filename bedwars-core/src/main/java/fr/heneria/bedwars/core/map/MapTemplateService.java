package fr.heneria.bedwars.core.map;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Transactional map-template use cases shared by commands and menus.
 *
 * <p>World methods must be invoked on the platform main thread. {@link #duplicate} performs heavy
 * file I/O and must be invoked asynchronously after the caller saved a loaded source. Registry
 * publication follows successful metadata/file operations; every per-map lock is released in a
 * finally block.
 */
public final class MapTemplateService {
  private final MapTemplateRepository repository;
  private final MapTemplateRegistry registry;
  private final MapWorldService worlds;
  private final MapFileService files;
  private final MapOperationLock locks;
  private final MapTemplateValidator validator;
  private final Clock clock;
  private final String worldPrefix;
  private final String environment;
  private final MapWorldSettings defaultSettings;
  private final int platformY;
  private final boolean saveBeforeUnload;
  private final String pluginVersion;
  private final java.util.function.Function<MapId, Set<String>> arenaLinks;
  private final java.util.function.Predicate<MapTemplate> protectedTemplate;

  public MapTemplateService(
      MapTemplateRepository repository,
      MapTemplateRegistry registry,
      MapWorldService worlds,
      MapFileService files,
      MapOperationLock locks,
      MapTemplateValidator validator,
      Clock clock,
      String worldPrefix,
      String environment,
      MapWorldSettings defaultSettings,
      int platformY,
      boolean saveBeforeUnload,
      String pluginVersion) {
    this(
        repository,
        registry,
        worlds,
        files,
        locks,
        validator,
        clock,
        worldPrefix,
        environment,
        defaultSettings,
        platformY,
        saveBeforeUnload,
        pluginVersion,
        id -> registry.find(id).map(MapTemplate::linkedArenaIds).orElse(Set.of()),
        template -> false);
  }

  public MapTemplateService(
      MapTemplateRepository repository,
      MapTemplateRegistry registry,
      MapWorldService worlds,
      MapFileService files,
      MapOperationLock locks,
      MapTemplateValidator validator,
      Clock clock,
      String worldPrefix,
      String environment,
      MapWorldSettings defaultSettings,
      int platformY,
      boolean saveBeforeUnload,
      String pluginVersion,
      java.util.function.Function<MapId, Set<String>> arenaLinks,
      java.util.function.Predicate<MapTemplate> protectedTemplate) {
    this.repository = Objects.requireNonNull(repository, "repository");
    this.registry = Objects.requireNonNull(registry, "registry");
    this.worlds = Objects.requireNonNull(worlds, "worlds");
    this.files = Objects.requireNonNull(files, "files");
    this.locks = Objects.requireNonNull(locks, "locks");
    this.validator = Objects.requireNonNull(validator, "validator");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.worldPrefix = requirePrefix(worldPrefix);
    this.environment = Objects.requireNonNull(environment, "environment");
    this.defaultSettings = Objects.requireNonNull(defaultSettings, "defaultSettings");
    this.platformY = platformY;
    this.saveBeforeUnload = saveBeforeUnload;
    this.pluginVersion = Objects.requireNonNull(pluginVersion, "pluginVersion");
    this.arenaLinks = Objects.requireNonNull(arenaLinks, "arenaLinks");
    this.protectedTemplate = Objects.requireNonNull(protectedTemplate, "protectedTemplate");
  }

  /** Loads metadata and recalculates runtime loaded state without auto-loading templates. */
  public synchronized MapTemplateLoadResult reload() throws IOException {
    MapTemplateLoadResult loaded = repository.loadAll();
    List<MapTemplate> normalized =
        loaded.templates().stream()
            .map(
                template -> {
                  List<String> problems = validator.validate(template);
                  if (!problems.isEmpty())
                    return template.failed(String.join(", ", problems), now());
                  if (!files.templateExists(template) && !worlds.isLoaded(template))
                    return template.failed("Template world folder is missing", now());
                  return template.normalized(
                      worlds.isLoaded(template) ? MapState.LOADED : MapState.UNLOADED);
                })
            .toList();
    registry.replace(normalized);
    return new MapTemplateLoadResult(normalized, loaded.failures());
  }

  /** Creates and loads one empty construction world; caller must be on the server thread. */
  public MapOperationResult create(String rawId, MapType type, String author) {
    final MapId id;
    try {
      id = MapId.parse(rawId);
    } catch (RuntimeException exception) {
      return MapOperationResult.failure(MapOperationCode.INVALID_ID, exception.getMessage());
    }
    if (registry.find(id).isPresent() || repository.exists(id))
      return MapOperationResult.failure(MapOperationCode.ALREADY_EXISTS, id.value());
    if (!locks.acquire(id))
      return MapOperationResult.failure(MapOperationCode.OPERATION_IN_PROGRESS, id.value());
    MapTemplate template =
        MapTemplate.create(
            id,
            Objects.requireNonNull(type, "type"),
            worldPrefix + id.value(),
            environment,
            defaultSettings,
            platformY,
            author,
            now());
    try {
      if (registry.findByWorld(template.worldName()).isPresent())
        return MapOperationResult.failure(MapOperationCode.ALREADY_EXISTS, template.worldName());
      files.createTemplateDirectory(template);
      MapWorldResult world = worlds.createVoidWorld(template);
      if (!world.successful()) {
        cleanupFailedCreate(template);
        return MapOperationResult.failure(MapOperationCode.WORLD_CREATION_FAILED, world.detail());
      }
      MapTemplate created = template.loadedAt(now());
      repository.save(created);
      registry.put(created);
      return MapOperationResult.success(created);
    } catch (IOException exception) {
      cleanupFailedCreate(template);
      return MapOperationResult.failure(MapOperationCode.STORAGE_ERROR, exception.getMessage());
    } finally {
      locks.release(id);
    }
  }

  /** Loads an existing template world on the server thread. */
  public MapOperationResult load(String rawId) {
    MapTemplate template = find(rawId).orElse(null);
    if (template == null) return MapOperationResult.failure(MapOperationCode.NOT_FOUND, rawId);
    if (template.loaded() || worlds.isLoaded(template))
      return MapOperationResult.failure(MapOperationCode.ALREADY_LOADED, template, rawId);
    return worldMutation(
        template,
        MapState.LOADING,
        worlds::load,
        MapOperationCode.WORLD_LOAD_FAILED,
        value -> value.loadedAt(now()));
  }

  /** Saves a loaded world and its metadata on the server thread. */
  public MapOperationResult save(String rawId) {
    MapTemplate template = find(rawId).orElse(null);
    if (template == null) return MapOperationResult.failure(MapOperationCode.NOT_FOUND, rawId);
    if (!template.loaded() || !worlds.isLoaded(template))
      return MapOperationResult.failure(MapOperationCode.NOT_LOADED, template, rawId);
    return worldMutation(
        template,
        MapState.SAVING,
        worlds::save,
        MapOperationCode.WORLD_SAVE_FAILED,
        value -> value.savedAt(now()));
  }

  /** Unloads a world; normal mode refuses players and force mode is prepared by the platform. */
  public MapOperationResult unload(String rawId, boolean force) {
    MapTemplate template = find(rawId).orElse(null);
    if (template == null) return MapOperationResult.failure(MapOperationCode.NOT_FOUND, rawId);
    if (!template.loaded() || !worlds.isLoaded(template))
      return MapOperationResult.failure(MapOperationCode.NOT_LOADED, template, rawId);
    if (worlds.playerCount(template) > 0 && !force)
      return MapOperationResult.failure(MapOperationCode.PLAYERS_PRESENT, template, rawId);
    return worldMutation(
        template,
        MapState.UNLOADING,
        value -> worlds.unload(value, saveBeforeUnload),
        MapOperationCode.WORLD_UNLOAD_FAILED,
        value -> value.unloadedAt(now()));
  }

  /**
   * Copies a saved template folder and creates independent metadata.
   *
   * <p>This method performs no Bukkit call and is safe to schedule off the server thread.
   */
  public MapOperationResult duplicate(String rawSource, String rawDestination, String author) {
    final MapId destination;
    try {
      destination = MapId.parse(rawDestination);
    } catch (RuntimeException exception) {
      return MapOperationResult.failure(MapOperationCode.INVALID_ID, exception.getMessage());
    }
    MapTemplate source = find(rawSource).orElse(null);
    if (source == null) return MapOperationResult.failure(MapOperationCode.NOT_FOUND, rawSource);
    if (registry.find(destination).isPresent() || repository.exists(destination))
      return MapOperationResult.failure(MapOperationCode.ALREADY_EXISTS, destination.value());
    if (!locks.acquire(source.id(), destination))
      return MapOperationResult.failure(
          MapOperationCode.OPERATION_IN_PROGRESS, destination.value());
    MapTemplate copy =
        MapTemplate.create(
            destination,
            source.type(),
            worldPrefix + destination.value(),
            source.environment(),
            source.settings(),
            platformY,
            author,
            now());
    try {
      files.duplicate(source, copy);
      repository.save(copy);
      registry.put(copy);
      return MapOperationResult.success(copy);
    } catch (IOException exception) {
      try {
        files.deleteTemplate(copy);
        repository.delete(destination);
      } catch (IOException ignored) {
        // The original failure is the actionable result; cleanup is best effort and never
        // publishes.
      }
      return MapOperationResult.failure(MapOperationCode.COPY_FAILED, exception.getMessage());
    } finally {
      locks.release(source.id(), destination);
    }
  }

  /**
   * Validates a deletion and unloads the Bukkit world before the filesystem phase.
   *
   * <p>A successful result deliberately keeps the per-map lock until {@link
   * #completeDelete(String)} is called. This phase must run on the server thread when the world
   * implementation requires it.
   */
  public MapOperationResult prepareDelete(String rawId) {
    MapTemplate template = find(rawId).orElse(null);
    if (template == null) return MapOperationResult.failure(MapOperationCode.NOT_FOUND, rawId);
    if (!arenaLinks.apply(template.id()).isEmpty() || protectedTemplate.test(template))
      return MapOperationResult.failure(MapOperationCode.MAP_LINKED, template, rawId);
    if (!locks.acquire(template.id()))
      return MapOperationResult.failure(MapOperationCode.OPERATION_IN_PROGRESS, template, rawId);
    if (template.loaded() || worlds.isLoaded(template)) {
      if (worlds.playerCount(template) > 0) {
        locks.release(template.id());
        return MapOperationResult.failure(MapOperationCode.PLAYERS_PRESENT, template, rawId);
      }
      MapWorldResult unload = worlds.unload(template, true);
      if (!unload.successful()) {
        locks.release(template.id());
        return MapOperationResult.failure(
            MapOperationCode.WORLD_UNLOAD_FAILED, template, unload.detail());
      }
    }
    return MapOperationResult.success(template);
  }

  /** Performs the backup and confined filesystem deletion; this phase may run asynchronously. */
  public MapOperationResult completeDelete(String rawId) {
    MapTemplate template = find(rawId).orElse(null);
    if (template == null) return MapOperationResult.failure(MapOperationCode.NOT_FOUND, rawId);
    if (!locks.active(template.id()))
      return MapOperationResult.failure(MapOperationCode.INVALID_ARGUMENT, template, rawId);
    try {
      try {
        files.backup(template, "DELETE", pluginVersion);
      } catch (IOException exception) {
        return MapOperationResult.failure(
            MapOperationCode.BACKUP_FAILED, template, exception.getMessage());
      }
      files.deleteTemplate(template);
      repository.delete(template.id());
      registry.remove(template.id());
      return MapOperationResult.success(template);
    } catch (IOException exception) {
      registry.put(template.failed(exception.getMessage(), now()));
      return MapOperationResult.failure(
          MapOperationCode.STORAGE_ERROR, template, exception.getMessage());
    } finally {
      locks.release(template.id());
    }
  }

  /** Convenience wrapper for non-Bukkit callers and tests. */
  public MapOperationResult delete(String rawId) {
    MapOperationResult prepared = prepareDelete(rawId);
    return prepared.successful() ? completeDelete(rawId) : prepared;
  }

  public MapOperationResult setDisplayName(
      String rawId, String displayName, long expectedRevision) {
    String value = displayName == null ? "" : displayName.trim();
    if (value.isEmpty() || value.length() > 64)
      return MapOperationResult.failure(MapOperationCode.INVALID_ARGUMENT, "display name");
    return metadataEdit(rawId, expectedRevision, map -> map.withDisplayName(value, now()));
  }

  public MapOperationResult setSpawn(String rawId, MapSpawn spawn, long expectedRevision) {
    MapTemplate template = find(rawId).orElse(null);
    if (template == null) return MapOperationResult.failure(MapOperationCode.NOT_FOUND, rawId);
    if (!spawn.world().equals(template.worldName()))
      return MapOperationResult.failure(MapOperationCode.INVALID_ARGUMENT, "spawn world");
    return metadataEdit(rawId, expectedRevision, map -> map.withSpawn(spawn, now()));
  }

  /** Reconciles derived links; arena definitions remain the source of truth. */
  public synchronized void synchronizeLinks(
      Collection<fr.heneria.bedwars.core.arena.ArenaDefinition> arenas) {
    for (MapTemplate template : list()) {
      Set<String> linked = new LinkedHashSet<>();
      for (var arena : arenas)
        if (arena.template().filter(template.id().value()::equalsIgnoreCase).isPresent())
          linked.add(arena.id().value());
      MapTemplate updated = template.withLinks(linked, now());
      if (updated == template) continue;
      try {
        repository.save(updated);
        registry.put(updated);
      } catch (IOException ignored) {
        // The active relation stays sourced from arenas; stale derived metadata is retried later.
      }
    }
  }

  public List<MapTemplate> list() {
    return registry.all();
  }

  public Optional<MapTemplate> find(String rawId) {
    try {
      return registry.find(MapId.parse(rawId));
    } catch (RuntimeException exception) {
      return Optional.empty();
    }
  }

  public Optional<MapTemplate> findByWorld(String world) {
    return registry.findByWorld(world);
  }

  public boolean validBedWarsTemplate(String rawId) {
    return find(rawId)
        .filter(template -> template.type() == MapType.BEDWARS)
        .filter(template -> template.state() != MapState.ERROR)
        .isPresent();
  }

  public boolean operationActive(String rawId) {
    return find(rawId).map(MapTemplate::id).map(locks::active).orElse(false);
  }

  private MapOperationResult metadataEdit(
      String rawId, long expectedRevision, java.util.function.UnaryOperator<MapTemplate> editor) {
    MapTemplate template = find(rawId).orElse(null);
    if (template == null) return MapOperationResult.failure(MapOperationCode.NOT_FOUND, rawId);
    if (template.revision() != expectedRevision)
      return MapOperationResult.failure(MapOperationCode.CONFLICT, template, rawId);
    if (!locks.acquire(template.id()))
      return MapOperationResult.failure(MapOperationCode.OPERATION_IN_PROGRESS, template, rawId);
    try {
      MapTemplate updated = editor.apply(template);
      repository.save(updated);
      registry.put(updated);
      return MapOperationResult.success(updated);
    } catch (IOException exception) {
      return MapOperationResult.failure(
          MapOperationCode.STORAGE_ERROR, template, exception.getMessage());
    } finally {
      locks.release(template.id());
    }
  }

  private MapOperationResult worldMutation(
      MapTemplate template,
      MapState transitional,
      java.util.function.Function<MapTemplate, MapWorldResult> operation,
      MapOperationCode failureCode,
      java.util.function.UnaryOperator<MapTemplate> success) {
    if (!locks.acquire(template.id()))
      return MapOperationResult.failure(
          MapOperationCode.OPERATION_IN_PROGRESS, template, template.id().value());
    registry.put(template.transition(transitional, now()));
    try {
      MapWorldResult result = operation.apply(template);
      if (!result.successful()) {
        registry.put(template);
        return MapOperationResult.failure(failureCode, template, result.detail());
      }
      MapTemplate updated = success.apply(template);
      repository.save(updated);
      registry.put(updated);
      return MapOperationResult.success(updated);
    } catch (IOException exception) {
      registry.put(template.failed(exception.getMessage(), now()));
      return MapOperationResult.failure(
          MapOperationCode.STORAGE_ERROR, template, exception.getMessage());
    } finally {
      locks.release(template.id());
    }
  }

  private void cleanupFailedCreate(MapTemplate template) {
    try {
      if (worlds.isLoaded(template)) worlds.unload(template, false);
      files.deleteTemplate(template);
      repository.delete(template.id());
    } catch (IOException ignored) {
      // No incomplete template is published; residual files remain diagnostic-only.
    }
  }

  private static String requirePrefix(String value) {
    String prefix = Objects.requireNonNull(value, "worldPrefix").toLowerCase(Locale.ROOT);
    if (!prefix.matches("[a-z0-9_-]{1,32}"))
      throw new IllegalArgumentException("Invalid template world prefix");
    return prefix;
  }

  private Instant now() {
    return clock.instant();
  }
}
