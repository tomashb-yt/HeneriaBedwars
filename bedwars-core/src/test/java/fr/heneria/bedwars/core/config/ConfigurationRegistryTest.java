package fr.heneria.bedwars.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConfigurationRegistryTest {
  @Test
  void activatesCompleteCandidateAndExposesVersions() {
    ConfigurationRegistry registry = new ConfigurationRegistry();
    registerAll(registry);
    registry.activate(completeDocuments(1));
    assertEquals(9, registry.loaded().size());
    assertEquals(1, registry.version(ConfigurationId.GENERAL));
    assertTrue(registry.find(ConfigurationId.MENUS).isPresent());
  }

  @Test
  void rejectsIncompleteCandidateWithoutPartialReplacement() {
    ConfigurationRegistry registry = new ConfigurationRegistry();
    registerAll(registry);
    registry.activate(completeDocuments(1));
    Map<ConfigurationId, ConfigurationDocument> incomplete = completeDocuments(2);
    incomplete.remove(ConfigurationId.MENUS);
    assertThrows(IllegalArgumentException.class, () -> registry.activate(incomplete));
    assertEquals(1, registry.version(ConfigurationId.GENERAL));
  }

  @Test
  void redactsSensitiveValues() {
    ConfigurationProblem problem =
        new ConfigurationProblem(
            ProblemSeverity.ERROR,
            "storage.yml",
            "mysql.password",
            "secret-value",
            "string",
            "none",
            "bad");
    assertEquals("<redacted>", problem.receivedValue());
    assertTrue(!problem.toLogMessage().contains("secret-value"));
  }

  @Test
  void refusesDuplicateRegistration() {
    ConfigurationRegistry registry = new ConfigurationRegistry();
    registry.register(ConfigurationId.GENERAL);
    assertThrows(IllegalArgumentException.class, () -> registry.register(ConfigurationId.GENERAL));
  }

  private static void registerAll(ConfigurationRegistry registry) {
    for (ConfigurationId id : ConfigurationId.values()) registry.register(id);
  }

  private static Map<ConfigurationId, ConfigurationDocument> completeDocuments(int version) {
    Map<ConfigurationId, ConfigurationDocument> documents = new EnumMap<>(ConfigurationId.class);
    for (ConfigurationId id : ConfigurationId.values()) {
      documents.put(id, new ConfigurationDocument(id.name(), id.fileName(), version, Map.of()));
    }
    return documents;
  }
}
