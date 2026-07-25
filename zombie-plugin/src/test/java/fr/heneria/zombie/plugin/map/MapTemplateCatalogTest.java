package fr.heneria.zombie.plugin.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.heneria.zombie.core.config.ZombieSettingsValidator;
import fr.heneria.zombie.core.map.MapTemplateDefinition;
import fr.heneria.zombie.plugin.config.ConfigurationManager;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;

class MapTemplateCatalogTest {

  @Test
  void createsTheConfiguredTemplateRootDuringFirstLookup() throws Exception {
    Path root = Path.of("build", "test-world", UUID.randomUUID().toString());
    ConfigurationManager configurations =
        new ConfigurationManager(
            root.resolve("plugins").resolve("HeneriaZombie"),
            getClass().getClassLoader(),
            new ZombieSettingsValidator());
    configurations.initialize();
    MapTemplateCatalog catalog = new MapTemplateCatalog(root, configurations, Runnable::run);

    assertTrue(catalog.find("missing").join().isEmpty());
    assertTrue(Files.isDirectory(root.resolve("zombie_templates")));
  }

  @Test
  void discoversVanillaWorldAndReadsSpawnWithoutMetadataFile() throws Exception {
    Path root = Path.of("build", "test-world", UUID.randomUUID().toString());
    ConfigurationManager configurations =
        new ConfigurationManager(
            root.resolve("plugins").resolve("HeneriaZombie"),
            getClass().getClassLoader(),
            new ZombieSettingsValidator());
    configurations.initialize();
    Path mapDirectory = root.resolve("zombie_templates").resolve("crypt");
    Files.createDirectories(mapDirectory);
    writeLevelDat(mapDirectory.resolve("level.dat"), 12, 72, -4);
    MapTemplateCatalog catalog = new MapTemplateCatalog(root, configurations, Runnable::run);

    assertEquals(java.util.List.of("crypt"), catalog.discover().join());
    MapTemplateDefinition definition = catalog.find("crypt").join().orElseThrow();
    assertEquals(4, definition.maximumPlayers());
    assertEquals(12.5D, definition.spawn().x());
    assertEquals(72.0D, definition.spawn().y());
    assertEquals(-3.5D, definition.spawn().z());
  }

  private static void writeLevelDat(Path target, int x, int y, int z) throws Exception {
    try (DataOutputStream output =
        new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(target)))) {
      output.writeByte(10);
      output.writeUTF("");
      output.writeByte(10);
      output.writeUTF("Data");
      writeIntTag(output, "SpawnX", x);
      writeIntTag(output, "SpawnY", y);
      writeIntTag(output, "SpawnZ", z);
      output.writeByte(0);
      output.writeByte(0);
    }
  }

  private static void writeIntTag(DataOutputStream output, String name, int value)
      throws Exception {
    output.writeByte(3);
    output.writeUTF(name);
    output.writeInt(value);
  }
}
