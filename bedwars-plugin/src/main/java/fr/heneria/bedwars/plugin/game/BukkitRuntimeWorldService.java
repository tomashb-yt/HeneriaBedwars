package fr.heneria.bedwars.plugin.game;

import fr.heneria.bedwars.core.config.WorldManagerSettings;
import fr.heneria.bedwars.core.game.RuntimeArena;
import fr.heneria.bedwars.core.game.RuntimeWorldHandle;
import fr.heneria.bedwars.core.game.RuntimeWorldService;
import fr.heneria.bedwars.core.logging.ProjectLogger;
import fr.heneria.bedwars.core.map.MapTemplate;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Secure Paper/Spigot adapter for disposable game worlds. */
public final class BukkitRuntimeWorldService implements RuntimeWorldService {
  private final JavaPlugin plugin;
  private final WorldManagerSettings settings;
  private final Path instanceRoot;
  private final Path worldRoot;
  private final ProjectLogger logger;
  private volatile CompletionStage<Void> readiness = CompletableFuture.completedFuture(null);

  public BukkitRuntimeWorldService(
      JavaPlugin plugin,
      WorldManagerSettings settings,
      Path instanceRoot,
      Path worldRoot,
      ProjectLogger logger) {
    this.plugin = plugin;
    this.settings = settings;
    this.instanceRoot = root(instanceRoot);
    this.worldRoot = root(worldRoot);
    this.logger = logger;
  }

  @Override
  public CompletionStage<RuntimeWorldHandle> create(RuntimeArena arena) {
    return readiness.thenCompose(ignored -> createReady(arena));
  }

  private CompletionStage<RuntimeWorldHandle> createReady(RuntimeArena arena) {
    if (!Bukkit.isPrimaryThread()) {
      CompletableFuture<RuntimeWorldHandle> scheduled = new CompletableFuture<>();
      runMain(
          () ->
              createReady(arena)
                  .whenComplete(
                      (world, failure) -> {
                        if (failure == null) scheduled.complete(world);
                        else scheduled.completeExceptionally(failure);
                      }));
      return scheduled;
    }
    CompletableFuture<RuntimeWorldHandle> result = new CompletableFuture<>();
    String suffix = arena.gameId().value().toString().replace("-", "");
    String worldName = settings.instanceWorldPrefix() + suffix;
    Path source = confined(worldRoot, arena.template().worldName());
    Path target = confined(worldRoot, worldName);
    Path temporary = confined(worldRoot, "." + worldName + ".tmp");
    Path instance = confined(instanceRoot, "game-" + arena.gameId());
    if (Bukkit.isPrimaryThread()) {
      World templateWorld = Bukkit.getWorld(arena.template().worldName());
      if (templateWorld != null) templateWorld.save();
    }
    plugin
        .getServer()
        .getScheduler()
        .runTaskAsynchronously(
            plugin,
            () -> {
              try {
                if (!Files.isDirectory(source) || Files.isSymbolicLink(source))
                  throw new IOException("Template world folder is missing or unsafe");
                if (Files.exists(target) || Files.exists(temporary) || Files.exists(instance))
                  throw new IOException("Runtime destination already exists");
                copyTree(source, temporary);
                Files.move(temporary, target);
                Files.createDirectories(instance.resolve("world"));
                Files.writeString(instance.resolve("world/active-world.txt"), worldName);
                Files.writeString(instance.resolve("arena.txt"), arena.definition().id().value());
                runMain(() -> loadWorld(arena.template(), worldName, instance, result));
              } catch (Exception exception) {
                cleanupQuietly(temporary, worldRoot);
                cleanupQuietly(target, worldRoot);
                cleanupQuietly(instance, instanceRoot);
                result.completeExceptionally(exception);
              }
            });
    return result;
  }

  @Override
  public CompletionStage<Void> destroy(RuntimeWorldHandle handle) {
    CompletableFuture<Void> result = new CompletableFuture<>();
    runMain(
        () -> {
          try {
            World world = Bukkit.getWorld(handle.worldName());
            if (world != null) {
              World fallback = Bukkit.getWorld(settings.fallbackWorld());
              if (fallback == null || fallback.equals(world))
                throw new IOException("Fallback world is missing or unsafe");
              Location destination = fallback.getSpawnLocation();
              for (Player player : java.util.List.copyOf(world.getPlayers()))
                if (!player.teleport(destination))
                  throw new IOException("Player evacuation failed");
              if (!Bukkit.unloadWorld(world, false))
                throw new IOException("Bukkit refused to unload runtime world");
            }
            plugin
                .getServer()
                .getScheduler()
                .runTaskAsynchronously(
                    plugin,
                    () -> {
                      try {
                        deleteTree(confined(worldRoot, handle.worldName()), worldRoot);
                        deleteTree(confined(instanceRoot, handle.storageKey()), instanceRoot);
                        result.complete(null);
                      } catch (Exception exception) {
                        result.completeExceptionally(exception);
                      }
                    });
          } catch (Exception exception) {
            result.completeExceptionally(exception);
          }
        });
    return result;
  }

  /** Deletes leftovers from a previous crash before accepting new games. */
  public CompletionStage<Void> cleanupOrphans() {
    CompletableFuture<Void> result = new CompletableFuture<>();
    plugin
        .getServer()
        .getScheduler()
        .runTaskAsynchronously(
            plugin,
            () -> {
              try {
                if (Files.isDirectory(instanceRoot)) {
                  try (var stream = Files.list(instanceRoot)) {
                    for (Path child : stream.toList())
                      if (child.getFileName().toString().startsWith("game-"))
                        deleteTree(child, instanceRoot);
                  }
                }
                if (Files.isDirectory(worldRoot)) {
                  try (var stream = Files.list(worldRoot)) {
                    for (Path child : stream.toList())
                      if (child.getFileName().toString().startsWith(settings.instanceWorldPrefix()))
                        deleteTree(child, worldRoot);
                  }
                }
                result.complete(null);
              } catch (Exception exception) {
                logger.warning("[Games] Orphan cleanup failed: " + exception.getMessage());
                result.completeExceptionally(exception);
              }
            });
    return result;
  }

  public void prepare() {
    if (!Bukkit.isPrimaryThread())
      throw new IllegalStateException("Runtime cleanup preparation must run on the server thread");
    for (World world : java.util.List.copyOf(Bukkit.getWorlds())) {
      if (!world.getName().startsWith(settings.instanceWorldPrefix())) continue;
      World fallback = Bukkit.getWorld(settings.fallbackWorld());
      if (fallback == null || fallback.equals(world))
        throw new IllegalStateException(
            "Cannot recover runtime world without a safe fallback: " + world.getName());
      for (Player player : java.util.List.copyOf(world.getPlayers()))
        if (!player.teleport(fallback.getSpawnLocation()))
          throw new IllegalStateException(
              "Cannot evacuate player from runtime world: " + world.getName());
      if (!Bukkit.unloadWorld(world, false))
        throw new IllegalStateException(
            "Bukkit refused to unload orphan runtime world: " + world.getName());
    }
    readiness = cleanupOrphans();
  }

  private void loadWorld(
      MapTemplate template,
      String worldName,
      Path instance,
      CompletableFuture<RuntimeWorldHandle> result) {
    try {
      World world =
          new WorldCreator(worldName)
              .environment(
                  World.Environment.valueOf(template.environment().toUpperCase(Locale.ROOT)))
              .generateStructures(false)
              .createWorld();
      if (world == null) throw new IOException("Bukkit could not load cloned runtime world");
      applySettings(world, template);
      result.complete(
          new RuntimeWorldHandle(arenaId(instance), worldName, instance.getFileName().toString()));
    } catch (Exception exception) {
      World loaded = Bukkit.getWorld(worldName);
      if (loaded != null) Bukkit.unloadWorld(loaded, false);
      plugin
          .getServer()
          .getScheduler()
          .runTaskAsynchronously(
              plugin,
              () -> {
                cleanupQuietly(confined(worldRoot, worldName), worldRoot);
                cleanupQuietly(instance, instanceRoot);
              });
      result.completeExceptionally(exception);
    }
  }

  private static fr.heneria.bedwars.core.game.GameId arenaId(Path instance) {
    String raw = instance.getFileName().toString().substring("game-".length());
    return fr.heneria.bedwars.core.game.GameId.parse(raw);
  }

  private void applySettings(World world, MapTemplate template) {
    world.setDifficulty(Difficulty.valueOf(template.settings().difficulty()));
    world.setPVP(template.settings().pvp());
    world.setSpawnFlags(false, false);
    world.setAutoSave(false);
    world.setTime(template.settings().fixedTime());
    world.setStorm(!template.settings().clearWeather());
    rule(world, "doDaylightCycle", template.settings().daylightCycle());
    rule(world, "doWeatherCycle", template.settings().weatherCycle());
    rule(world, "doMobSpawning", false);
    rule(world, "doFireTick", template.settings().fireTick());
    rule(world, "mobGriefing", false);
    rule(world, "keepInventory", true);
    rule(world, "announceAdvancements", false);
    rule(world, "showDeathMessages", false);
    rule(world, "doImmediateRespawn", true);
  }

  private void copyTree(Path source, Path destination) throws IOException {
    Set<String> excludedFiles = settings.excludedFiles();
    Set<String> excludedDirectories = settings.excludedDirectories();
    Files.walkFileTree(
        source,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes)
              throws IOException {
            if (Files.isSymbolicLink(directory)) return FileVisitResult.SKIP_SUBTREE;
            if (!directory.equals(source)
                && excludedDirectories.contains(directory.getFileName().toString()))
              return FileVisitResult.SKIP_SUBTREE;
            Files.createDirectories(destination.resolve(source.relativize(directory)));
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
              throws IOException {
            if (!Files.isSymbolicLink(file)
                && !excludedFiles.contains(file.getFileName().toString()))
              Files.copy(
                  file,
                  destination.resolve(source.relativize(file)),
                  StandardCopyOption.COPY_ATTRIBUTES);
            return FileVisitResult.CONTINUE;
          }
        });
  }

  private static void deleteTree(Path target, Path allowedRoot) throws IOException {
    Path normalized = target.toAbsolutePath().normalize();
    if (!normalized.startsWith(allowedRoot) || normalized.equals(allowedRoot))
      throw new IOException("Refusing runtime deletion outside controlled root");
    if (!Files.exists(normalized, LinkOption.NOFOLLOW_LINKS)) return;
    if (Files.isSymbolicLink(normalized)) throw new IOException("Refusing symbolic-link deletion");
    Files.walkFileTree(
        normalized,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
              throws IOException {
            if (Files.isSymbolicLink(file)) throw new IOException("Unsafe runtime symbolic link");
            Files.delete(file);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory(Path directory, IOException exception)
              throws IOException {
            if (exception != null) throw exception;
            Files.delete(directory);
            return FileVisitResult.CONTINUE;
          }
        });
  }

  private static Path root(Path value) {
    return value.toAbsolutePath().normalize();
  }

  private static Path confined(Path root, String child) {
    Path candidate = root.resolve(child).normalize();
    if (!candidate.startsWith(root) || candidate.equals(root))
      throw new IllegalArgumentException("Unsafe runtime path");
    return candidate;
  }

  private void cleanupQuietly(Path target, Path root) {
    try {
      deleteTree(target, root);
    } catch (IOException exception) {
      logger.warning("[Games] Runtime rollback cleanup failed: " + exception.getMessage());
    }
  }

  private void runMain(Runnable action) {
    if (Bukkit.isPrimaryThread()) action.run();
    else plugin.getServer().getScheduler().runTask(plugin, action);
  }

  @SuppressWarnings({"rawtypes", "unchecked", "deprecation"})
  private static void rule(World world, String name, Object value) {
    GameRule rule = GameRule.getByName(name);
    if (rule != null) world.setGameRule(rule, value);
  }
}
