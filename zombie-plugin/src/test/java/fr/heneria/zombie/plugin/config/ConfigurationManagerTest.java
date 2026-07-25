package fr.heneria.zombie.plugin.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.heneria.zombie.core.config.ZombieSettingsValidator;
import fr.heneria.zombie.plugin.message.MessageService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class ConfigurationManagerTest {

  private final Path dataDirectory = Path.of("build", "test-data", UUID.randomUUID().toString());

  @Test
  void installsAndLoadsTicketDefaults() throws Exception {
    ConfigurationManager manager =
        new ConfigurationManager(
            dataDirectory, getClass().getClassLoader(), new ZombieSettingsValidator());

    assertTrue(manager.initialize().isEmpty());
    assertTrue(Files.isRegularFile(dataDirectory.resolve("config.yml")));
    assertTrue(Files.isRegularFile(dataDirectory.resolve("messages.yml")));
    assertEquals("fr_FR", manager.current().settings().plugin().language());
    assertEquals(-1, manager.current().settings().instances().maximumConcurrentGames());
    assertEquals("data.db", manager.current().settings().storage().sqliteFile());
    assertTrue(manager.current().messages().containsKey("command.instance-created"));

    MessageService messages = new MessageService(manager);
    String help =
        PlainTextComponentSerializer.plainText().serialize(messages.render("command.help"));
    String usage =
        PlainTextComponentSerializer.plainText().serialize(messages.render("command.usage"));
    assertTrue(help.contains("/zombie instance join <id>"));
    assertTrue(usage.contains("<create|list|join|leave|stop|info>"));
  }

  @Test
  void invalidReloadKeepsLastValidatedSnapshot() throws Exception {
    ConfigurationManager manager =
        new ConfigurationManager(
            dataDirectory, getClass().getClassLoader(), new ZombieSettingsValidator());
    manager.initialize();
    Files.writeString(
        dataDirectory.resolve("config.yml"),
        """
        config-version: 1
        plugin:
          language: fr_FR
        server:
          lobby-world: zombie_lobby
          fallback-world: world
        storage:
          type: sqlite
          sqlite-file: ../outside.db
        instances:
          maximum-concurrent-games: 0
        gui:
          default-theme: dark
        documentation:
          require-context-update: true
        """);

    ReloadResult result = manager.reload();

    assertFalse(result.successful());
    assertEquals("data.db", manager.current().settings().storage().sqliteFile());
    assertEquals(-1, manager.current().settings().instances().maximumConcurrentGames());
  }
}

