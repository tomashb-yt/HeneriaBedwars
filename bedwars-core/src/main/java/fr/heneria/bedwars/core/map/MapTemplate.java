package fr.heneria.bedwars.core.map;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable persistent construction template.
 *
 * <p>It contains no Bukkit object and must never be confused with a temporary game instance.
 * Runtime state is normalized on startup; disk changes are published only after their operation
 * succeeds.
 */
public record MapTemplate(
    int configVersion,
    long revision,
    MapId id,
    String displayName,
    MapType type,
    MapState state,
    String folderName,
    String worldName,
    String environment,
    MapGeneratorType generatorType,
    MapSpawn spawn,
    MapWorldSettings settings,
    Instant createdAt,
    Instant updatedAt,
    Optional<Instant> lastLoadedAt,
    Optional<Instant> lastSavedAt,
    String author,
    String description,
    Set<String> tags,
    Set<String> linkedArenaIds,
    boolean dirty,
    Optional<String> error) {
  public static final int CURRENT_CONFIG_VERSION = 1;

  public MapTemplate {
    if (configVersion != CURRENT_CONFIG_VERSION)
      throw new IllegalArgumentException("Unsupported map template version");
    if (revision < 1) throw new IllegalArgumentException("Map revision must be positive");
    id = Objects.requireNonNull(id, "id");
    displayName = requireText(displayName, "displayName");
    type = Objects.requireNonNull(type, "type");
    state = Objects.requireNonNull(state, "state");
    folderName = requireText(folderName, "folderName");
    worldName = requireText(worldName, "worldName");
    environment = requireText(environment, "environment");
    generatorType = Objects.requireNonNull(generatorType, "generatorType");
    spawn = Objects.requireNonNull(spawn, "spawn");
    settings = Objects.requireNonNull(settings, "settings");
    createdAt = Objects.requireNonNull(createdAt, "createdAt");
    updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    lastLoadedAt = Objects.requireNonNull(lastLoadedAt, "lastLoadedAt");
    lastSavedAt = Objects.requireNonNull(lastSavedAt, "lastSavedAt");
    author = Objects.requireNonNull(author, "author").trim();
    description = Objects.requireNonNull(description, "description").trim();
    tags = Set.copyOf(tags);
    linkedArenaIds = Set.copyOf(linkedArenaIds);
    error = Objects.requireNonNull(error, "error").map(String::trim).filter(s -> !s.isEmpty());
  }

  public static MapTemplate create(
      MapId id,
      MapType type,
      String worldName,
      String environment,
      MapWorldSettings settings,
      double spawnY,
      String author,
      Instant now) {
    return new MapTemplate(
        CURRENT_CONFIG_VERSION,
        1,
        id,
        id.value(),
        type,
        MapState.UNLOADED,
        id.value(),
        worldName,
        environment,
        MapGeneratorType.VOID,
        MapSpawn.defaultSpawn(worldName, spawnY),
        settings,
        now,
        now,
        Optional.empty(),
        Optional.empty(),
        author == null ? "" : author,
        "",
        Set.of(),
        Set.of(),
        false,
        Optional.empty());
  }

  public boolean loaded() {
    return state == MapState.LOADED;
  }

  public boolean operational() {
    return state != MapState.LOADING && state != MapState.UNLOADING && state != MapState.SAVING;
  }

  public MapTemplate transition(MapState value, Instant now) {
    return copy(
        revision,
        displayName,
        type,
        value,
        spawn,
        updatedAt,
        lastLoadedAt,
        lastSavedAt,
        linkedArenaIds,
        dirty,
        value == MapState.ERROR ? error : Optional.empty());
  }

  public MapTemplate loadedAt(Instant now) {
    return copy(
        revision + 1,
        displayName,
        type,
        MapState.LOADED,
        spawn,
        now,
        Optional.of(now),
        lastSavedAt,
        linkedArenaIds,
        false,
        Optional.empty());
  }

  public MapTemplate unloadedAt(Instant now) {
    return copy(
        revision + 1,
        displayName,
        type,
        MapState.UNLOADED,
        spawn,
        now,
        lastLoadedAt,
        lastSavedAt,
        linkedArenaIds,
        false,
        Optional.empty());
  }

  public MapTemplate savedAt(Instant now) {
    return copy(
        revision + 1,
        displayName,
        type,
        MapState.LOADED,
        spawn,
        now,
        lastLoadedAt,
        Optional.of(now),
        linkedArenaIds,
        false,
        Optional.empty());
  }

  public MapTemplate withDisplayName(String value, Instant now) {
    return copy(
        revision + 1,
        requireText(value, "displayName"),
        type,
        state,
        spawn,
        now,
        lastLoadedAt,
        lastSavedAt,
        linkedArenaIds,
        dirty,
        error);
  }

  public MapTemplate withType(MapType value, Instant now) {
    return copy(
        revision + 1,
        displayName,
        value,
        state,
        spawn,
        now,
        lastLoadedAt,
        lastSavedAt,
        linkedArenaIds,
        dirty,
        error);
  }

  public MapTemplate withSpawn(MapSpawn value, Instant now) {
    return copy(
        revision + 1,
        displayName,
        type,
        state,
        value,
        now,
        lastLoadedAt,
        lastSavedAt,
        linkedArenaIds,
        true,
        error);
  }

  public MapTemplate withLinks(Set<String> values, Instant now) {
    if (linkedArenaIds.equals(values)) return this;
    return copy(
        revision + 1,
        displayName,
        type,
        state,
        spawn,
        now,
        lastLoadedAt,
        lastSavedAt,
        values,
        dirty,
        error);
  }

  public MapTemplate failed(String detail, Instant now) {
    return copy(
        revision,
        displayName,
        type,
        MapState.ERROR,
        spawn,
        now,
        lastLoadedAt,
        lastSavedAt,
        linkedArenaIds,
        dirty,
        Optional.ofNullable(detail));
  }

  /** Runtime normalization after restart does not create a new administrative revision. */
  public MapTemplate normalized(MapState value) {
    return copy(
        revision,
        displayName,
        type,
        value,
        spawn,
        updatedAt,
        lastLoadedAt,
        lastSavedAt,
        linkedArenaIds,
        false,
        Optional.empty());
  }

  private MapTemplate copy(
      long nextRevision,
      String nextName,
      MapType nextType,
      MapState nextState,
      MapSpawn nextSpawn,
      Instant nextUpdated,
      Optional<Instant> nextLoaded,
      Optional<Instant> nextSaved,
      Set<String> nextLinks,
      boolean nextDirty,
      Optional<String> nextError) {
    return new MapTemplate(
        configVersion,
        nextRevision,
        id,
        nextName,
        nextType,
        nextState,
        folderName,
        worldName,
        environment,
        generatorType,
        nextSpawn,
        settings,
        createdAt,
        nextUpdated,
        nextLoaded,
        nextSaved,
        author,
        description,
        tags,
        nextLinks,
        nextDirty,
        nextError);
  }

  private static String requireText(String value, String name) {
    String result = Objects.requireNonNull(value, name).trim();
    if (result.isEmpty()) throw new IllegalArgumentException(name + " is blank");
    return result;
  }
}
