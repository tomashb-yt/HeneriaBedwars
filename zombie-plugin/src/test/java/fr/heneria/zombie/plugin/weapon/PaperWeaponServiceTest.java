package fr.heneria.zombie.plugin.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;

import fr.heneria.zombie.core.editor.MapDefinition;
import fr.heneria.zombie.core.editor.MapObjectType;
import fr.heneria.zombie.core.editor.MapPoint;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PaperWeaponServiceTest {

  @Test
  void stationAnimationDefaultsToFiveSecondsAndRejectsTooShortValues() {
    assertEquals(100, PaperWeaponService.animationTicks(station(Map.of())));
    assertEquals(140, PaperWeaponService.animationTicks(station(Map.of("animation-ticks", "140"))));
    assertEquals(20, PaperWeaponService.animationTicks(station(Map.of("animation-ticks", "1"))));
    assertEquals(
        100, PaperWeaponService.animationTicks(station(Map.of("animation-ticks", "invalid"))));
  }

  private static MapDefinition.MapObject station(Map<String, String> properties) {
    return new MapDefinition.MapObject(
        "station",
        MapObjectType.MYSTERY_BOX,
        "Station",
        "zone_1",
        new MapPoint("world", 0, 64, 0, 0, 0),
        properties);
  }
}
