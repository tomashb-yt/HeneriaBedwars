package fr.heneria.bedwars.plugin.arena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.heneria.bedwars.core.arena.ArenaDefinition;
import fr.heneria.bedwars.core.arena.ArenaId;
import fr.heneria.bedwars.core.arena.ArenaLoadResult;
import fr.heneria.bedwars.plugin.config.SafeYamlWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class YamlArenaRepositoryTest {
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-07-16T12:00:00Z"), ZoneOffset.UTC);
  private Path work;
  private YamlArenaRepository repository;

  @BeforeEach
  void setUp() throws Exception {
    work = Path.of("build", "test-work", "arena-repository-" + System.nanoTime());
    Files.createDirectories(work);
    repository =
        new YamlArenaRepository(
            work.resolve("arenas"), work.resolve("backups/arenas"), CLOCK, new SafeYamlWriter());
  }

  @AfterEach
  void clean() throws Exception {
    if (!Files.exists(work)) return;
    try (var files = Files.walk(work)) {
      for (Path path : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
    }
  }

  @Test
  void saveAndLoadRoundTripUsesOneUtf8File() throws Exception {
    ArenaDefinition draft = ArenaDefinition.draft(new ArenaId("alpha"), CLOCK.instant());
    repository.save(draft);
    ArenaLoadResult loaded = repository.loadAll();
    assertEquals(1, loaded.definitions().size());
    assertEquals(draft, loaded.definitions().getFirst());
    assertTrue(
        Files.readString(work.resolve("arenas/alpha.yml"), StandardCharsets.UTF_8)
            .contains("config-version: 1"));
    try (var files = Files.list(work.resolve("arenas"))) {
      assertFalse(files.anyMatch(path -> path.getFileName().toString().endsWith(".tmp")));
    }
  }

  @Test
  void brokenKnownFileIsReportedWithoutBlockingValidFiles() throws Exception {
    repository.save(ArenaDefinition.draft(new ArenaId("alpha"), CLOCK.instant()));
    Files.writeString(work.resolve("arenas/broken.yml"), "id: [", StandardCharsets.UTF_8);
    ArenaLoadResult loaded = repository.loadAll();
    assertEquals(1, loaded.definitions().size());
    assertTrue(loaded.failedKnownIds().containsKey(new ArenaId("broken")));
  }

  @Test
  void unsafeFilenameCannotEscapeArenaDirectory() throws Exception {
    Files.createDirectories(work.resolve("arenas"));
    Files.writeString(work.resolve("arenas/BAD.yml"), "id: BAD", StandardCharsets.UTF_8);
    ArenaLoadResult loaded = repository.loadAll();
    assertEquals(1, loaded.unreadableFiles().size());
    assertTrue(loaded.definitions().isEmpty());
  }

  @Test
  void deletionCreatesDatedBackupFirst() throws Exception {
    repository.save(ArenaDefinition.draft(new ArenaId("alpha"), CLOCK.instant()));
    repository.deleteWithBackup(new ArenaId("alpha"));
    assertFalse(Files.exists(work.resolve("arenas/alpha.yml")));
    assertTrue(Files.isRegularFile(work.resolve("backups/arenas/2026-07-16/alpha.yml")));
  }
}
