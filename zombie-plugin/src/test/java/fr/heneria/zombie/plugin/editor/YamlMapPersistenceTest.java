package fr.heneria.zombie.plugin.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.heneria.zombie.core.editor.MapDefinition;
import fr.heneria.zombie.core.editor.MapObjectType;
import fr.heneria.zombie.core.editor.MapPoint;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class YamlMapPersistenceTest {

  @Test
  void savesReloadsAndCreatesRollbackBackup() throws Exception {
    Path root = Path.of("build", "test-data", "maps-" + UUID.randomUUID());
    Files.createDirectories(root);
    YamlMapPersistence persistence = new YamlMapPersistence(root, Runnable::run);
    MapPoint point = new MapPoint("world", 2.5, 65, 8.5, 90, 0);
    UUID creator = UUID.randomUUID();
    MapDefinition definition =
        new MapDefinition(
            MapDefinition.CURRENT_SCHEMA,
            "crypt",
            "Crypt",
            "Test",
            creator,
            Instant.parse("2026-07-25T12:00:00Z"),
            Instant.parse("2026-07-25T12:01:00Z"),
            "world",
            "FILLED_MAP",
            "crypt.png",
            4,
            "music.crypt",
            "HARD",
            "CLASSIC",
            Optional.of(point),
            Map.of("spawn", new MapDefinition.Zone("spawn", "Spawn", "AQUA", "", "", 1, point)),
            Map.of(),
            Map.of(
                "z1",
                new MapDefinition.ZombieSpawn(
                    "z1",
                    "Zombie",
                    "spawn",
                    1,
                    4,
                    1,
                    99,
                    4,
                    80,
                    false,
                    Set.of("NORMAL"),
                    20,
                    point)),
            Map.of(
                "box",
                new MapDefinition.MapObject(
                    "box",
                    MapObjectType.MYSTERY_BOX,
                    "Box",
                    "spawn",
                    point,
                    Map.of("cost", "950"))));

    persistence.save(definition).join();
    persistence.save(definition.withDisplayName("Crypt II", Instant.now())).join();
    MapDefinition loaded = persistence.loadAll().join().iterator().next();

    assertEquals("Crypt II", loaded.displayName());
    assertEquals(1, loaded.zombieSpawns().size());
    assertEquals("950", loaded.objects().get("box").properties().get("cost"));
    assertTrue(Files.isRegularFile(root.resolve("maps/crypt/map.yml.bak")));
  }
}
