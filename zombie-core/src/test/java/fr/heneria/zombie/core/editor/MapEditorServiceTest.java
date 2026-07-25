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
