package fr.heneria.bedwars.core.game.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fr.heneria.bedwars.core.config.MessageRenderer;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RuntimeScoreboardRendererTest {
  private final RuntimeScoreboardRenderer renderer =
      new RuntimeScoreboardRenderer(new MessageRenderer());

  @Test
  void rendersLocalizedValuesEmptyLinesAndVisualDuplicates() {
    RuntimeScoreboardView view =
        renderer.render(
            "<aqua><bold>{server}</bold></aqua>",
            List.of("", "<white>{state}", "", "<white>{state}"),
            Map.of("server", "Heneria", "state", "En attente"));
    assertEquals("§b§lHeneria§r§r", view.title());
    assertEquals(List.of("", "§fEn attente", "", "§fEn attente"), view.lines());
  }

  @Test
  void keepsUnknownPlaceholdersVisibleForDiagnostics() {
    assertEquals(
        List.of("{missing}"), renderer.render("Title", List.of("{missing}"), Map.of()).lines());
  }

  @Test
  void refusesMoreThanFifteenLines() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new RuntimeScoreboardView("Title", java.util.Collections.nCopies(16, "line")));
  }
}
