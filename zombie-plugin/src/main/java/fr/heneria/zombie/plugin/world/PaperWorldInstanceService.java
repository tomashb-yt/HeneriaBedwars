package fr.heneria.zombie.plugin.world;

import fr.heneria.zombie.core.config.ZombieSettings.InstanceOptions;
import fr.heneria.zombie.core.config.ZombieSettings.WorldRuleOptions;
import fr.heneria.zombie.core.instance.WorldInstanceGateway;
import fr.heneria.zombie.core.instance.WorldInstanceHandle;
import fr.heneria.zombie.plugin.config.ConfigurationManager;
import fr.heneria.zombie.plugin.map.MapTemplateCatalog;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.Plugin;

/** Paper adapter that copies templates asynchronously and owns every runtime world. */
public final class PaperWorldInstanceService implements WorldInstanceGateway {

  private final Plugin plugin;
  private final Path worldContainer;
  private final ConfigurationManager configurations;
  private final MapTemplateCatalog templates;
  private final Executor ioExecutor;
  private final Executor mainThread;
  private final ConcurrentMap<String, UUID> worldOwners = new ConcurrentHashMap<>();

  /**
   * Creates the world service.
   *
   * @param plugin owning plugin
   * @param worldContainer Paper world container
   * @param configurations active settings
   * @param templates template catalog
   * @param ioExecutor file executor
   * @param mainThread Paper main-thread executor
   */
  public PaperWorldInstanceService(
      Plugin plugin,
      Path worldContainer,
      ConfigurationManager configurations,
      MapTemplateCatalog templates,
      Executor ioExecutor,
      Executor mainThread) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.worldContainer =
        Objects.requireNonNull(worldContainer, "worldContainer").toAbsolutePath().normalize();
    this.configurations = Objects.requireNonNull(configurations, "configurations");
    this.templates = Objects.requireNonNull(templates, "templates");
    this.ioExecutor = Objects.requireNonNull(ioExecutor, "ioExecutor");
    this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
  }

  @Override
  public CompletableFuture<WorldInstanceHandle> prepare(UUID instanceId, String mapId) {
    String worldName = runtimeWorldName(instanceId);
    Path source = templates.sourceDirectory(mapId);
    Path destination = runtimeDirectory(worldName);
    return CompletableFuture.runAsync(() -> copyTemplate(source, destination), ioExecutor)
        .thenApplyAsync(
            ignored -> {
              World world = Bukkit.createWorld(new WorldCreator(worldName));
              if (world == null) {
                throw new IllegalStateException("Paper refused to load world " + worldName);
              }
              worldOwners.put(world.getName(), instanceId);
              applyRules(world, configurations.current().settings().worldRules());
              return new WorldInstanceHandle(world.getName());
            },
            mainThread)
        .exceptionallyCompose(
            failure -> {
              if (configurations.current().settings().instances().preserveFailedWorlds()) {
                return CompletableFuture.failedFuture(unwrap(failure));
              }
              return unloadFailedWorld(worldName)
                  .thenCompose(
                      unloaded ->
                          unloaded
                              ? CompletableFuture.runAsync(
                                  () -> deleteDirectory(destination), ioExecutor)
                              : CompletableFuture.completedFuture(null))
                  .thenCompose(ignored -> CompletableFuture.failedFuture(unwrap(failure)));
            });
  }

  @Override
  public CompletableFuture<Boolean> destroy(WorldInstanceHandle handle, boolean preserveOnFailure) {
    InstanceOptions options = configurations.current().settings().instances();
    return delayed(options.unloadDelaySeconds())
        .thenApplyAsync(
            ignored -> {
              World world = Bukkit.getWorld(handle.worldName());
              if (world != null && !Bukkit.unloadWorld(world, false)) {
                return false;
              }
              worldOwners.remove(handle.worldName());
              return true;
            },
            mainThread)
        .thenCompose(
            unloaded -> {
              if (!unloaded || !options.deleteWorldAfterGame()) {
                return CompletableFuture.completedFuture(unloaded);
              }
              return CompletableFuture.supplyAsync(
                  () -> {
                    try {
                      deleteDirectory(runtimeDirectory(handle.worldName()));
                      return true;
                    } catch (CompletionException deletionFailure) {
                      if (!preserveOnFailure) {
                        plugin
                            .getLogger()
                            .warning(
                                "Instance world cleanup failed: "
                                    + deletionFailure.getCause().getMessage());
                      }
                      return false;
                    }
                  },
                  ioExecutor);
            });
  }

  /**
   * Finds the owner of a runtime world.
   *
   * @param worldName exact world name
   * @return optional instance identifier
   */
  public Optional<UUID> ownerOf(String worldName) {
    return Optional.ofNullable(worldOwners.get(worldName));
  }

  /**
   * Returns whether this service owns the world.
   *
   * @param worldName exact world name
   * @return ownership
   */
  public boolean isInstanceWorld(String worldName) {
    return worldOwners.containsKey(worldName);
  }

  /**
   * Unloads all known worlds synchronously during plugin shutdown and preserves their files.
   *
   * @return names that could not be unloaded
   */
  public java.util.List<String> unloadAllPreservingFiles() {
    if (!Bukkit.isPrimaryThread()) {
      throw new IllegalStateException("Shutdown world unload must run on the server thread");
    }
    java.util.List<String> failures = new java.util.ArrayList<>();
    for (String worldName : java.util.List.copyOf(worldOwners.keySet())) {
      World world = Bukkit.getWorld(worldName);
      if (world != null && !Bukkit.unloadWorld(world, false)) {
        failures.add(worldName);
      } else {
        worldOwners.remove(worldName);
      }
    }
    return java.util.List.copyOf(failures);
  }

  private CompletableFuture<Void> delayed(int seconds) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    Bukkit.getScheduler()
        .runTaskLater(plugin, () -> future.complete(null), Math.max(0, seconds) * 20L);
    return future;
  }

  private CompletableFuture<Boolean> unloadFailedWorld(String worldName) {
    return CompletableFuture.supplyAsync(
        () -> {
          World world = Bukkit.getWorld(worldName);
          if (world != null && !Bukkit.unloadWorld(world, false)) {
            plugin
                .getLogger()
                .warning(
                    "Failed instance world remains loaded and will not be deleted: " + worldName);
            return false;
          }
          worldOwners.remove(worldName);
          return true;
        },
        mainThread);
  }

  private void applyRules(World world, WorldRuleOptions rules) {
    world.setAutoSave(false);
    world.setPVP(rules.allowPvp());
    world.setGameRule(GameRule.DO_MOB_SPAWNING, rules.allowNaturalMobSpawning());
    world.setGameRule(GameRule.DO_WEATHER_CYCLE, rules.allowWeatherCycle());
    world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, rules.allowTimeCycle());
    world.setGameRule(GameRule.KEEP_INVENTORY, rules.keepInventory());
  }

  private String runtimeWorldName(UUID instanceId) {
    return configurations.current().settings().instances().worldsDirectory()
        + "/hz_"
        + instanceId.toString().replace("-", "");
  }

  private Path runtimeDirectory(String worldName) {
    Path root =
        worldContainer
            .resolve(configurations.current().settings().instances().worldsDirectory())
            .normalize();
    Path destination = worldContainer.resolve(worldName).normalize();
    if (!root.startsWith(worldContainer) || !destination.startsWith(root)) {
      throw new IllegalArgumentException("Runtime world escapes configured root");
    }
    return destination;
  }

  private static void copyTemplate(Path source, Path destination) {
    try {
      if (!Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(source)) {
        throw new IOException("Template directory is missing or unsafe: " + source);
      }
      if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
        throw new IOException("Runtime directory already exists: " + destination);
      }
      Files.walkFileTree(
          source,
          new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes)
                throws IOException {
              if (Files.isSymbolicLink(directory)) {
                throw new IOException("Symbolic links are forbidden in templates: " + directory);
              }
              Files.createDirectories(destination.resolve(source.relativize(directory)));
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
                throws IOException {
              if (Files.isSymbolicLink(file)) {
                throw new IOException("Symbolic links are forbidden in templates: " + file);
              }
              String name = file.getFileName().toString();
              if (!name.equals("uid.dat") && !name.equals("session.lock")) {
                Files.copy(
                    file,
                    destination.resolve(source.relativize(file)),
                    StandardCopyOption.COPY_ATTRIBUTES);
              }
              return FileVisitResult.CONTINUE;
            }
          });
    } catch (IOException failure) {
      throw new CompletionException(failure);
    }
  }

  private static void deleteDirectory(Path directory) {
    try {
      if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
        return;
      }
      Files.walkFileTree(
          directory,
          new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
                throws IOException {
              Files.delete(file);
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path current, IOException failure)
                throws IOException {
              if (failure != null) {
                throw failure;
              }
              Files.delete(current);
              return FileVisitResult.CONTINUE;
            }
          });
    } catch (IOException failure) {
      throw new CompletionException(failure);
    }
  }

  private static Throwable unwrap(Throwable failure) {
    return failure instanceof CompletionException && failure.getCause() != null
        ? failure.getCause()
        : failure;
  }
}

