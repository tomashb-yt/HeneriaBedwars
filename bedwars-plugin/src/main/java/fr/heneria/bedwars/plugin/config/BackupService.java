package fr.heneria.bedwars.plugin.config;

import fr.heneria.bedwars.core.logging.ProjectLogger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/** Creates collision-safe backups before migrations or explicitly forced replacement. */
public final class BackupService {
  private static final DateTimeFormatter FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
  private final Path backupRoot;
  private final Clock clock;
  private final ProjectLogger logger;

  public BackupService(Path backupRoot, Clock clock, ProjectLogger logger) {
    this.backupRoot = Objects.requireNonNull(backupRoot, "backupRoot");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.logger = Objects.requireNonNull(logger, "logger");
  }

  public synchronized Path backup(Path source) throws IOException {
    String stamp = LocalDateTime.now(clock).format(FORMAT);
    Path directory = backupRoot.resolve(stamp);
    int collision = 0;
    while (Files.exists(directory.resolve(source.getFileName()))) {
      directory = backupRoot.resolve(stamp + "_" + ++collision);
    }
    Files.createDirectories(directory);
    Path destination = directory.resolve(source.getFileName());
    Files.copy(source, destination);
    logger.info("Configuration backup created: " + destination);
    return destination;
  }
}
