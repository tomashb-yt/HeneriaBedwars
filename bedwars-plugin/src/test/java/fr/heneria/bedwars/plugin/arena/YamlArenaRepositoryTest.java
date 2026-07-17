package fr.heneria.bedwars.plugin.arena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.heneria.bedwars.core.arena.ArenaBedDefinition;
import fr.heneria.bedwars.core.arena.ArenaBlockPosition;
import fr.heneria.bedwars.core.arena.ArenaBoundary;
import fr.heneria.bedwars.core.arena.ArenaDefinition;
import fr.heneria.bedwars.core.arena.ArenaId;
import fr.heneria.bedwars.core.arena.ArenaLoadResult;
import fr.heneria.bedwars.core.arena.ArenaVector;
import fr.heneria.bedwars.core.arena.TeamId;
import fr.heneria.bedwars.plugin.config.SafeYamlWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.Optional;
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

  @Test
  void ticket005FileWithoutRevisionLoadsAtRevisionOne() throws Exception {
    Files.createDirectories(work.resolve("arenas"));
    Files.writeString(
        work.resolve("arenas/legacy.yml"),
        "config-version: 1\nid: legacy\ndisplay-name: Legacy\nstatus: DRAFT\nplayers:\n  minimum: 2\n  maximum: 8\nteams:\n  count: 2\n  players-per-team: 4\n",
        StandardCharsets.UTF_8);
    assertEquals(1, repository.loadAll().definitions().getFirst().revision());
  }

  @Test
  void partialBoundaryAndRevisionRoundTrip() throws Exception {
    ArenaDefinition draft = ArenaDefinition.draft(new ArenaId("alpha"), CLOCK.instant());
    ArenaDefinition changed =
        draft.withBoundary(
            Optional.of(
                new ArenaBoundary(false, Optional.of(new ArenaVector(1, 2, 3)), Optional.empty())),
            draft.status(),
            CLOCK.instant());
    repository.save(changed);
    ArenaDefinition loaded = repository.loadAll().definitions().getFirst();
    assertEquals(2, loaded.revision());
    assertTrue(loaded.boundary().orElseThrow().minimum().isPresent());
    assertTrue(loaded.boundary().orElseThrow().maximum().isEmpty());
  }

  @Test
  void completeTwoBlockBedRoundTripsThroughYaml() throws Exception {
    ArenaDefinition draft = ArenaDefinition.draft(new ArenaId("alpha"), CLOCK.instant());
    ArenaBedDefinition bed =
        new ArenaBedDefinition(
            new ArenaBlockPosition("hbw_template_alpha", 1, 64, 2),
            new ArenaBlockPosition("hbw_template_alpha", 2, 64, 2),
            "EAST");
    ArenaDefinition configured =
        draft.withUpdatedTeam(
            new TeamId("red"),
            team -> team.withBedDefinition(Optional.of(bed)),
            draft.status(),
            CLOCK.instant());

    repository.save(configured);

    ArenaDefinition loaded = repository.loadAll().definitions().getFirst();
    assertEquals(
        bed,
        loaded.teams().stream()
            .filter(team -> team.id().value().equals("red"))
            .findFirst()
            .orElseThrow()
            .bedDefinition()
            .orElseThrow());
  }
}
