package fr.heneria.zombie.plugin.editor;

import fr.heneria.zombie.core.editor.MapDefinition;
import fr.heneria.zombie.plugin.map.MapTemplateCatalog;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.bukkit.Bukkit;
import org.bukkit.World;

/**
 * Saves an editing world on Paper's thread and mirrors it into the template catalogue off-thread.
 */
public final class MapWorldPublicationService {
  private final Path worldContainer;
  private final MapTemplateCatalog templates;
  private final Executor ioExecutor;

  public MapWorldPublicationService(
      Path worldContainer, MapTemplateCatalog templates, Executor ioExecutor) {
    this.worldContainer = worldContainer.toAbsolutePath().normalize();
    this.templates = Objects.requireNonNull(templates, "templates");
    this.ioExecutor = Objects.requireNonNull(ioExecutor, "ioExecutor");
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
    String newWorld = "zombie_editing/hz_edit_" + newMapId;
    Path target = worldContainer.resolve(newWorld).toAbsolutePath().normalize();
    if (!source.startsWith(worldContainer) || !target.startsWith(worldContainer)) {
      return CompletableFuture.failedFuture(
          new IllegalArgumentException("Editing world path escapes world container"));
    }
    if (java.nio.file.Files.exists(target)) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("Le monde d'édition cible existe déjà"));
    }
    return CompletableFuture.supplyAsync(
        () -> {
          WorldDirectoryCopier.replace(source, target);
          return newWorld;
        },
        ioExecutor);
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
}
