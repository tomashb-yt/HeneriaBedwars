package fr.heneria.zombie.plugin.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class ZombieDefinitionLoaderTest {

  @Test
  void loadsValidDefinition() {
    var definition = ZombieDefinitionLoader.parse(yaml(20, "ZOMBIE"), "test.yml");
    assertEquals("test_zombie", definition.id());
    assertEquals(20, definition.attributes().healthBase());
  }

  @Test
  void rejectsUnknownEntityAndNegativeHealth() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ZombieDefinitionLoader.parse(yaml(20, "NOT_AN_ENTITY"), "unknown.yml"));
    assertThrows(
        IllegalArgumentException.class,
        () -> ZombieDefinitionLoader.parse(yaml(-1, "ZOMBIE"), "negative.yml"));
  }

  private static YamlConfiguration yaml(double health, String entityType) {
    return YamlConfiguration.loadConfiguration(
        new StringReader(
            """
            schema-version: 1
            id: test_zombie
            entity:
              type: %s
            attributes:
              health:
                base: %s
            spawn-rules:
              minimum-round: 1
              weight: 10
            """
                .formatted(entityType, health)));
  }
}
