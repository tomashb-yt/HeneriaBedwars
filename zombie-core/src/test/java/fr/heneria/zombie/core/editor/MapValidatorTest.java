package fr.heneria.zombie.core.editor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MapValidatorTest {
  private final MapValidator validator = new MapValidator();

  @Test
  void reportsMissingFoundationAndOrphanReferences() {
    MapDefinition map =
        MapDefinition.create("bad", "Bad", UUID.randomUUID(), Instant.EPOCH, "world");
    MapPoint point = new MapPoint("world", 0, 64, 0, 0, 0);
    map =
        map.withZombieSpawn(
            new MapDefinition.ZombieSpawn(
                "spawn_1",
                "Spawn",
                "missing",
                1,
                1,
                1,
                10,
                0,
                64,
                false,
                Set.of("NORMAL"),
                20,
                point),
            Instant.EPOCH);

    ValidationReport report = validator.validate(map);
    assertFalse(report.valid());
    assertTrue(report.errors().stream().anyMatch(value -> value.contains("Spawn joueur")));
    assertTrue(report.errors().stream().anyMatch(value -> value.contains("orphelin")));
  }

  @Test
  void acceptsConnectedCompleteFoundation() {
    MapPoint point = new MapPoint("world", 0, 64, 0, 0, 0);
    UUID creator = UUID.randomUUID();
    MapDefinition map =
        new MapDefinition(
            MapDefinition.CURRENT_SCHEMA,
            "crypt",
            "Crypt",
            "",
            creator,
            Instant.EPOCH,
            Instant.EPOCH,
            "world",
            "FILLED_MAP",
            "",
            4,
            "",
            "NORMAL",
            "CLASSIC",
            Optional.of(point),
            Map.of(
                "a", new MapDefinition.Zone("a", "A", "AQUA", "", "", 1, point),
                "b", new MapDefinition.Zone("b", "B", "BLUE", "", "", 1, point)),
            Map.of(
                "door",
                new MapDefinition.Door(
                    "door", "Door", 750, "a", "b", "DOOR", false, "", "", "", point)),
            Map.of(
                "spawn",
                new MapDefinition.ZombieSpawn(
                    "spawn", "Spawn", "a", 1, 4, 1, 99, 0, 64, false, Set.of("NORMAL"), 20, point)),
            Map.of(
                "box",
                new MapDefinition.MapObject(
                    "box", MapObjectType.MYSTERY_BOX, "Box", "a", point, Map.of()),
                "pap",
                new MapDefinition.MapObject(
                    "pap", MapObjectType.PACK_A_PUNCH, "PaP", "b", point, Map.of())));

    assertTrue(validator.validate(map).valid());
  }
}
