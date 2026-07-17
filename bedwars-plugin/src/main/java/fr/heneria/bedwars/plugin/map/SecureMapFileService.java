package fr.heneria.bedwars.plugin.map;

import fr.heneria.bedwars.core.map.MapFileService;
import fr.heneria.bedwars.core.map.MapTemplate;
import java.io.IOException;
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
  private static final String IMPORT_DIRECTORY = "import";
  private static final String IMPORT_INSTRUCTIONS = "LISEZ-MOI.txt";
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
  public void ensureImportDirectory(MapTemplate template) throws IOException {
    Path marker = confined(templateMarkers, template.folderName());
    if (Files.isSymbolicLink(marker)) throw new IOException("Unsafe map marker folder");
    Files.createDirectories(marker);
    Path imports = confined(marker, IMPORT_DIRECTORY);
    if (Files.isSymbolicLink(imports)) throw new IOException("Unsafe map import folder");
    Files.createDirectories(imports);
    Path instructions = imports.resolve(IMPORT_INSTRUCTIONS);
    if (!Files.exists(instructions))
      Files.writeString(
          instructions,
          "REMPLACER CETTE CARTE PAR UN MONDE BEDWARS\n\n"
              + "1. Fermez le monde depuis le menu HeneriaBedWars.\n"
              + "2. Copiez ICI le contenu du dossier de votre monde.\n"
              + "3. Le fichier level.dat doit se trouver directement dans ce dossier.\n"
              + "4. Dans le menu de la carte, cliquez sur Importer / remplacer.\n\n"
              + "Le plugin sauvegarde l'ancienne carte avant le remplacement.\n"
              + "Ne placez aucun raccourci ou lien symbolique dans ce dossier.\n");
  }

  @Override
  public boolean importReady(MapTemplate template) {
    Path imports = importDirectory(template);
    return Files.isDirectory(imports)
        && !Files.isSymbolicLink(imports)
        && Files.isRegularFile(imports.resolve("level.dat"));
  }

  @Override
  public void replaceFromImport(MapTemplate template) throws IOException {
    Path source = importDirectory(template);
    if (!importReady(template))
      throw new IOException("Import folder must contain level.dat at its root");
    rejectSymbolicLinks(source);
    Path target = world(template);
    Path incoming = confined(worldRoot, "." + template.worldName() + ".import.tmp");
    Path previous = confined(worldRoot, "." + template.worldName() + ".previous.tmp");
    if (Files.exists(incoming) || Files.exists(previous))
      throw new IOException("A previous map import requires manual inspection");
    try {
      copyTree(source, incoming);
      if (Files.exists(target)) moveDirectory(target, previous);
      moveDirectory(incoming, target);
      deleteTreeIfExists(previous, worldRoot);
    } catch (IOException exception) {
      try {
        deleteTreeIfExists(incoming, worldRoot);
      } catch (IOException cleanup) {
        exception.addSuppressed(cleanup);
      }
      try {
        if (Files.exists(previous)) {
          deleteTreeIfExists(target, worldRoot);
          moveDirectory(previous, target);
        }
      } catch (IOException rollback) {
        exception.addSuppressed(rollback);
      }
      throw exception;
    }
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
            if (Files.isSymbolicLink(file)
                || excludedFiles.contains(file.getFileName().toString())
                || IMPORT_INSTRUCTIONS.equals(file.getFileName().toString()))
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

  private Path importDirectory(MapTemplate template) {
    return confined(confined(templateMarkers, template.folderName()), IMPORT_DIRECTORY);
  }

  private static void rejectSymbolicLinks(Path source) throws IOException {
    Files.walkFileTree(
        source,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes)
              throws IOException {
            if (Files.isSymbolicLink(directory))
              throw new IOException("Unsafe symbolic link in map import");
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
              throws IOException {
            if (Files.isSymbolicLink(file))
              throw new IOException("Unsafe symbolic link in map import");
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
      throw new IllegalArgumentException("Unsafe managed map path");
    return candidate;
  }

  private static void moveDirectory(Path source, Path destination) throws IOException {
    try {
      Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException atomicFailure) {
      if (!Files.exists(source) || Files.exists(destination)) throw atomicFailure;
      try {
        Files.move(source, destination);
      } catch (IOException fallbackFailure) {
        fallbackFailure.addSuppressed(atomicFailure);
        throw fallbackFailure;
      }
    }
  }

  private void writeMarker(MapTemplate template, Path marker) throws IOException {
    Files.createDirectories(marker);
    Files.writeString(marker.resolve("managed-world.txt"), template.worldName());
    ensureImportDirectory(template);
  }
}
