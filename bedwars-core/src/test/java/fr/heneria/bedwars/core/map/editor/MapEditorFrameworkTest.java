package fr.heneria.bedwars.core.map.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.heneria.bedwars.core.map.MapId;
import fr.heneria.bedwars.core.map.MapTemplate;
import fr.heneria.bedwars.core.map.MapType;
import fr.heneria.bedwars.core.map.MapWorldSettings;
import fr.heneria.bedwars.core.map.operation.MapOperationStatus;
import fr.heneria.bedwars.core.map.operation.MapOperationTracker;
import fr.heneria.bedwars.core.map.operation.MapOperationType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MapEditorFrameworkTest {
  private static final Instant NOW = Instant.parse("2026-07-16T20:00:00Z");

  @Test
  void viewStateResetsPagingAndStoreCleansEveryReference() {
    UUID player = UUID.randomUUID();
    MapEditorStateStore store = new MapEditorStateStore();
    MapEditorViewState state = store.state(player);
    state.page(4);
    state.filter(MapListFilter.LOADED);
    assertEquals(0, state.page());
    state.observe("desert", 7);
    state.followOperation("desert");
    store.forget("desert");
    assertTrue(state.observedMapId().isEmpty());
    assertTrue(state.followedOperationMapId().isEmpty());
    store.remove(player);
    assertEquals(0, store.size());
  }

  @Test
  void filtersAndSortsArePureAndDeterministic() {
    MapTemplate zeta = template("zeta", MapType.BEDWARS);
    MapTemplate alpha = template("alpha", MapType.GENERIC);
    List<MapTemplate> source = List.of(zeta, alpha);
    assertEquals(List.of(zeta), source.stream().filter(MapListFilter.BEDWARS::accepts).toList());
    assertEquals(
        List.of("alpha", "zeta"),
        source.stream()
            .sorted(MapListSort.ID.comparator(MapSortDirection.ASCENDING))
            .map(map -> map.id().value())
            .toList());
    assertEquals(List.of(zeta, alpha), source);
  }

  @Test
  void progressSeparatesGuidanceFromTechnicalValidation() {
    MapProgressEvaluator evaluator = new MapProgressEvaluator();
    MapTemplate draft = template("desert", MapType.BEDWARS);
    MapProgress initial = evaluator.evaluate(draft, false);
    assertEquals(1, initial.current());
    assertEquals("map.workflow.next.spawn", initial.nextActionKey());

    MapTemplate ready =
        draft
            .withSpawn(
                new fr.heneria.bedwars.core.map.MapSpawn(true, draft.worldName(), 0, 64, 0, 0, 0),
                NOW.plusSeconds(1))
            .savedAt(NOW.plusSeconds(2))
            .withLinks(Set.of("arena"), NOW.plusSeconds(3));
    assertEquals("map.workflow.next.complete", evaluator.evaluate(ready, false).nextActionKey());
  }

  @Test
  void trackerRefusesConcurrentOperationAndRetainsFinalStatus() {
    MapOperationTracker tracker = new MapOperationTracker(Clock.fixed(NOW, ZoneOffset.UTC));
    MapId id = MapId.parse("desert");
    UUID player = UUID.randomUUID();
    assertTrue(tracker.start(id, MapOperationType.BACKUP, player, "preparing").isPresent());
    assertTrue(tracker.active(id));
    assertTrue(tracker.start(id, MapOperationType.DELETE, player, "blocked").isEmpty());
    tracker.running(id, "copying");
    assertEquals(MapOperationStatus.RUNNING, tracker.find(id).orElseThrow().status());
    tracker.success(id, "complete");
    assertFalse(tracker.active(id));
    assertEquals(MapOperationStatus.SUCCESS, tracker.find(id).orElseThrow().status());
  }

  private static MapTemplate template(String id, MapType type) {
    return MapTemplate.create(
        MapId.parse(id),
        type,
        "hbw_template_" + id,
        "NORMAL",
        new MapWorldSettings(false, false, false, 6000, true),
        64,
        "Admin",
        NOW);
  }
}
