package fr.heneria.zombie.plugin.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.heneria.zombie.core.editor.MapDefinition;
import fr.heneria.zombie.core.editor.MapPublication;
import fr.heneria.zombie.core.editor.MapStatus;
import fr.heneria.zombie.core.editor.PublishedMapVersion;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class YamlMapPublicationPersistenceTest {

  @Test
  void atomicallyStoresManifestAndImmutableVersionSnapshot() throws Exception {
    Path root = Path.of("build", "test-data", "publications-" + UUID.randomUUID());
    Path templates = root.resolve("zombie_templates");
    Files.createDirectories(templates.resolve("crypt/region"));
    Files.writeString(templates.resolve("crypt/level.dat"), "world");
    Files.writeString(templates.resolve("crypt/region/r.0.0.mca"), "chunks");
    YamlMapPublicationPersistence persistence =
        new YamlMapPublicationPersistence(root, templates, Runnable::run);
    UUID actor = UUID.randomUUID();
    MapDefinition definition =
        MapDefinition.create("crypt", "Crypt", actor, Instant.EPOCH, "world");
    MapPublication publication =
        new MapPublication(
            "crypt",
            MapStatus.PUBLISHED,
            Optional.of(1),
            List.of(
                new PublishedMapVersion(1, definition, Instant.EPOCH, actor, Optional.empty())));

    persistence.save(publication).join();
    MapPublication loaded = persistence.loadAll().join().iterator().next();

    assertEquals(publication, loaded);
    assertTrue(Files.isRegularFile(root.resolve("maps/crypt/publication.yml")));
    assertTrue(Files.isRegularFile(root.resolve("maps/crypt/versions/v1.yml")));
    assertTrue(Files.isRegularFile(root.resolve("maps/crypt/world-versions/v1/region/r.0.0.mca")));

    Files.writeString(templates.resolve("crypt/region/r.0.0.mca"), "chunks-v2");
    PublishedMapVersion v2 =
        new PublishedMapVersion(
            2,
            definition.withDisplayName("Crypt II", Instant.ofEpochSecond(1)),
            Instant.ofEpochSecond(1),
            actor,
            Optional.empty());
    persistence
        .save(
            new MapPublication(
                "crypt",
                MapStatus.PUBLISHED,
                Optional.of(2),
                List.of(publication.versions().getFirst(), v2)))
        .join();
    Files.writeString(templates.resolve("crypt/region/r.0.0.mca"), "unpublished-draft");
    PublishedMapVersion rollback =
        new PublishedMapVersion(3, definition, Instant.ofEpochSecond(2), actor, Optional.of(1));
    persistence
        .save(
            new MapPublication(
                "crypt",
                MapStatus.PUBLISHED,
                Optional.of(3),
                List.of(publication.versions().getFirst(), v2, rollback)))
        .join();

    assertEquals(
        "chunks-v2",
        Files.readString(root.resolve("maps/crypt/world-versions/v2/region/r.0.0.mca")));
    assertEquals(
        "chunks", Files.readString(root.resolve("maps/crypt/world-versions/v3/region/r.0.0.mca")));
    assertEquals("chunks", Files.readString(templates.resolve("crypt/region/r.0.0.mca")));
  }
}
