package fr.heneria.zombie.plugin.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GuiConfigurationServiceTest {

  private Path directory;

  @BeforeEach
  void prepareDirectory() throws Exception {
    directory = Path.of("build", "test-data", "gui-" + UUID.randomUUID());
    Files.createDirectories(directory);
  }

  @Test
  void installsDefaultsAndResolvesTheme() {
    GuiConfigurationService service = service();
    GuiConfigurationSnapshot snapshot = service.initializeAsync().join();

    GuiMenuTemplate menu = snapshot.menu(new GuiId("admin-main")).orElseThrow();
    assertEquals(54, menu.size());
    assertEquals("dark", snapshot.theme(menu).id());
    assertTrue(Files.isRegularFile(directory.resolve("guis.yml")));
  }

  @Test
  void rejectsInvalidSizeAndPreservesPreviousSnapshot() throws Exception {
    GuiConfigurationService service = service();
    GuiConfigurationSnapshot before = service.initializeAsync().join();
    Files.writeString(
        directory.resolve("guis.yml"),
        """
        default-theme: dark
        themes:
          dark:
            background: {material: STONE, name: " "}
        menus:
          broken:
            title: broken
            size: 10
            theme: dark
        """);

    assertThrows(CompletionException.class, () -> service.reloadAsync().join());
    assertEquals(before, service.current());
  }

  @Test
  void rejectsSlotCollisionsAndUnknownActions() throws Exception {
    GuiConfigurationService service = service();
    service.initializeAsync().join();
    Files.writeString(
        directory.resolve("guis.yml"),
        """
        default-theme: dark
        themes:
          dark:
            background: {material: STONE, name: " "}
        menus:
          broken:
            title: broken
            size: 9
            theme: dark
            buttons:
              one: {slot: 0, material: STONE}
              two: {slot: 0, material: PAPER, actions: {left: missing.action}}
        """);

    CompletionException failure =
        assertThrows(CompletionException.class, () -> service.reloadAsync().join());
    assertTrue(failure.getCause().getMessage().contains("collision"));
    assertTrue(failure.getCause().getMessage().contains("unknown action"));
  }

  private GuiConfigurationService service() {
    return new GuiConfigurationService(
        directory,
        getClass().getClassLoader(),
        Runnable::run,
        () -> GuiScreens.ACTION_IDS,
        ignored -> {});
  }
}
