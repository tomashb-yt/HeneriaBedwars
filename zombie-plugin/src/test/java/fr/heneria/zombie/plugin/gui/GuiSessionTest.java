package fr.heneria.zombie.plugin.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GuiSessionTest {

  @Test
  void keepsIsolatedNavigationStateAndRotatesViewToken() {
    GuiSession session = new GuiSession(UUID.randomUUID(), Instant.EPOCH);
    UUID initial = session.viewToken();
    session.activate(new GuiId("player-main"), GuiContext.EMPTY, true);
    session.history().push(new GuiId("player-main"), GuiContext.EMPTY);
    session.activate(new GuiId("maps"), GuiContext.EMPTY, false);
    session.page(12);
    session.search("dead");

    assertNotEquals(initial, session.viewToken());
    assertEquals(1, session.history().size());
    assertEquals("dead", session.search());
    assertEquals(0, session.page());
  }

  @Test
  void cleanupReleasesEveryTransientReference() {
    GuiSession session = new GuiSession(UUID.randomUUID(), Instant.EPOCH);
    session.activate(new GuiId("maps"), GuiContext.EMPTY, true);
    session.filters().put("state", "valid");
    session.temporaryData().put("selection", "map");
    session.clear();

    assertTrue(session.currentGui().isEmpty());
    assertTrue(session.filters().isEmpty());
    assertTrue(session.temporaryData().isEmpty());
    assertEquals(0, session.history().size());
  }

  @Test
  void rejectsAnImmediateDuplicateClick() {
    GuiSession session = new GuiSession(UUID.randomUUID(), Instant.EPOCH);
    assertTrue(session.tryClick(Instant.EPOCH, Duration.ofMillis(150)));
    assertFalse(session.tryClick(Instant.EPOCH.plusMillis(20), Duration.ofMillis(150)));
    assertTrue(session.tryClick(Instant.EPOCH.plusMillis(151), Duration.ofMillis(150)));
  }
}
