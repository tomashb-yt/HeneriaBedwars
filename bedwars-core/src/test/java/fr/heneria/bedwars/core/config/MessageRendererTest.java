package fr.heneria.bedwars.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MessageRendererTest {
  private final MessageRenderer renderer = new MessageRenderer();

  @Test
  void replacesStringAndNumberPlaceholders() {
    PlaceholderContext context =
        PlaceholderContext.builder().put("player", "Alex").put("count", 4).build();
    assertEquals("Hello Alex: 4", renderer.render("Hello {player}: {count}", context));
  }

  @Test
  void leavesUnknownPlaceholderVisible() {
    assertEquals("Hello {unknown}", renderer.render("Hello {unknown}", PlaceholderContext.EMPTY));
  }

  @Test
  void doesNotInterpretTagsFromPlaceholderValues() {
    PlaceholderContext context = PlaceholderContext.builder().put("player", "<red>&cAlex").build();
    assertEquals("§aHello <red>&cAlex", renderer.render("<green>Hello {player}", context));
  }

  @Test
  void supportsMiniMessageAndLegacyColors() {
    assertEquals("§cRed §aGreen", renderer.render("<red>Red &aGreen", PlaceholderContext.EMPTY));
  }

  @Test
  void supportsHexColors() {
    assertEquals("§x§F§F§A§A§0§0Gold", renderer.render("<#FFAA00>Gold", PlaceholderContext.EMPTY));
    assertEquals("§x§F§F§A§A§0§0Gold", renderer.render("&#FFAA00Gold", PlaceholderContext.EMPTY));
  }

  @Test
  void nullPlaceholderIsSafe() {
    assertEquals(
        "Value: ",
        renderer.render("Value: {value}", PlaceholderContext.builder().put("value", null).build()));
  }

  @Test
  void missingTranslationHasDiagnosticFallback() {
    TranslationBundle bundle = new TranslationBundle("test", 1, java.util.Map.of());
    assertTrue(bundle.message(TranslationKey.NO_PERMISSION).contains("Missing translation"));
  }
}
