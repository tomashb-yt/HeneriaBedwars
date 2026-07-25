package fr.heneria.zombie.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.heneria.zombie.core.config.ZombieSettings.ChatOptions;
import fr.heneria.zombie.core.config.ZombieSettings.DocumentationOptions;
import fr.heneria.zombie.core.config.ZombieSettings.GuiOptions;
import fr.heneria.zombie.core.config.ZombieSettings.InstanceOptions;
import fr.heneria.zombie.core.config.ZombieSettings.LobbyOptions;
import fr.heneria.zombie.core.config.ZombieSettings.LocationOptions;
import fr.heneria.zombie.core.config.ZombieSettings.PluginOptions;
import fr.heneria.zombie.core.config.ZombieSettings.ReconnectOptions;
import fr.heneria.zombie.core.config.ZombieSettings.ServerOptions;
import fr.heneria.zombie.core.config.ZombieSettings.StorageOptions;
import fr.heneria.zombie.core.config.ZombieSettings.WorldRuleOptions;
import org.junit.jupiter.api.Test;

class ZombieSettingsValidatorTest {

  private final ZombieSettingsValidator validator = new ZombieSettingsValidator();

  @Test
  void acceptsTicketDefaultsWithoutDiagnostic() {
    assertTrue(validator.validate(defaults()).isEmpty());
  }

  @Test
  void rejectsInvalidBackendCapacityAndEscapingDirectories() {
    ZombieSettings defaults = defaults();
    ZombieSettings invalid =
        new ZombieSettings(
            1,
            defaults.plugin(),
            defaults.server(),
            new StorageOptions("mysql", "../outside.db"),
            defaults.lobby(),
            new InstanceOptions("../games", "maps/sub", true, true, -1, 0, 0, true),
            defaults.chat(),
            new ReconnectOptions(true, 0, true, true),
            defaults.worldRules(),
            defaults.gui(),
            defaults.documentation());

    assertEquals(8, validator.validate(invalid).size());
    assertTrue(
        validator.validate(invalid).stream()
            .allMatch(issue -> issue.severity() == ValidationSeverity.ERROR));
  }

  static ZombieSettings defaults() {
    return new ZombieSettings(
        1,
        new PluginOptions("fr_FR", false, false),
        new ServerOptions("zombie_lobby", "world"),
        new StorageOptions("sqlite", "data.db"),
        new LobbyOptions("zombie_lobby", new LocationOptions("zombie_lobby", 0, 80, 0, 0, 0)),
        new InstanceOptions("zombie_instances", "zombie_templates", true, true, 2, 30, -1, true),
        new ChatOptions(true, true, true, true),
        new ReconnectOptions(true, 30, true, true),
        new WorldRuleOptions(false, false, false, false, false, false, false, false, true, true),
        new GuiOptions("dark", true, true),
        new DocumentationOptions(true));
  }
}
