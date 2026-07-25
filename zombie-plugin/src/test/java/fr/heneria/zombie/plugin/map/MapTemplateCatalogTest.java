package fr.heneria.zombie.plugin.map;

import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.heneria.zombie.core.config.ZombieSettingsValidator;
import fr.heneria.zombie.plugin.config.ConfigurationManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
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
}
