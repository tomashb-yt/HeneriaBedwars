package fr.heneria.zombie.plugin.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
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

  @Test
  void deeplyMergesEditorMenusIntoAPreEditorConfiguration() throws Exception {
    Files.writeString(
        directory.resolve("guis.yml"),
        """
        default-theme: custom
        themes:
          custom:
            background: {material: GRAY_STAINED_GLASS_PANE, name: " "}
        menus:
          admin-main:
            title: "<red>Administration personnalisée"
            size: 54
            theme: custom
            buttons:
              maps: {slot: 20, material: FILLED_MAP, name: "<aqua>Maps", actions: {left: nav.maps}}
        """);

    GuiConfigurationSnapshot snapshot = service().initializeAsync().join();
    GuiMenuTemplate editor = snapshot.menu(new GuiId("editor-main")).orElseThrow();
    GuiButtonTemplate information = editor.buttons().get("info");

    assertEquals(
        "<red>Administration personnalisée",
        snapshot.menu(new GuiId("admin-main")).orElseThrow().title());
    assertEquals("dark", editor.theme());
    assertEquals(10, information.slot());
    assertEquals(org.bukkit.Material.BOOK, information.material());
  }

  private GuiConfigurationService service() {
    return new GuiConfigurationService(
        directory,
        getClass().getClassLoader(),
        Runnable::run,
        () -> {
          HashSet<String> actions = new HashSet<>(GuiScreens.ACTION_IDS);
          actions.addAll(fr.heneria.zombie.plugin.editor.EditorGuiModule.ACTION_IDS);
          actions.addAll(MapMenuModule.ACTION_IDS);
          return Set.copyOf(actions);
        },
        ignored -> {});
  }
}
