package fr.heneria.bedwars.plugin.map;

import fr.heneria.bedwars.core.map.MapFileService;
import fr.heneria.bedwars.core.map.MapTemplate;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Confined world-folder copy, backup and deletion implementation.
 *
 * <p>Symbolic links are never followed. Duplication writes to a sibling temporary directory and
 * publishes by rename only after the complete copy succeeds.
 */
public final class SecureMapFileService implements MapFileService {
  private static final DateTimeFormatter BACKUP_DATE =
      DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneOffset.UTC);
  private final Path templateMarkers;
  private final Path metadataRoot;
  private final Path worldRoot;
  private final Path backupRoot;
  private final Set<String> excludedFiles;
  private final Set<String> excludedDirectories;
  private final Clock clock;

  public SecureMapFileService(
      Path templateMarkers,
      Path metadataRoot,
      Path worldRoot,
      Path backupRoot,
      Set<String> excludedFiles,
      Set<String> excludedDirectories,
      Clock clock) {
    this.templateMarkers = root(templateMarkers);
    this.metadataRoot = root(metadataRoot);
    this.worldRoot = root(worldRoot);
    this.backupRoot = root(backupRoot);
    this.excludedFiles = Set.copyOf(excludedFiles);
    this.excludedDirectories = Set.copyOf(excludedDirectories);
    this.clock = clock;
  }

  @Override
  public void createTemplateDirectory(MapTemplate template) throws IOException {
    Path marker = confined(templateMarkers, template.folderName());
    if (Files.exists(world(template)) || Files.exists(marker))
      throw new IOException("Managed map folder already exists");
    writeMarker(template, marker);
  }

  @Override
  public void duplicate(MapTemplate source, MapTemplate destination) throws IOException {
    Path from = world(source);
    if (!Files.isDirectory(from) || Files.isSymbolicLink(from))
      throw new IOException("Source world folder is missing or unsafe");
    Path target = world(destination);
    Path temporary = confined(worldRoot, "." + destination.worldName() + ".tmp");
    if (Files.exists(target) || Files.exists(temporary))
      throw new IOException("Destination world folder already exists");
    try {
      copyTree(from, temporary);
      moveDirectory(temporary, target);
      writeMarker(destination, confined(templateMarkers, destination.folderName()));
    } catch (IOException exception) {
      deleteTreeIfExists(temporary, worldRoot);
      deleteTreeIfExists(target, worldRoot);
      deleteTreeIfExists(confined(templateMarkers, destination.folderName()), templateMarkers);
      throw exception;
    }
  }

  @Override
  public void backup(MapTemplate template, String reason, String pluginVersion) throws IOException {
    Path source = world(template);
    if (!Files.isDirectory(source) || Files.isSymbolicLink(source))
      throw new IOException("World folder cannot be backed up");
    Path destination = uniqueBackup(template.id().value());
    Files.createDirectories(destination);
    copyTree(source, destination.resolve("world"));
    Path metadata = confined(metadataRoot, template.id().value() + ".yml");
    if (Files.isRegularFile(metadata))
      Files.copy(metadata, destination.resolve("metadata.yml"), StandardCopyOption.COPY_ATTRIBUTES);
    YamlConfiguration manifest = new YamlConfiguration();
    manifest.set("map-id", template.id().value());
    manifest.set("display-name", template.displayName());
    manifest.set("backup-reason", reason);
    manifest.set("created-at", clock.instant().toString());
    manifest.set("plugin-version", pluginVersion);
    Files.writeString(destination.resolve("manifest.yml"), manifest.saveToString());
  }

  @Override
  public void deleteTemplate(MapTemplate template) throws IOException {
    deleteTreeIfExists(world(template), worldRoot);
    deleteTreeIfExists(confined(templateMarkers, template.folderName()), templateMarkers);
  }

  @Override
  public boolean templateExists(MapTemplate template) {
    Path path = world(template);
    return Files.isDirectory(path) && !Files.isSymbolicLink(path);
  }

  private void copyTree(Path source, Path destination) throws IOException {
    Files.walkFileTree(
        source,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes)
              throws IOException {
            if (Files.isSymbolicLink(directory)) return FileVisitResult.SKIP_SUBTREE;
            Path relative = source.relativize(directory);
            if (!relative.toString().isEmpty()
                && excludedDirectories.contains(directory.getFileName().toString()))
              return FileVisitResult.SKIP_SUBTREE;
            Files.createDirectories(destination.resolve(relative));
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
              throws IOException {
            if (Files.isSymbolicLink(file) || excludedFiles.contains(file.getFileName().toString()))
              return FileVisitResult.CONTINUE;
            Files.copy(
                file,
                destination.resolve(source.relativize(file)),
                StandardCopyOption.COPY_ATTRIBUTES);
            return FileVisitResult.CONTINUE;
          }
        });
  }

  private static void deleteTreeIfExists(Path target, Path allowedRoot) throws IOException {
    Path normalized = target.toAbsolutePath().normalize();
    if (!normalized.startsWith(allowedRoot) || normalized.equals(allowedRoot))
      throw new IOException("Refusing deletion outside controlled root");
    if (!Files.exists(normalized, java.nio.file.LinkOption.NOFOLLOW_LINKS)) return;
    if (Files.isSymbolicLink(normalized)) throw new IOException("Refusing symbolic-link deletion");
    Files.walkFileTree(
        normalized,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
              throws IOException {
            if (Files.isSymbolicLink(file)) throw new IOException("Unsafe symbolic link in world");
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

  private Path uniqueBackup(String id) throws IOException {
    Files.createDirectories(backupRoot);
    String base = BACKUP_DATE.format(clock.instant());
    Path timestamp = confined(backupRoot, base);
    int suffix = 0;
    while (Files.exists(timestamp)) timestamp = confined(backupRoot, base + '_' + ++suffix);
    return timestamp.resolve(id);
  }

  private Path world(MapTemplate template) {
    return confined(worldRoot, template.worldName());
  }

  private static Path root(Path value) {
    return value.toAbsolutePath().normalize();
  }

  private static Path confined(Path root, String child) {
    Path candidate = root.resolve(child).normalize();
    if (!candidate.startsWith(root) || candidate.equals(root))
      throw new IllegalArgumentException("Unsafe managed map path");
    return candidate;
  }

  private static void moveDirectory(Path source, Path destination) throws IOException {
    try {
      Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException exception) {
      Files.move(source, destination);
    }
  }

  private static void writeMarker(MapTemplate template, Path marker) throws IOException {
    Files.createDirectories(marker);
    Files.writeString(marker.resolve("managed-world.txt"), template.worldName());
  }
}
