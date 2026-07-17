package fr.heneria.bedwars.core.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MapTemplateFrameworkTest {
  private static final Instant NOW = Instant.parse("2026-07-16T20:00:00Z");

  @Test
  void mapIdNormalizesCaseAndAcceptsSafeCharacters() {
    assertEquals("sky-islands_2", MapId.parse("SKY-Islands_2").value());
  }

  @Test
  void mapIdRejectsSpacesTraversalSlashesSpecialCharactersAndLengths() {
    for (String value :
        List.of(
            "a",
            "a".repeat(33),
            "Lobby Principal",
            "../world",
            "map/test",
            "épreuve",
            "C:\\server"))
      assertThrows(IllegalArgumentException.class, () -> MapId.parse(value), value);
  }

  @Test
  void templateIsImmutableAndRevisionChangesOnlyForPersistentEdits() {
    MapTemplate original = template("desert");
    MapTemplate loading = original.transition(MapState.LOADING, NOW.plusSeconds(1));
    MapTemplate renamed = original.withDisplayName("Desert Temple", NOW.plusSeconds(2));
    assertEquals(1, loading.revision());
    assertEquals(2, renamed.revision());
    assertEquals("desert", original.displayName());
    assertNotSame(original, renamed);
  }

  @Test
  void registryIsSortedImmutableAndSearchesByWorld() {
    MapTemplateRegistry registry = new MapTemplateRegistry();
    registry.replace(List.of(template("zeta"), template("alpha")));
    assertEquals(
        List.of("alpha", "zeta"), registry.all().stream().map(map -> map.id().value()).toList());
    assertEquals("alpha", registry.findByWorld("HBW_TEMPLATE_ALPHA").orElseThrow().id().value());
    assertThrows(UnsupportedOperationException.class, () -> registry.snapshot().clear());
  }

  @Test
  void registryRejectsDuplicates() {
    MapTemplateRegistry registry = new MapTemplateRegistry();
    assertThrows(
        IllegalArgumentException.class,
        () -> registry.replace(List.of(template("alpha"), template("alpha"))));
  }

  @Test
  void operationLockRefusesOverlapAndReleasesAfterUse() {
    MapOperationLock lock = new MapOperationLock();
    MapId id = MapId.parse("alpha");
    assertTrue(lock.acquire(id));
    assertFalse(lock.acquire(id));
    lock.release(id);
    assertTrue(lock.acquire(id));
  }

  @Test
  void createPublishesOnlyAfterWorldAndMetadataSuccess() {
    Fixture fixture = new Fixture();
    MapOperationResult result = fixture.service.create("desert", MapType.BEDWARS, "Admin");
    assertTrue(result.successful());
    assertTrue(result.template().orElseThrow().loaded());
    assertEquals(2, result.template().orElseThrow().revision());
    assertEquals(1, fixture.service.list().size());
    assertTrue(fixture.repository.exists(MapId.parse("desert")));
  }

  @Test
  void failedWorldCreationDoesNotPublishPartialTemplate() {
    Fixture fixture = new Fixture();
    fixture.worlds.failCreate = true;
    MapOperationResult result = fixture.service.create("desert", MapType.BEDWARS, "Admin");
    assertEquals(MapOperationCode.WORLD_CREATION_FAILED, result.code());
    assertTrue(fixture.service.list().isEmpty());
    assertFalse(fixture.repository.exists(MapId.parse("desert")));
  }

  @Test
  void saveAndUnloadAdvanceRevisionAndRefusePlayers() {
    Fixture fixture = new Fixture();
    MapTemplate created =
        fixture.service.create("desert", MapType.BEDWARS, "Admin").template().orElseThrow();
    MapTemplate saved = fixture.service.save("desert").template().orElseThrow();
    assertEquals(created.revision() + 1, saved.revision());
    fixture.worlds.players = 1;
    assertEquals(MapOperationCode.PLAYERS_PRESENT, fixture.service.unload("desert", false).code());
    fixture.worlds.players = 0;
    assertEquals(
        MapState.UNLOADED,
        fixture.service.unload("desert", false).template().orElseThrow().state());
  }

  @Test
  void duplicateCreatesIndependentMetadataAndCleansAfterFailure() {
    Fixture fixture = new Fixture();
    fixture.service.create("desert", MapType.BEDWARS, "Admin");
    MapTemplate copy =
        fixture.service.duplicate("desert", "copy", "Builder").template().orElseThrow();
    assertEquals(MapId.parse("copy"), copy.id());
    assertEquals(MapState.UNLOADED, copy.state());
    fixture.files.failCopy = true;
    assertEquals(
        MapOperationCode.COPY_FAILED,
        fixture.service.duplicate("desert", "broken", "Builder").code());
    assertTrue(fixture.service.find("broken").isEmpty());
  }

  @Test
  void importLifecycleUnloadsBacksUpReplacesAndReloadsAsBedWars() {
    Fixture fixture = new Fixture();
    fixture.service.create("desert", MapType.GENERIC, "Admin");
    fixture.files.importReady = true;

    assertTrue(fixture.service.prepareImport("desert").successful());
    assertFalse(fixture.worlds.loaded);
    assertTrue(fixture.service.completeImportFiles("desert").successful());
    MapOperationResult finished = fixture.service.finishImport("desert");

    assertTrue(finished.successful());
    assertTrue(fixture.files.replaced);
    assertEquals(1, fixture.files.backups);
    assertEquals(MapType.BEDWARS, finished.template().orElseThrow().type());
    assertTrue(finished.template().orElseThrow().loaded());
  }

  @Test
  void deletionRequiresBackupAndRefusesLinkedTemplate() throws Exception {
    Fixture fixture = new Fixture();
    MapTemplate created =
        fixture.service.create("desert", MapType.BEDWARS, "Admin").template().orElseThrow();
    MapTemplate linked = created.withLinks(java.util.Set.of("arena"), NOW.plusSeconds(3));
    fixture.repository.save(linked);
    fixture.registry.put(linked);
    assertEquals(MapOperationCode.MAP_LINKED, fixture.service.delete("desert").code());
    fixture.repository.save(created);
    fixture.registry.put(created);
    fixture.files.failBackup = true;
    assertEquals(MapOperationCode.BACKUP_FAILED, fixture.service.delete("desert").code());
    assertTrue(fixture.service.find("desert").isPresent());
  }

  @Test
  void liveArenaRelationRefusesDeletionEvenWhenDerivedMetadataIsStale() {
    Fixture fixture = new Fixture();
    MapTemplateService guarded =
        new MapTemplateService(
            fixture.repository,
            fixture.registry,
            fixture.worlds,
            fixture.files,
            new MapOperationLock(),
            new MapTemplateValidator(),
            Clock.fixed(NOW, ZoneOffset.UTC),
            "hbw_template_",
            "NORMAL",
            new MapWorldSettings(false, false, false, 6000, true),
            64,
            true,
            "test",
            id -> java.util.Set.of("arena"),
            template -> false);
    guarded.create("desert", MapType.BEDWARS, "Admin");
    assertEquals(MapOperationCode.MAP_LINKED, guarded.delete("desert").code());
  }

  @Test
  void staleMetadataEditIsRejectedWithoutPersistence() {
    Fixture fixture = new Fixture();
    MapTemplate created =
        fixture.service.create("desert", MapType.BEDWARS, "Admin").template().orElseThrow();
    assertEquals(
        MapOperationCode.CONFLICT,
        fixture.service.setDisplayName("desert", "New", created.revision() - 1).code());
    assertEquals("desert", fixture.service.find("desert").orElseThrow().displayName());
  }

  @Test
  void settingsTypeSpawnAndDirtyStateUseTransactionalServiceOperations() throws Exception {
    Fixture fixture = new Fixture();
    MapTemplate created =
        fixture.service.create("desert", MapType.BEDWARS, "Admin").template().orElseThrow();
    MapWorldSettings settings = created.settings().withPvp(true).withDifficulty("HARD");
    MapTemplate configured =
        fixture
            .service
            .setSettings("desert", settings, created.revision())
            .template()
            .orElseThrow();
    assertTrue(configured.settings().pvp());
    assertEquals("HARD", configured.settings().difficulty());

    MapTemplate generic =
        fixture
            .service
            .setType("desert", MapType.GENERIC, configured.revision())
            .template()
            .orElseThrow();
    MapTemplate withoutSpawn =
        fixture.service.clearSpawn("desert", generic.revision()).template().orElseThrow();
    assertFalse(withoutSpawn.spawn().configured());
    long revision = withoutSpawn.revision();
    fixture.service.markDirty(withoutSpawn.id().value());
    MapTemplate dirty = fixture.service.find("desert").orElseThrow();
    assertTrue(dirty.dirty());
    assertEquals(revision, dirty.revision());
  }

  @Test
  void linkedBedWarsMapCannotChangeToAnIncompatibleType() throws Exception {
    Fixture fixture = new Fixture();
    MapTemplate created =
        fixture.service.create("desert", MapType.BEDWARS, "Admin").template().orElseThrow();
    MapTemplate linked = created.withLinks(java.util.Set.of("arena"), NOW.plusSeconds(3));
    fixture.repository.save(linked);
    fixture.registry.put(linked);
    assertEquals(
        MapOperationCode.MAP_LINKED,
        fixture.service.setType("desert", MapType.GENERIC, linked.revision()).code());
  }

  private static MapTemplate template(String id) {
    return MapTemplate.create(
        MapId.parse(id),
        MapType.BEDWARS,
        "hbw_template_" + id,
        "NORMAL",
        new MapWorldSettings(false, false, false, 6000, true),
        64,
        "Admin",
        NOW);
  }

  private static final class Fixture {
    final MemoryRepository repository = new MemoryRepository();
    final MapTemplateRegistry registry = new MapTemplateRegistry();
    final FakeWorlds worlds = new FakeWorlds();
    final FakeFiles files = new FakeFiles();
    final MapTemplateService service =
        new MapTemplateService(
            repository,
            registry,
            worlds,
            files,
            new MapOperationLock(),
            new MapTemplateValidator(),
            Clock.fixed(NOW, ZoneOffset.UTC),
            "hbw_template_",
            "NORMAL",
            new MapWorldSettings(false, false, false, 6000, true),
            64,
            true,
            "test");
  }

  private static final class MemoryRepository implements MapTemplateRepository {
    final Map<MapId, MapTemplate> values = new LinkedHashMap<>();

    @Override
    public MapTemplateLoadResult loadAll() {
      return new MapTemplateLoadResult(List.copyOf(values.values()), List.of());
    }

    @Override
    public Optional<MapTemplate> load(MapId id) {
      return Optional.ofNullable(values.get(id));
    }

    @Override
    public void save(MapTemplate template) {
      values.put(template.id(), template);
    }

    @Override
    public void delete(MapId id) {
      values.remove(id);
    }

    @Override
    public boolean exists(MapId id) {
      return values.containsKey(id);
    }
  }

  private static final class FakeWorlds implements MapWorldService {
    boolean loaded;
    boolean failCreate;
    int players;

    @Override
    public MapWorldResult createVoidWorld(MapTemplate template) {
      if (failCreate) return MapWorldResult.failure("failure");
      loaded = true;
      return MapWorldResult.success();
    }

    @Override
    public MapWorldResult load(MapTemplate template) {
      loaded = true;
      return MapWorldResult.success();
    }

    @Override
    public MapWorldResult save(MapTemplate template) {
      return loaded ? MapWorldResult.success() : MapWorldResult.failure("not loaded");
    }

    @Override
    public MapWorldResult unload(MapTemplate template, boolean save) {
      loaded = false;
      return MapWorldResult.success();
    }

    @Override
    public boolean isLoaded(MapTemplate template) {
      return loaded;
    }

    @Override
    public int playerCount(MapTemplate template) {
      return players;
    }
  }

  private static final class FakeFiles implements MapFileService {
    final java.util.Set<MapId> existing = new java.util.HashSet<>();
    boolean failCopy;
    boolean failBackup;
    boolean importReady;
    boolean replaced;
    int backups;

    @Override
    public void createTemplateDirectory(MapTemplate template) {
      existing.add(template.id());
    }

    @Override
    public void ensureImportDirectory(MapTemplate template) {}

    @Override
    public boolean importReady(MapTemplate template) {
      return importReady;
    }

    @Override
    public void replaceFromImport(MapTemplate template) throws IOException {
      if (!importReady) throw new IOException("import");
      replaced = true;
      existing.add(template.id());
    }

    @Override
    public void duplicate(MapTemplate source, MapTemplate destination) throws IOException {
      if (failCopy) throw new IOException("copy");
      existing.add(destination.id());
    }

    @Override
    public void backup(MapTemplate template, String reason, String pluginVersion)
        throws IOException {
      if (failBackup) throw new IOException("backup");
      backups++;
    }

    @Override
    public void deleteTemplate(MapTemplate template) {
      existing.remove(template.id());
    }

    @Override
    public boolean templateExists(MapTemplate template) {
      return existing.contains(template.id());
    }
  }
}
