package fr.heneria.zombie.plugin.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fr.heneria.zombie.core.editor.MapObjectType;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class MapVisualizationServiceTest {

  @Test
  void mapsPaidStationsToVisibleChestBlocks() {
    assertEquals(
        Material.CHEST, MapVisualizationService.stationMaterial(MapObjectType.MYSTERY_BOX));
    assertEquals(
        Material.ENDER_CHEST, MapVisualizationService.stationMaterial(MapObjectType.PACK_A_PUNCH));
    assertThrows(
        IllegalArgumentException.class,
        () -> MapVisualizationService.stationMaterial(MapObjectType.BARRICADE));
  }
}
