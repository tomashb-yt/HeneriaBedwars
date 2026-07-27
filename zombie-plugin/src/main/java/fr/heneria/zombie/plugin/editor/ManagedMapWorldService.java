package fr.heneria.zombie.plugin.editor;

import fr.heneria.zombie.core.editor.MapDefinition;
import fr.heneria.zombie.plugin.map.MapTemplateCatalog;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

/**
 * Owns creation, import, load, save, unload, duplication and publication of editing worlds.
 *
 * <p>Filesystem work is delegated to the bounded I/O executor. Bukkit world operations are executed
 * exclusively through the injected main-thread executor.
 */
public final class ManagedMapWorldService {
  private final Path worldContainer;
  private final MapTemplateCatalog templates;
  private final Executor ioExecutor;
  private final Executor mainThread;

  public ManagedMapWorldService(
      Path worldContainer, MapTemplateCatalog templates, Executor ioExecutor, Executor mainThread) {
    this.worldContainer = worldContainer.toAbsolutePath().normalize();
    this.templates = Objects.requireNonNull(templates, "templates");
    this.ioExecutor = Objects.requireNonNull(ioExecutor, "ioExecutor");
    this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
  }

  /**
   * Imports an immutable template into a dedicated, plugin-owned editing world.
   *
   * <p>The source template is never loaded or modified. Copying runs on the I/O executor and the
   * resulting world is loaded only on Paper's main thread.
   *
   * @param mapId safe template and future map identifier
   * @return dedicated editing world name
   */
  public CompletableFuture<String> importTemplate(String mapId) {
    if (!Bukkit.isPrimaryThread() || !MapDefinition.safeId(mapId)) {
      return CompletableFuture.failedFuture(
          new IllegalArgumentException("Invalid imported map or thread"));
    }
    Path target = editingDirectory(mapId);
    return CompletableFuture.supplyAsync(() -> java.nio.file.Files.exists(target), ioExecutor)
        .thenCompose(
            exists ->
                exists
                    ? CompletableFuture.failedFuture(
                        new IllegalStateException("Le monde d'édition existe déjà"))
                    : templates.find(mapId))
        .thenCompose(
            template ->
                template.isPresent()
                    ? CompletableFuture.runAsync(
                        () ->
                            WorldDirectoryCopier.replace(templates.sourceDirectory(mapId), target),
                        ioExecutor)
                    : CompletableFuture.failedFuture(
                        new IllegalArgumentException("Template absent ou invalide : " + mapId)))
        .thenApplyAsync(ignored -> loadOwnedWorld(mapId), mainThread)
        .exceptionallyCompose(
            failure ->
                CompletableFuture.supplyAsync(
                        () -> {
                          World partial = Bukkit.getWorld(editingWorldName(mapId));
                          return partial == null || Bukkit.unloadWorld(partial, false);
                        },
                        mainThread)
                    .thenCompose(
                        unloaded ->
                            unloaded
                                ? CompletableFuture.runAsync(
                                    () -> WorldDirectoryCopier.deleteOwned(target, editingRoot()),
                                    ioExecutor)
                                : CompletableFuture.completedFuture(null))
                    .handle((ignored, cleanupFailure) -> null)
                    .thenCompose(
                        ignored ->
                            CompletableFuture.failedFuture(
                                failure instanceof java.util.concurrent.CompletionException
                                        && failure.getCause() != null
                                    ? failure.getCause()
                                    : failure)));
  }

  /**
   * Creates a new empty editing world owned by the plugin.
   *
   * @param mapId future map identifier
   * @return editing world name
   */
  public CompletableFuture<String> createEditingWorld(String mapId) {
    if (!Bukkit.isPrimaryThread() || !MapDefinition.safeId(mapId)) {
      return CompletableFuture.failedFuture(
          new IllegalArgumentException("Invalid new map or thread"));
    }
    return CompletableFuture.supplyAsync(
            () -> java.nio.file.Files.exists(editingDirectory(mapId)), ioExecutor)
        .thenCompose(
            exists ->
                exists
                    ? CompletableFuture.failedFuture(
                        new IllegalStateException("Le monde d'édition existe déjà"))
                    : CompletableFuture.supplyAsync(() -> loadOwnedWorld(mapId), mainThread));
  }

  /**
   * Loads the world referenced by a map definition.
   *
   * @param definition map definition
   * @return loaded world, or {@code null} when its safe directory does not exist
   */
  public CompletableFuture<World> loadEditingWorld(MapDefinition definition) {
    if (!Bukkit.isPrimaryThread()) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("Editing world load must start on the Paper thread"));
    }
    World loaded = Bukkit.getWorld(definition.world());
    if (loaded != null) {
      return CompletableFuture.completedFuture(loaded);
    }
    Path candidate = worldContainer.resolve(definition.world()).toAbsolutePath().normalize();
    if (!candidate.startsWith(worldContainer)) {
      return CompletableFuture.completedFuture(null);
    }
    return CompletableFuture.supplyAsync(
            () -> java.nio.file.Files.isDirectory(candidate), ioExecutor)
        .thenApplyAsync(
            exists -> exists ? Bukkit.createWorld(new WorldCreator(definition.world())) : null,
            mainThread);
  }

  /**
   * Flushes the editable blocks to disk on Paper's main thread.
   *
   * @param definition map definition
   * @return completed save
   */
  public CompletableFuture<Void> saveEditingWorld(MapDefinition definition) {
    if (!Bukkit.isPrimaryThread()) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("Editing world save must run on the Paper thread"));
    }
    World world = Bukkit.getWorld(definition.world());
    if (world == null) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("Le monde d'édition n'est pas chargé"));
    }
    world.save();
    return CompletableFuture.completedFuture(null);
  }

  /**
   * Saves and unloads a plugin-owned editing world with no player inside.
   *
   * @param definition map definition
   * @return whether a loaded world was unloaded
   */
  public CompletableFuture<Boolean> unloadEditingWorld(MapDefinition definition) {
    if (!Bukkit.isPrimaryThread()) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("Editing world unload must run on the Paper thread"));
    }
    if (!worldContainer
        .resolve(definition.world())
        .toAbsolutePath()
        .normalize()
        .equals(editingDirectory(definition.id()))) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("Le monde n'appartient pas au gestionnaire"));
    }
    World world = Bukkit.getWorld(definition.world());
    if (world == null) {
      return CompletableFuture.completedFuture(false);
    }
    if (!world.getPlayers().isEmpty()) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("Des joueurs sont encore présents sur ce monde"));
    }
    world.save();
    if (!Bukkit.unloadWorld(world, true)) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("Paper a refusé de décharger le monde"));
    }
    return CompletableFuture.completedFuture(true);
  }

  /** Saves and copies the physically separate editing world into the public template directory. */
  public CompletableFuture<Void> updateTemplate(MapDefinition definition) {
    if (!Bukkit.isPrimaryThread()) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("Editing world save must start on the Paper thread"));
    }
    World world = Bukkit.getWorld(definition.world());
    if (world == null) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("Monde d'édition non chargé : " + definition.world()));
    }
    world.save();
    Path source = worldContainer.resolve(world.getName()).toAbsolutePath().normalize();
    if (!source.startsWith(worldContainer)) {
      return CompletableFuture.failedFuture(
          new IllegalArgumentException("Editing world escapes the server world container"));
    }
    Path target = templates.sourceDirectory(definition.id());
    return CompletableFuture.runAsync(
            () -> {
              WorldDirectoryCopier.replace(source, target);
              writeTechnicalMetadata(definition, target);
            },
            ioExecutor)
        .thenRun(templates::refreshCount);
  }

  /** Duplicates a saved editing world into a new isolated editing workspace. */
  public CompletableFuture<String> duplicateEditingWorld(
      MapDefinition sourceDefinition, String newMapId) {
    if (!MapDefinition.safeId(newMapId) || !Bukkit.isPrimaryThread()) {
      return CompletableFuture.failedFuture(
          new IllegalArgumentException("Invalid duplicated map or thread"));
    }
    World sourceWorld = Bukkit.getWorld(sourceDefinition.world());
    if (sourceWorld == null) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("Le monde source n'est pas chargé"));
    }
    sourceWorld.save();
    Path source = worldContainer.resolve(sourceWorld.getName()).toAbsolutePath().normalize();
    String newWorld = editingWorldName(newMapId);
    Path target = editingDirectory(newMapId);
    if (!source.startsWith(worldContainer) || !target.startsWith(worldContainer)) {
      return CompletableFuture.failedFuture(
          new IllegalArgumentException("Editing world path escapes world container"));
    }
    return CompletableFuture.supplyAsync(() -> java.nio.file.Files.exists(target), ioExecutor)
        .thenCompose(
            exists ->
                exists
                    ? CompletableFuture.failedFuture(
                        new IllegalStateException("Le monde d'édition cible existe déjà"))
                    : CompletableFuture.supplyAsync(
                        () -> {
                          WorldDirectoryCopier.replace(source, target);
                          return newWorld;
                        },
                        ioExecutor));
  }

  /**
   * Unloads the plugin-owned editing world before its directory is permanently removed.
   *
   * <p>Maps created from an existing server world never own that world and therefore never unload
   * or delete it.
   */
  public CompletableFuture<Void> prepareDeletion(MapDefinition definition) {
    if (!Bukkit.isPrimaryThread()) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("Map deletion must start on the Paper thread"));
    }
    Path configured = worldContainer.resolve(definition.world()).toAbsolutePath().normalize();
    Path owned = editingDirectory(definition.id());
    if (!configured.equals(owned)) {
      return CompletableFuture.completedFuture(null);
    }
    World world = Bukkit.getWorld(definition.world());
    if (world == null) {
      return CompletableFuture.completedFuture(null);
    }
    if (!world.getPlayers().isEmpty()) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("Des joueurs se trouvent encore dans le monde d'édition"));
    }
    if (!Bukkit.unloadWorld(world, false)) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("Le monde d'édition n'a pas pu être déchargé"));
    }
    return CompletableFuture.completedFuture(null);
  }

  private static void writeTechnicalMetadata(MapDefinition definition, Path target) {
    fr.heneria.zombie.core.editor.MapPoint spawn =
        definition
            .playerSpawn()
            .orElseThrow(() -> new IllegalStateException("Spawn joueur manquant"));
    org.bukkit.configuration.file.YamlConfiguration yaml =
        new org.bukkit.configuration.file.YamlConfiguration();
    yaml.set("schema-version", 1);
    yaml.set("map-id", definition.id());
    yaml.set("maximum-players", definition.maximumPlayers());
    yaml.set("spawn.x", spawn.x());
    yaml.set("spawn.y", spawn.y());
    yaml.set("spawn.z", spawn.z());
    yaml.set("spawn.yaw", spawn.yaw());
    yaml.set("spawn.pitch", spawn.pitch());
    try {
      yaml.save(target.resolve("zombie-map.yml").toFile());
    } catch (java.io.IOException failure) {
      throw new java.util.concurrent.CompletionException(failure);
    }
  }

  private String loadOwnedWorld(String mapId) {
    String worldName = editingWorldName(mapId);
    World world = Bukkit.createWorld(new WorldCreator(worldName));
    if (world == null) {
      throw new IllegalStateException("Paper refuse de charger le monde d'édition " + worldName);
    }
    world.setAutoSave(true);
    return worldName;
  }

  private String editingWorldName(String mapId) {
    return "zombie_editing/hz_edit_" + mapId;
  }

  private Path editingDirectory(String mapId) {
    Path directory = editingRoot().resolve("hz_edit_" + mapId).toAbsolutePath().normalize();
    if (!directory.startsWith(editingRoot())) {
      throw new IllegalArgumentException("Editing world escapes owned root");
    }
    return directory;
  }

  private Path editingRoot() {
    return worldContainer.resolve("zombie_editing").toAbsolutePath().normalize();
  }
}
