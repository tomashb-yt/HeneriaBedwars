package fr.heneria.bedwars.plugin.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.heneria.bedwars.core.map.MapId;
import fr.heneria.bedwars.core.map.MapTemplate;
import fr.heneria.bedwars.core.map.MapType;
import fr.heneria.bedwars.core.map.MapWorldSettings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MapStorageTest {
  private static final Instant NOW = Instant.parse("2026-07-16T20:00:00Z");
  private Path root;

  @BeforeEach
  void createRoot() throws IOException {
    root = Path.of("build", "test-work", UUID.randomUUID().toString()).toAbsolutePath();
    Files.createDirectories(root);
  }

  @Test
  void yamlRepositoryRoundTripsMetadataAndDeletesOnlyExpectedFile() throws Exception {
    YamlMapTemplateRepository repository =
        new YamlMapTemplateRepository(root.resolve("metadata"), clock());
    MapTemplate original = template("desert").withDisplayName("<gold>Desert", NOW.plusSeconds(1));

    repository.save(original);
    MapTemplate loaded = repository.load(MapId.parse("DESERT")).orElseThrow();

    assertEquals(original.id(), loaded.id());
    assertEquals(original.displayName(), loaded.displayName());
    assertEquals(original.revision(), loaded.revision());
    assertEquals(original.spawn(), loaded.spawn());
    assertEquals(1, repository.loadAll().templates().size());
    assertFalse(Files.exists(root.resolve("metadata/desert.yml.tmp")));
    repository.delete(original.id());
    assertFalse(repository.exists(original.id()));
  }

  @Test
  void invalidMetadataIsIsolatedWithoutHidingValidTemplates() throws Exception {
    YamlMapTemplateRepository repository =
        new YamlMapTemplateRepository(root.resolve("metadata"), clock());
    repository.save(template("valid"));
    Files.writeString(root.resolve("metadata/broken.yml"), "broken: [yaml\n");

    var loaded = repository.loadAll();

    assertEquals(1, loaded.templates().size());
    assertEquals(1, loaded.failures().size());
  }

  @Test
  void duplicateExcludesWorldIdentityAndPlayerDataThenPublishesByRename() throws Exception {
    Path worlds = root.resolve("worlds");
    Path source = worlds.resolve("hbw_template_desert");
    Files.createDirectories(source.resolve("region"));
    Files.createDirectories(source.resolve("playerdata"));
    Files.writeString(source.resolve("region/r.0.0.mca"), "blocks");
    Files.writeString(source.resolve("uid.dat"), "uuid");
    Files.writeString(source.resolve("session.lock"), "lock");
    Files.writeString(source.resolve("playerdata/player.dat"), "player");
    SecureMapFileService files = fileService(worlds);

    files.duplicate(template("desert"), template("copy"));

    Path copy = worlds.resolve("hbw_template_copy");
    assertEquals("blocks", Files.readString(copy.resolve("region/r.0.0.mca")));
    assertFalse(Files.exists(copy.resolve("uid.dat")));
    assertFalse(Files.exists(copy.resolve("session.lock")));
    assertFalse(Files.exists(copy.resolve("playerdata")));
    assertFalse(Files.exists(worlds.resolve(".hbw_template_copy.tmp")));
  }

  @Test
  void backupContainsWorldMetadataAndManifestBeforeDeletion() throws Exception {
    Path worlds = root.resolve("worlds");
    Files.createDirectories(worlds.resolve("hbw_template_desert"));
    Files.writeString(worlds.resolve("hbw_template_desert/level.dat"), "level");
    Files.createDirectories(root.resolve("metadata"));
    Files.writeString(root.resolve("metadata/desert.yml"), "id: desert\n");
    SecureMapFileService files = fileService(worlds);

    files.backup(template("desert"), "DELETE", "0.1.0");

    try (var stream = Files.walk(root.resolve("backups"))) {
      var names =
          stream.filter(Files::isRegularFile).map(path -> path.getFileName().toString()).toList();
      assertTrue(names.containsAll(Set.of("level.dat", "metadata.yml", "manifest.yml")));
    }
  }

  @Test
  void unsafeExternalWorldNameIsRejectedBeforeDeletion() throws Exception {
    SecureMapFileService files = fileService(root.resolve("worlds"));
    MapTemplate unsafe =
        new MapTemplate(
            template("safe").configVersion(),
            1,
            MapId.parse("safe"),
            "safe",
            MapType.GENERIC,
            fr.heneria.bedwars.core.map.MapState.UNLOADED,
            "safe",
            "../outside",
            "NORMAL",
            fr.heneria.bedwars.core.map.MapGeneratorType.VOID,
            new fr.heneria.bedwars.core.map.MapSpawn(false, "../outside", 0.5, 65, 0.5, 0, 0),
            new MapWorldSettings(false, false, false, 6000, true),
            NOW,
            NOW,
            java.util.Optional.empty(),
            java.util.Optional.empty(),
            "",
            "",
            Set.of(),
            Set.of(),
            false,
            java.util.Optional.empty());
    assertThrows(IllegalArgumentException.class, () -> files.deleteTemplate(unsafe));
  }

  private SecureMapFileService fileService(Path worlds) {
    return new SecureMapFileService(
        root.resolve("markers"),
        root.resolve("metadata"),
        worlds,
        root.resolve("backups"),
        Set.of("uid.dat", "session.lock"),
        Set.of("playerdata", "stats", "advancements"),
        clock());
  }

  private static MapTemplate template(String id) {
    return MapTemplate.create(
        MapId.parse(id),
        MapType.BEDWARS,
        "hbw_template_" + id,
        "NORMAL",
        new MapWorldSettings(false, false, false, 6000, true),
        64,
        "Admin",
        NOW);
  }

  private static Clock clock() {
    return Clock.fixed(NOW, ZoneOffset.UTC);
  }
}
