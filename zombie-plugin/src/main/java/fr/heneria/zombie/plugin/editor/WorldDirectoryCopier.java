package fr.heneria.zombie.plugin.editor;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;

/** Secure filesystem helper for off-thread world snapshots and atomic directory replacement. */
final class WorldDirectoryCopier {
  private WorldDirectoryCopier() {}

  static void replace(Path source, Path target) {
    Path normalizedSource = source.toAbsolutePath().normalize();
    Path normalizedTarget = target.toAbsolutePath().normalize();
    Path parent = normalizedTarget.getParent();
    if (parent == null
        || normalizedSource.equals(normalizedTarget)
        || !Files.isDirectory(normalizedSource, LinkOption.NOFOLLOW_LINKS)
        || Files.isSymbolicLink(normalizedSource)) {
      throw new IllegalArgumentException("Invalid world copy paths");
    }
    Path temporary =
        parent.resolve("." + normalizedTarget.getFileName() + ".tmp-" + UUID.randomUUID());
    Path backup = parent.resolve("." + normalizedTarget.getFileName() + ".previous");
    try {
      Files.createDirectories(parent);
      copyTree(normalizedSource, temporary);
      deleteTree(backup, parent);
      if (Files.exists(normalizedTarget, LinkOption.NOFOLLOW_LINKS)) {
        move(normalizedTarget, backup);
      }
      try {
        move(temporary, normalizedTarget);
      } catch (IOException failure) {
        if (Files.exists(backup, LinkOption.NOFOLLOW_LINKS)
            && !Files.exists(normalizedTarget, LinkOption.NOFOLLOW_LINKS)) {
          move(backup, normalizedTarget);
        }
        throw failure;
      }
      deleteTree(backup, parent);
    } catch (IOException failure) {
      try {
        deleteTree(temporary, parent);
      } catch (IOException cleanupFailure) {
        failure.addSuppressed(cleanupFailure);
      }
      throw new java.util.concurrent.CompletionException(failure);
    }
  }

  private static void copyTree(Path source, Path destination) throws IOException {
    Files.walkFileTree(
        source,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes)
              throws IOException {
            if (Files.isSymbolicLink(directory)) {
              throw new IOException("Symbolic links are forbidden in world snapshots");
            }
            Files.createDirectories(destination.resolve(source.relativize(directory)));
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
              throws IOException {
            if (Files.isSymbolicLink(file)) {
              throw new IOException("Symbolic links are forbidden in world snapshots");
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
  }

  private static void deleteTree(Path directory, Path allowedParent) throws IOException {
    Path normalized = directory.toAbsolutePath().normalize();
    if (!normalized.startsWith(allowedParent.toAbsolutePath().normalize())
        || normalized.equals(allowedParent.toAbsolutePath().normalize())
        || !Files.exists(normalized, LinkOption.NOFOLLOW_LINKS)) {
      return;
    }
    Files.walkFileTree(
        normalized,
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
  }

  private static void move(Path source, Path target) throws IOException {
    try {
      Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
    } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
      Files.move(source, target);
    }
  }
}
