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
    Path worldContainer = root.resolve("server");
    Path templates = worldContainer.resolve("zombie_templates");
    YamlMapPersistence persistence =
        new YamlMapPersistence(root, templates, worldContainer, Runnable::run);
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
            1,
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

  @Test
  void deletionRemovesAllOwnedArtifactsButPreservesAnExternalWorld() throws Exception {
    Path root = Path.of("build", "test-data", "map-delete-" + UUID.randomUUID());
    Path worldContainer = root.resolve("server");
    Path templates = worldContainer.resolve("zombie_templates");
    Path externalWorld = worldContainer.resolve("world");
    Files.createDirectories(templates.resolve("crypt/region"));
    Files.createDirectories(externalWorld);
    Files.writeString(templates.resolve("crypt/region/r.0.0.mca"), "chunks");
    Files.writeString(externalWorld.resolve("level.dat"), "external");
    YamlMapPersistence persistence =
        new YamlMapPersistence(root, templates, worldContainer, Runnable::run);
    MapDefinition definition =
        MapDefinition.create("crypt", "Crypt", UUID.randomUUID(), Instant.EPOCH, "world");
    persistence.save(definition).join();
    Files.createDirectories(root.resolve("maps/crypt/world-versions/v1"));
    Files.writeString(root.resolve("maps/crypt/world-versions/v1/level.dat"), "snapshot");

    persistence.delete(definition).join();

    assertTrue(Files.notExists(root.resolve("maps/crypt")));
    assertTrue(Files.notExists(templates.resolve("crypt")));
    assertTrue(Files.isRegularFile(externalWorld.resolve("level.dat")));
  }

  @Test
  void deletionRemovesTheOwnedEditingWorldOfADuplicatedMap() throws Exception {
    Path root = Path.of("build", "test-data", "owned-world-delete-" + UUID.randomUUID());
    Path worldContainer = root.resolve("server");
    Path templates = worldContainer.resolve("zombie_templates");
    Path editingWorld = worldContainer.resolve("zombie_editing/hz_edit_crypt");
    Files.createDirectories(editingWorld);
    Files.writeString(editingWorld.resolve("level.dat"), "editing");
    YamlMapPersistence persistence =
        new YamlMapPersistence(root, templates, worldContainer, Runnable::run);
    MapDefinition definition =
        MapDefinition.create(
            "crypt", "Crypt", UUID.randomUUID(), Instant.EPOCH, "zombie_editing/hz_edit_crypt");
    persistence.save(definition).join();

    persistence.delete(definition).join();

    assertTrue(Files.notExists(editingWorld));
    assertTrue(Files.notExists(root.resolve("maps/crypt")));
  }
}
