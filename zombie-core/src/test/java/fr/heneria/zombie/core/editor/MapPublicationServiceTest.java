package fr.heneria.zombie.core.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;

class MapPublicationServiceTest {

  @Test
  void publishesImmutableVersionsAndHidesUnpublishedMap() {
    MapRegistry maps = new MapRegistry();
    MapDefinition first = validMap("crypt", "Crypt");
    maps.register(first);
    MemoryPublicationPersistence persistence = new MemoryPublicationPersistence();
    MapPublicationService service =
        new MapPublicationService(
            maps,
            new MapValidator(),
            persistence,
            Clock.fixed(Instant.parse("2026-07-27T10:00:00Z"), ZoneOffset.UTC));
    UUID actor = UUID.randomUUID();

    MapPublication v1 = service.publish("crypt", actor).join();
    maps.update(first.withDisplayName("Crypt Redux", Instant.parse("2026-07-27T10:01:00Z")));

    assertEquals(1, v1.activeVersion().orElseThrow());
    assertEquals("Crypt", service.publishedDefinition("crypt").orElseThrow().displayName());

    MapPublication v2 = service.publish("crypt", actor).join();
    assertEquals(2, v2.activeVersion().orElseThrow());
    assertEquals("Crypt Redux", service.publishedDefinition("crypt").orElseThrow().displayName());
    assertNotEquals(v2.versions().get(0).definition(), v2.versions().get(1).definition());

    MapPublication v3 = service.rollback("crypt", 1, actor).join();
    assertEquals(3, v3.activeVersion().orElseThrow());
    assertEquals(1, v3.active().orElseThrow().restoredFrom().orElseThrow());
    assertEquals("Crypt", service.publishedDefinition("crypt").orElseThrow().displayName());

    service.unpublish("crypt").join();
    assertFalse(service.publishedDefinition("crypt").isPresent());
    assertEquals(3, persistence.values.get("crypt").versions().size());
  }

  @Test
  void rejectsInvalidDefinitionsBeforePersistence() {
    MapRegistry maps = new MapRegistry();
    maps.register(
        MapDefinition.create("empty", "Empty", UUID.randomUUID(), Instant.EPOCH, "world"));
    MemoryPublicationPersistence persistence = new MemoryPublicationPersistence();
    MapPublicationService service =
        new MapPublicationService(maps, new MapValidator(), persistence, Clock.systemUTC());

    assertThrows(
        java.util.concurrent.CompletionException.class,
        () -> service.publish("empty", UUID.randomUUID()).join());
    assertTrue(persistence.values.isEmpty());
  }

  @Test
  void permanentDeletionRemovesPublicationAndRejectsLaterPublishing() {
    MapRegistry maps = new MapRegistry();
    maps.register(validMap("crypt", "Crypt"));
    MapPublicationService service =
        new MapPublicationService(
            maps, new MapValidator(), new MemoryPublicationPersistence(), Clock.systemUTC());
    service.publish("crypt", UUID.randomUUID()).join();

    service
        .delete(
            "crypt",
            () -> {
              maps.remove(maps.find("crypt").orElseThrow());
              return CompletableFuture.completedFuture(null);
            })
        .join();

    assertTrue(service.published().isEmpty());
    assertThrows(IllegalArgumentException.class, () -> service.publish("crypt", UUID.randomUUID()));
  }

  private static MapDefinition validMap(String id, String name) {
    UUID creator = UUID.randomUUID();
    Instant now = Instant.parse("2026-07-27T09:00:00Z");
    MapPoint point = new MapPoint("world", 0, 64, 0, 0, 0);
    return new MapDefinition(
        MapDefinition.CURRENT_SCHEMA,
        id,
        name,
        "Description",
        creator,
        now,
        now,
        "world",
        "FILLED_MAP",
        "",
        1,
        4,
        "",
        "NORMAL",
        "CLASSIC",
        Optional.of(point),
        Map.of("zone", new MapDefinition.Zone("zone", "Zone", "AQUA", "", "", 1, point)),
        Map.of(),
        Map.of(
            "spawn",
            new MapDefinition.ZombieSpawn(
                "spawn", "Spawn", "zone", 1, 4, 1, 100, 0, 100, false, Set.of(), 0, point)),
        Map.of());
  }

  private static final class MemoryPublicationPersistence implements MapPublicationPersistence {
    private final Map<String, MapPublication> values = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<Collection<MapPublication>> loadAll() {
      return CompletableFuture.completedFuture(values.values());
    }

    @Override
    public CompletableFuture<Void> save(MapPublication publication) {
      values.put(publication.mapId(), publication);
      return CompletableFuture.completedFuture(null);
    }
  }
}
