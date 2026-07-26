package fr.heneria.zombie.plugin.editor;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

class EditorItemServiceTest {

  @Test
  void ignoresNullItemsAndMetadata() {
    EditorItemService service = new EditorItemService(NamespacedKey.minecraft("map_editor_tool"));

    assertFalse(service.isTool(null));
    assertFalse(service.hasToolMarker(null));
  }
}
