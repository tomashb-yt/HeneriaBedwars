package fr.heneria.bedwars.core.arena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ArenaFrameworkTest {
  private static final Instant NOW = Instant.parse("2026-07-16T12:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Test
  void arenaIdsAreNormalizedAndFilenameSafe() {
    assertEquals("my_arena-1", ArenaId.parse("MY_ARENA-1").value());
    assertThrows(IllegalArgumentException.class, () -> ArenaId.parse("../arena"));
    assertThrows(IllegalArgumentException.class, () -> ArenaId.parse("a"));
  }

  @Test
  void draftIsImmutableAndStartsDisabled() {
    ArenaDefinition draft = ArenaDefinition.draft(new ArenaId("alpha"), NOW);
    assertEquals(ArenaStatus.DRAFT, draft.status());
    assertFalse(draft.enabled());
    assertTrue(draft.worldName().isEmpty());
    assertThrows(
        UnsupportedOperationException.class, () -> draft.metadata().attributes().put("x", "y"));
  }

  @Test
  void validatorReportsMissingRequiredFieldsAndSpectatorWarning() {
    ArenaValidationResult result =
        new ArenaValidator(world -> true)
            .validate(ArenaDefinition.draft(new ArenaId("alpha"), NOW));
    assertFalse(result.valid());
    assertTrue(
        result.problems().stream().anyMatch(problem -> problem.code().equals("missing-world")));
    assertTrue(
        result.problems().stream().anyMatch(problem -> problem.code().equals("missing-waiting")));
    assertEquals(1, result.warnings());
  }

  @Test
  void validatorAcceptsCompleteCoherentArena() {
    ArenaDefinition arena = complete("alpha");
    ArenaValidationResult result =
        new ArenaValidator(world -> world.equals("world")).validate(arena);
    assertTrue(result.valid());
    assertEquals(0, result.problems().size());
  }

  @Test
  void validatorRejectsUnknownWorldAndCapacityMismatch() {
    ArenaDefinition source = complete("alpha");
    ArenaDefinition arena =
        source.edited(
            ArenaStatus.READY,
            source.worldName(),
            2,
            7,
            2,
            4,
            source.waitingLocation(),
            source.spectatorLocation(),
            NOW);
    ArenaValidationResult result = new ArenaValidator(world -> false).validate(arena);
    assertTrue(
        result.problems().stream().anyMatch(problem -> problem.code().equals("unknown-world")));
    assertTrue(
        result.problems().stream().anyMatch(problem -> problem.code().equals("capacity-mismatch")));
  }

  @Test
  void registryReplacesItsSnapshotAtomically() {
    ArenaRegistry registry = new ArenaRegistry();
    registry.replace(List.of(complete("beta"), complete("alpha")));
    assertEquals(
        List.of("alpha", "beta"),
        registry.all().stream().map(arena -> arena.id().value()).toList());
    assertThrows(
        IllegalArgumentException.class,
        () -> registry.replace(List.of(complete("alpha"), complete("alpha"))));
    assertEquals(2, registry.size());
  }

  @Test
  void createPersistsBeforePublishing() {
    FakeRepository repository = new FakeRepository();
    ArenaService service = service(repository);
    assertTrue(service.create("alpha").successful());
    assertEquals(1, repository.saved.size());
    assertEquals(1, service.list().size());
  }

  @Test
  void failedSaveLeavesMemoryUntouched() {
    FakeRepository repository = new FakeRepository();
    ArenaService service = service(repository);
    service.create("alpha");
    repository.failSave = true;
    assertEquals(ArenaOperationCode.STORAGE_FAILED, service.setWorld("alpha", "world").code());
    assertTrue(service.find("alpha").orElseThrow().worldName().isEmpty());
  }

  @Test
  void enableRequiresAValidArena() {
    FakeRepository repository = new FakeRepository();
    ArenaService service = service(repository);
    service.create("alpha");
    assertEquals(ArenaOperationCode.VALIDATION_FAILED, service.enable("alpha").code());
  }

  @Test
  void teamsRecalculateMaximumAndKeepCoherentMinimum() {
    FakeRepository repository = new FakeRepository();
    ArenaService service = service(repository);
    service.create("alpha");
    service.setPlayers("alpha", 12, 16);
    ArenaDefinition changed = service.setTeams("alpha", 2, 4).arena().orElseThrow();
    assertEquals(8, changed.maximumPlayers());
    assertEquals(8, changed.minimumPlayers());
  }

  @Test
  void deleteMustSucceedInRepositoryBeforeRegistryChanges() {
    FakeRepository repository = new FakeRepository();
    ArenaService service = service(repository);
    service.create("alpha");
    repository.failDelete = true;
    assertEquals(ArenaOperationCode.STORAGE_FAILED, service.delete("alpha").code());
    assertTrue(service.find("alpha").isPresent());
  }

  @Test
  void reloadPreservesOnlyKnownFailedOldDefinitions() throws Exception {
    FakeRepository repository = new FakeRepository();
    ArenaService service = service(repository);
    service.create("alpha");
    repository.load =
        new ArenaLoadResult(
            List.of(complete("beta")),
            Map.of(new ArenaId("alpha"), "broken"),
            List.of("unsafe.yml"));
    ArenaReloadResult result = service.reload();
    assertEquals(1, result.preserved());
    assertTrue(service.find("alpha").isPresent());
    assertTrue(service.find("beta").isPresent());
    assertEquals(2, result.failures().size());
  }

  private static ArenaService service(FakeRepository repository) {
    return new ArenaService(
        repository, new ArenaRegistry(), new ArenaValidator(world -> world.equals("world")), CLOCK);
  }

  private static ArenaDefinition complete(String id) {
    ArenaLocation waiting = new ArenaLocation("world", new ArenaVector(0, 64, 0), 0, 0);
    ArenaLocation spectator = new ArenaLocation("world", new ArenaVector(0, 70, 0), 0, 0);
    return ArenaDefinition.draft(new ArenaId(id), NOW)
        .edited(
            ArenaStatus.READY,
            Optional.of("world"),
            2,
            8,
            2,
            4,
            Optional.of(waiting),
            Optional.of(spectator),
            NOW);
  }

  private static final class FakeRepository implements ArenaRepository {
    private final List<ArenaDefinition> saved = new ArrayList<>();
    private ArenaLoadResult load = new ArenaLoadResult(List.of(), Map.of(), List.of());
    private boolean failSave;
    private boolean failDelete;

    @Override
    public ArenaLoadResult loadAll() {
      return load;
    }

    @Override
    public void save(ArenaDefinition arena) throws IOException {
      if (failSave) throw new IOException("simulated");
      saved.add(arena);
    }

    @Override
    public void deleteWithBackup(ArenaId id) throws IOException {
      if (failDelete) throw new IOException("simulated");
    }
  }
}
