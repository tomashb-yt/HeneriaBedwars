package fr.heneria.bedwars.core.arena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.heneria.bedwars.core.game.generator.GeneratorId;
import fr.heneria.bedwars.core.game.generator.GeneratorResource;
import fr.heneria.bedwars.core.game.generator.GeneratorStackingStrategy;
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
  void teamSetupUpdatesOnlyTheTargetAndRejectsAReusedBedBlock() {
    FakeRepository repository = new FakeRepository();
    ArenaService service = service(repository);
    ArenaDefinition created = service.create("alpha").arena().orElseThrow();
    TeamId red = created.teams().get(0).id();
    TeamId blue = created.teams().get(1).id();
    ArenaLocation bed = new ArenaLocation("world", new ArenaVector(10, 64, 10), 0, 0);
    ArenaDefinition withBed =
        service.setTeamBed("alpha", red, bed, created.revision()).arena().orElseThrow();
    ArenaLocation spawn = new ArenaLocation("world", new ArenaVector(20, 65, 20), 90, 0);
    ArenaDefinition withSpawn =
        service.setTeamSpawn("alpha", red, spawn, withBed.revision()).arena().orElseThrow();

    assertEquals(bed, withSpawn.teams().get(0).bedLocation().orElseThrow());
    assertEquals(spawn, withSpawn.teams().get(0).spawn().orElseThrow());
    assertTrue(withSpawn.teams().get(1).bedLocation().isEmpty());

    ArenaLocation shop = new ArenaLocation("world", new ArenaVector(15, 65, 15), 180, 0);
    ArenaDefinition withShop =
        service.setTeamShop("alpha", red, shop, withSpawn.revision()).arena().orElseThrow();
    assertEquals(shop, withShop.teams().get(0).shopLocation().orElseThrow());
    ArenaDefinition withoutShop =
        service.clearTeamShop("alpha", red, withShop.revision()).arena().orElseThrow();
    assertTrue(withoutShop.teams().get(0).shopLocation().isEmpty());

    ArenaLocation upgradeShop = new ArenaLocation("world", new ArenaVector(14, 65, 14), 180, 0);
    ArenaDefinition withUpgradeShop =
        service
            .setTeamUpgradeShop("alpha", red, upgradeShop, withoutShop.revision())
            .arena()
            .orElseThrow();
    assertEquals(upgradeShop, withUpgradeShop.teams().get(0).upgradeShopLocation().orElseThrow());
    ArenaDefinition withoutUpgradeShop =
        service
            .clearTeamUpgradeShop("alpha", red, withUpgradeShop.revision())
            .arena()
            .orElseThrow();
    assertTrue(withoutUpgradeShop.teams().get(0).upgradeShopLocation().isEmpty());

    ArenaOperationResult duplicate =
        service.setTeamBed("alpha", blue, bed, withoutUpgradeShop.revision());
    assertEquals(ArenaOperationCode.INVALID_ARGUMENT, duplicate.code());
    assertTrue(service.find("alpha").orElseThrow().teams().get(1).bedLocation().isEmpty());
  }

  @Test
  void generatorsAreAddedMovedAndRemovedWithOptimisticRevisions() {
    FakeRepository repository = new FakeRepository();
    ArenaService service = service(repository);
    ArenaDefinition created = service.create("alpha").arena().orElseThrow();
    ArenaGeneratorDefinition iron =
        generator("iron-1", GeneratorResource.IRON, new ArenaVector(1, 64, 1));

    ArenaDefinition added =
        service.addGenerator("alpha", iron, created.revision()).arena().orElseThrow();
    assertEquals(iron, added.generators().getFirst());

    ArenaLocation movedLocation = new ArenaLocation("world", new ArenaVector(5, 65, 5), 0, 0);
    ArenaDefinition moved =
        service
            .moveGenerator("alpha", iron.id(), movedLocation, added.revision())
            .arena()
            .orElseThrow();
    assertEquals(movedLocation, moved.generators().getFirst().location());

    ArenaOperationResult stale =
        service.addGenerator(
            "alpha",
            generator("gold-1", GeneratorResource.GOLD, new ArenaVector(8, 64, 8)),
            added.revision());
    assertEquals(ArenaOperationCode.CONFLICT, stale.code());

    ArenaDefinition removed =
        service.removeGenerator("alpha", iron.id(), moved.revision()).arena().orElseThrow();
    assertTrue(removed.generators().isEmpty());
  }

  @Test
  void generatorsShareBlocksAcrossResourcesButRejectExactResourceDuplicates() {
    FakeRepository repository = new FakeRepository();
    ArenaService service = service(repository);
    ArenaDefinition created = service.create("alpha").arena().orElseThrow();
    ArenaGeneratorDefinition iron =
        generator("iron-1", GeneratorResource.IRON, new ArenaVector(1.2, 64, 1.8));
    ArenaDefinition added =
        service.addGenerator("alpha", iron, created.revision()).arena().orElseThrow();

    ArenaDefinition combined =
        service
            .addGenerator(
                "alpha",
                generator("gold-1", GeneratorResource.GOLD, new ArenaVector(1.9, 64, 1.1)),
                added.revision())
            .arena()
            .orElseThrow();
    assertEquals(2, combined.generators().size());

    ArenaOperationResult duplicate =
        service.addGenerator(
            "alpha",
            generator("iron-2", GeneratorResource.IRON, new ArenaVector(1.1, 64, 1.9)),
            combined.revision());
    assertEquals(ArenaOperationCode.INVALID_ARGUMENT, duplicate.code());
    assertEquals(1000, iron.runtime().interval().toMillis());
    assertEquals(iron.id(), iron.runtime().id());
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

  @Test
  void revisionsStartAtOneAndIncreaseAfterEverySuccessfulSave() {
    FakeRepository repository = new FakeRepository();
    ArenaService service = service(repository);
    ArenaDefinition created = service.create("alpha").arena().orElseThrow();
    assertEquals(1, created.revision());
    ArenaDefinition changed =
        service.setWorld("alpha", "world", created.revision()).arena().orElseThrow();
    assertEquals(2, changed.revision());
  }

  @Test
  void staleRevisionIsRejectedWithoutSaving() {
    FakeRepository repository = new FakeRepository();
    ArenaService service = service(repository);
    ArenaDefinition created = service.create("alpha").arena().orElseThrow();
    service.setWorld("alpha", "world", created.revision());
    int saves = repository.saved.size();
    ArenaOperationResult conflict = service.setDisplayName("alpha", "Stale", created.revision());
    assertEquals(ArenaOperationCode.CONFLICT, conflict.code());
    assertEquals(saves, repository.saved.size());
    assertEquals("alpha", service.find("alpha").orElseThrow().displayName());
  }

  @Test
  void currentRevisionAllowsDisplayNameChange() {
    FakeRepository repository = new FakeRepository();
    ArenaService service = service(repository);
    ArenaDefinition created = service.create("alpha").arena().orElseThrow();
    ArenaOperationResult result =
        service.setDisplayName("alpha", "<green>Alpha", created.revision());
    assertTrue(result.successful());
    assertEquals("<green>Alpha", result.arena().orElseThrow().displayName());
  }

  @Test
  void failedSaveDoesNotPublishOrIncrementRevision() {
    FakeRepository repository = new FakeRepository();
    ArenaService service = service(repository);
    ArenaDefinition created = service.create("alpha").arena().orElseThrow();
    repository.failSave = true;
    assertEquals(
        ArenaOperationCode.STORAGE_FAILED,
        service.setDisplayName("alpha", "New", created.revision()).code());
    assertEquals(created.revision(), service.find("alpha").orElseThrow().revision());
  }

  @Test
  void positionsCanBeClearedOptimistically() {
    FakeRepository repository = new FakeRepository();
    ArenaService service = service(repository);
    ArenaDefinition created = service.create("alpha").arena().orElseThrow();
    ArenaLocation location = new ArenaLocation("world", new ArenaVector(1, 64, 1), 0, 0);
    ArenaDefinition set =
        service.setWaiting("alpha", location, created.revision()).arena().orElseThrow();
    ArenaDefinition cleared = service.clearWaiting("alpha", set.revision()).arena().orElseThrow();
    assertTrue(cleared.waitingLocation().isEmpty());
  }

  @Test
  void boundarySupportsPartialAutosavedEditingAndReset() {
    FakeRepository repository = new FakeRepository();
    ArenaService service = service(repository);
    ArenaDefinition created = service.create("alpha").arena().orElseThrow();
    ArenaDefinition minimum =
        service
            .setBoundaryMinimum("alpha", new ArenaVector(0, 0, 0), created.revision())
            .arena()
            .orElseThrow();
    assertTrue(minimum.boundary().orElseThrow().minimum().isPresent());
    ArenaDefinition maximum =
        service
            .setBoundaryMaximum("alpha", new ArenaVector(10, 10, 10), minimum.revision())
            .arena()
            .orElseThrow();
    assertTrue(maximum.boundary().orElseThrow().ordered());
    ArenaDefinition cleared =
        service.clearBoundary("alpha", maximum.revision()).arena().orElseThrow();
    assertTrue(cleared.boundary().isEmpty());
  }

  @Test
  void deleteRejectsAnObsoleteEditorRevision() {
    FakeRepository repository = new FakeRepository();
    ArenaService service = service(repository);
    ArenaDefinition created = service.create("alpha").arena().orElseThrow();
    service.setWorld("alpha", "world", created.revision());
    assertEquals(ArenaOperationCode.CONFLICT, service.delete("alpha", created.revision()).code());
    assertTrue(service.find("alpha").isPresent());
  }

  private static ArenaService service(FakeRepository repository) {
    return new ArenaService(
        repository, new ArenaRegistry(), new ArenaValidator(world -> world.equals("world")), CLOCK);
  }

  private static ArenaGeneratorDefinition generator(
      String id, GeneratorResource resource, ArenaVector position) {
    return new ArenaGeneratorDefinition(
        new GeneratorId(id),
        resource,
        new ArenaLocation("world", position, 0, 0),
        1,
        20,
        1,
        48,
        GeneratorStackingStrategy.MERGE_NEARBY);
  }

  private static ArenaDefinition complete(String id) {
    ArenaLocation waiting = new ArenaLocation("world", new ArenaVector(0, 64, 0), 0, 0);
    ArenaLocation spectator = new ArenaLocation("world", new ArenaVector(0, 70, 0), 0, 0);
    ArenaDefinition base =
        ArenaDefinition.draft(new ArenaId(id), NOW)
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
    return base.withTeams(
        base.teams().stream()
            .limit(2)
            .map(
                team -> {
                  int x = 10 + team.order() * 3;
                  ArenaBedDefinition bed =
                      new ArenaBedDefinition(
                          new ArenaBlockPosition("world", x, 64, 0),
                          new ArenaBlockPosition("world", x + 1, 64, 0),
                          "EAST");
                  return team.withSpawn(Optional.of(waiting)).withBedDefinition(Optional.of(bed));
                })
            .toList(),
        ArenaStatus.READY,
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
