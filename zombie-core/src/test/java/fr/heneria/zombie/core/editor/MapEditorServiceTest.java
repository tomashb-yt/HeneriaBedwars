package fr.heneria.zombie.core.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;

class MapEditorServiceTest {

  @Test
  void createsMapOpensSingleSessionAutosavesAndSupportsHistory() {
    MemoryPersistence persistence = new MemoryPersistence();
    MapEditorService service =
        new MapEditorService(
            new MapRegistry(),
            persistence,
            Clock.fixed(Instant.parse("2026-07-25T12:00:00Z"), ZoneOffset.UTC));
    UUID player = UUID.randomUUID();

    MapDefinition created = service.create("crypt", "Crypt", player, "world").join();
    assertEquals(created, persistence.values.get("crypt"));
    assertTrue(service.open(player, "crypt").isPresent());
    assertTrue(service.open(player, "crypt").isEmpty());

    MapPoint point = new MapPoint("world", 1, 65, 2, 0, 0);
    service
        .mutate(
            player,
            map ->
                map.withZone(
                    new MapDefinition.Zone("spawn", "Spawn", "AQUA", "", "", 1, point),
                    Instant.parse("2026-07-25T12:00:01Z")))
        .join();
    assertEquals(1, persistence.values.get("crypt").zones().size());
    assertTrue(service.undo(player).join());
    assertTrue(persistence.values.get("crypt").zones().isEmpty());
    assertTrue(service.redo(player).join());
    assertEquals(1, persistence.values.get("crypt").zones().size());
    assertTrue(service.leave(player).join());
    assertFalse(service.session(player).isPresent());
  }

  @Test
  void locksOneMapToOneEditorUntilTheSessionLeaves() {
    MapEditorService service =
        new MapEditorService(new MapRegistry(), new MemoryPersistence(), Clock.systemUTC());
    UUID creator = UUID.randomUUID();
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    service.create("crypt", "Crypt", creator, "world").join();

    assertTrue(service.open(first, "crypt").isPresent());
    assertEquals(first, service.editorOf("crypt").orElseThrow());
    assertTrue(service.open(second, "crypt").isEmpty());

    assertTrue(service.leave(first).join());
    assertTrue(service.open(second, "crypt").isPresent());
  }

  @Test
  void duplicatesACompleteDefinitionIntoADistinctEditingWorld() {
    MemoryPersistence persistence = new MemoryPersistence();
    MapEditorService service =
        new MapEditorService(new MapRegistry(), persistence, Clock.systemUTC());
    UUID creator = UUID.randomUUID();
    service.create("crypt", "Crypt", creator, "world").join();
    MapPoint point = new MapPoint("world", 4, 70, 8, 90, 0);
    service.open(creator, "crypt");
    service.mutate(creator, map -> map.withPlayerSpawn(point, Instant.now())).join();
    service.leave(creator).join();

    MapDefinition copy =
        service.duplicate("crypt", "crypt_copy", creator, "zombie_editing/crypt_copy").join();

    assertEquals("crypt_copy", copy.id());
    assertEquals("zombie_editing/crypt_copy", copy.world());
    assertEquals("zombie_editing/crypt_copy", copy.playerSpawn().orElseThrow().world());
    assertEquals(copy, persistence.values.get("crypt_copy"));
  }

  private static final class MemoryPersistence implements MapPersistence {
    private final Map<String, MapDefinition> values = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<Collection<MapDefinition>> loadAll() {
      return CompletableFuture.completedFuture(values.values());
    }

    @Override
    public CompletableFuture<Void> save(MapDefinition definition) {
      values.put(definition.id(), definition);
      return CompletableFuture.completedFuture(null);
    }
  }
}
