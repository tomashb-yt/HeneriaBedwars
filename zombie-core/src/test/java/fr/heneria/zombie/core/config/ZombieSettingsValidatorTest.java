package fr.heneria.zombie.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.heneria.zombie.core.config.ZombieSettings.DocumentationOptions;
import fr.heneria.zombie.core.config.ZombieSettings.GuiOptions;
import fr.heneria.zombie.core.config.ZombieSettings.InstanceOptions;
import fr.heneria.zombie.core.config.ZombieSettings.PluginOptions;
import fr.heneria.zombie.core.config.ZombieSettings.ServerOptions;
import fr.heneria.zombie.core.config.ZombieSettings.StorageOptions;
import org.junit.jupiter.api.Test;

class ZombieSettingsValidatorTest {

  private final ZombieSettingsValidator validator = new ZombieSettingsValidator();

  @Test
  void acceptsTicketDefaultsWithoutDiagnostic() {
    assertTrue(validator.validate(defaults()).isEmpty());
  }

  @Test
  void rejectsInvalidBackendCapacityAndEscapingPath() {
    ZombieSettings invalid =
        new ZombieSettings(
            1,
            defaults().plugin(),
            defaults().server(),
            new StorageOptions("mysql", "../outside.db"),
            new InstanceOptions(0, true),
            defaults().gui(),
            defaults().documentation());

    assertEquals(3, validator.validate(invalid).size());
    assertTrue(
        validator.validate(invalid).stream()
            .allMatch(issue -> issue.severity() == ValidationSeverity.ERROR));
  }

  private static ZombieSettings defaults() {
    return new ZombieSettings(
        1,
        new PluginOptions("fr_FR", false, false),
        new ServerOptions("zombie_lobby", "world"),
        new StorageOptions("sqlite", "data.db"),
        new InstanceOptions(-1, true),
        new GuiOptions("dark", true, true),
        new DocumentationOptions(true));
  }
}
