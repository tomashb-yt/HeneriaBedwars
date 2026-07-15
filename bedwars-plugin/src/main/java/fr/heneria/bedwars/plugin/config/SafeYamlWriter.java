package fr.heneria.bedwars.plugin.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/** Serializes writes and replaces YAML files atomically when the filesystem supports it. */
public final class SafeYamlWriter implements YamlWriter {
  @Override
  public synchronized void write(Path target, String yaml) throws IOException {
    Objects.requireNonNull(target, "target");
    Files.createDirectories(target.toAbsolutePath().getParent());
    Path temporary =
        Files.createTempFile(
            target.toAbsolutePath().getParent(), target.getFileName().toString(), ".tmp");
    boolean moved = false;
    try {
      Files.writeString(temporary, yaml, StandardCharsets.UTF_8);
      try {
        Files.move(
            temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException ignored) {
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
      }
      moved = true;
    } finally {
      if (!moved) Files.deleteIfExists(temporary);
    }
  }
}
