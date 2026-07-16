package fr.heneria.bedwars.core.arena.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.heneria.bedwars.core.arena.ArenaDefinition;
import fr.heneria.bedwars.core.arena.ArenaId;
import fr.heneria.bedwars.core.arena.ArenaStatus;
import fr.heneria.bedwars.core.config.ProblemSeverity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ArenaEditorStateTest {
  @Test
  void filterAndSortSurvivePageNavigation() {
    ArenaEditorViewState state = new ArenaEditorViewState();
    state.filter(ArenaListFilter.INVALID);
    state.sort(ArenaListSort.UPDATED);
    state.page(3);
    assertEquals(ArenaListFilter.INVALID, state.filter());
    assertEquals(ArenaListSort.UPDATED, state.sort());
    assertEquals(3, state.page());
  }

  @Test
  void changingFilterOrSortReturnsToFirstPage() {
    ArenaEditorViewState state = new ArenaEditorViewState();
    state.page(4);
    state.filter(ArenaListFilter.DRAFT);
    assertEquals(0, state.page());
    state.page(2);
    state.sort(ArenaListSort.NAME);
    assertEquals(0, state.page());
  }

  @Test
  void stateStoreCleansDisconnectedPlayer() {
    ArenaEditorStateStore store = new ArenaEditorStateStore();
    UUID player = UUID.randomUUID();
    store.state(player).observe("alpha", 4);
    assertEquals(1, store.size());
    store.remove(player);
    assertEquals(0, store.size());
  }

  @Test
  void observedRevisionCanBeRefreshed() {
    ArenaEditorViewState state = new ArenaEditorViewState();
    state.observe("alpha", 1);
    state.observe("alpha", 2);
    assertEquals(2, state.observedRevision("alpha", 0));
  }

  @Test
  void filtersMatchAdministrativeStatuses() {
    ArenaDefinition draft = ArenaDefinition.draft(new ArenaId("draft"), Instant.EPOCH);
    ArenaDefinition enabled = draft.withStatus(ArenaStatus.ENABLED, Instant.EPOCH);
    assertTrue(ArenaListFilter.DRAFT.accepts(draft));
    assertTrue(ArenaListFilter.ENABLED.accepts(enabled));
    assertFalse(ArenaListFilter.DISABLED.accepts(enabled));
  }

  @Test
  void problemFieldsRouteToCorrectEditorSections() {
    assertEquals(ArenaEditorSection.WORLD, ArenaProblemRouter.section("world"));
    assertEquals(ArenaEditorSection.WAITING, ArenaProblemRouter.section("locations.waiting"));
    assertEquals(ArenaEditorSection.PLAYERS, ArenaProblemRouter.section("players.maximum"));
    assertEquals(ArenaEditorSection.BOUNDARY, ArenaProblemRouter.section("boundary"));
  }

  @Test
  void everySeverityHasAVisualItem() {
    assertEquals(
        List.of(
            "arena.validation.info",
            "arena.validation.warning",
            "arena.validation.error",
            "arena.validation.critical"),
        java.util.Arrays.stream(ProblemSeverity.values())
            .map(ArenaValidationVisual::itemKey)
            .toList());
  }
}
