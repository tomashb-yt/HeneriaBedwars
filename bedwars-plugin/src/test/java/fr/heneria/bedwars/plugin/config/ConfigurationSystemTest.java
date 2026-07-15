package fr.heneria.bedwars.plugin.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.heneria.bedwars.core.config.ConfigurationId;
import fr.heneria.bedwars.core.config.ConfigurationReloadResult;
import fr.heneria.bedwars.core.logging.ProjectLogger;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfigurationSystemTest {
  private Path temporary;

  @BeforeEach
  void createWorkspaceTemporaryDirectory() throws IOException {
    temporary = Path.of("build", "test-work", UUID.randomUUID().toString());
    Files.createDirectories(temporary);
  }

  @Test
  void createsEveryMissingFileAndPreservesExistingContent() throws Exception {
    TestLogger logger = new TestLogger();
    DefaultConfigurationInstaller installer = installer(logger);
    assertEquals(11, installer.installMissing());
    Path config = temporary.resolve("config.yml");
    Files.writeString(config, "custom: true\n");
    assertEquals(0, installer.installMissing());
    assertEquals("custom: true\n", Files.readString(config));
    assertTrue(Files.isDirectory(temporary.resolve("arenas")));
    assertTrue(Files.isDirectory(temporary.resolve("languages")));
    assertTrue(Files.isDirectory(temporary.resolve("backups")));
  }

  @Test
  void loadsValidFilesVersionsTypedValuesAndBothLanguages() throws Exception {
    ConfigurationService service = service();
    service.initialize();
    assertEquals("fr_FR", service.snapshot().plugin().locale());
    assertEquals(5, service.snapshot().gameplay().respawnDelaySeconds());
    assertEquals(54, service.snapshot().menus().defaultSize());
    assertEquals(1, service.registry().version(ConfigurationId.GENERAL));
    assertEquals(List.of("en_US", "fr_FR"), service.availableLocales());
    assertEquals(
        service.snapshot().languages().get("fr_FR").keys(),
        service.snapshot().languages().get("en_US").keys());
  }

  @Test
  void appliesDefaultsAndReportsInvalidPortDelayMenuAndMaterial() throws Exception {
    ConfigurationService service = service();
    service.initialize();
    replace("storage.yml", "port: 3306", "port: 70000");
    replace("gameplay.yml", "delay-seconds: 5", "delay-seconds: -5");
    replace("menus.yml", "default-size: 54", "default-size: 10");
    replace("items.yml", "GRAY_STAINED_GLASS_PANE", "NOT_A_MATERIAL");
    ConfigurationReloadResult result = service.reloadAll();
    assertTrue(result.successful());
    assertTrue(result.warnings() >= 4);
    assertEquals(3306, service.snapshot().storage().mysqlPort());
    assertEquals(5, service.snapshot().gameplay().respawnDelaySeconds());
    assertEquals(54, service.snapshot().menus().defaultSize());
  }

  @Test
  void missingLocaleUsesDocumentedDefaultWithWarning() throws Exception {
    ConfigurationService service = service();
    service.initialize();
    replace("config.yml", "  language: fr_FR\n", "");
    ConfigurationReloadResult result = service.reloadAll();
    assertTrue(result.successful());
    assertEquals("fr_FR", service.snapshot().plugin().locale());
    assertTrue(result.warnings() > 0);
  }

  @Test
  void invalidYamlPreservesTheCompletePreviousSnapshot() throws Exception {
    ConfigurationService service = service();
    service.initialize();
    Instant previous = service.snapshot().loadedAt();
    String locale = service.snapshot().plugin().locale();
    Files.writeString(temporary.resolve("gameplay.yml"), "broken: [yaml\n");
    ConfigurationReloadResult result = service.reloadAll();
    assertFalse(result.successful());
    assertTrue(result.errors() > 0);
    assertEquals(previous, service.snapshot().loadedAt());
    assertEquals(locale, service.snapshot().plugin().locale());
  }

  @Test
  void unknownLanguageIsRefusedAndKnownLanguagePersists() throws Exception {
    ConfigurationService service = service();
    service.initialize();
    assertFalse(service.setLanguage("xx_XX").successful());
    assertEquals("fr_FR", service.snapshot().plugin().locale());
    assertTrue(service.setLanguage("en_US").successful());
    assertEquals("en_US", service.snapshot().plugin().locale());
    assertTrue(Files.readString(temporary.resolve("config.yml")).contains("language: en_US"));
  }

  @Test
  void missingTranslationKeyRejectsReload() throws Exception {
    ConfigurationService service = service();
    service.initialize();
    Path english = temporary.resolve("languages/en_US.yml");
    String yaml =
        Files.readString(english)
            .replace("    unknown: \"<red>Unknown language: <white>{language}\"\n", "");
    Files.writeString(english, yaml);
    assertFalse(service.reloadAll().successful());
  }

  @Test
  void backupCreatesValidNonOverwritingPaths() throws Exception {
    TestLogger logger = new TestLogger();
    Path source = temporary.resolve("config.yml");
    Files.writeString(source, "config-version: 1\n");
    BackupService backups =
        new BackupService(
            temporary.resolve("backups"),
            Clock.fixed(Instant.parse("2026-07-15T18:30:00Z"), ZoneOffset.UTC),
            logger);
    Path first = backups.backup(source);
    Path second = backups.backup(source);
    assertTrue(Files.exists(first));
    assertTrue(Files.exists(second));
    assertNotEquals(first, second);
  }

  private ConfigurationService service() {
    return new ConfigurationService(
        temporary, ConfigurationSystemTest::resource, new TestLogger(), Clock.systemUTC());
  }

  private DefaultConfigurationInstaller installer(TestLogger logger) {
    return new DefaultConfigurationInstaller(temporary, ConfigurationSystemTest::resource, logger);
  }

  private void replace(String file, String target, String replacement) throws IOException {
    Path path = temporary.resolve(file);
    Files.writeString(path, Files.readString(path).replace(target, replacement));
  }

  private static InputStream resource(String name) throws IOException {
    return Files.newInputStream(Path.of("src", "main", "resources", name));
  }

  private static final class TestLogger implements ProjectLogger {
    private final List<String> messages = new ArrayList<>();

    public void info(String message) {
      messages.add(message);
    }

    public void warning(String message) {
      messages.add(message);
    }

    public void error(String message, Throwable cause) {
      messages.add(message);
    }

    public void debug(String message) {
      messages.add(message);
    }
  }
}
