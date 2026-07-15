package fr.heneria.bedwars.plugin.config;

import fr.heneria.bedwars.core.config.ConfigurationDocument;
import fr.heneria.bedwars.core.config.ConfigurationId;
import fr.heneria.bedwars.core.config.ConfigurationProblem;
import fr.heneria.bedwars.core.config.ConfigurationSnapshot;
import fr.heneria.bedwars.core.config.GameplaySettings;
import fr.heneria.bedwars.core.config.LobbySettings;
import fr.heneria.bedwars.core.config.MenuSettings;
import fr.heneria.bedwars.core.config.PluginSettings;
import fr.heneria.bedwars.core.config.ProblemSeverity;
import fr.heneria.bedwars.core.config.SoundSettings;
import fr.heneria.bedwars.core.config.StorageSettings;
import fr.heneria.bedwars.core.config.TranslationBundle;
import fr.heneria.bedwars.core.config.TranslationKey;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;

/** Validates raw documents and converts them into one typed immutable snapshot. */
public final class ConfigurationSnapshotFactory {
  private static final int CURRENT_VERSION = 1;
  private final Clock clock;

  public ConfigurationSnapshotFactory(Clock clock) {
    this.clock = clock;
  }

  public ConfigurationSnapshot create(
      Map<ConfigurationId, ConfigurationDocument> documents,
      Map<String, ConfigurationDocument> languageDocuments,
      List<ConfigurationProblem> existing) {
    List<ConfigurationProblem> problems = new ArrayList<>(existing);
    documents.values().forEach(document -> validateVersion(document, problems));
    languageDocuments.values().forEach(document -> validateVersion(document, problems));

    Reader general = new Reader(documents.get(ConfigurationId.GENERAL), problems);
    String locale = general.string("plugin.language", "fr_FR");
    PluginSettings plugin =
        new PluginSettings(
            locale,
            general.bool("plugin.debug", false),
            general.bool("plugin.check-updates", false),
            general.bool("security.confirm-dangerous-actions", true),
            general.bool("security.prevent-reload-during-games", true),
            general.integer(
                "performance.warn-main-thread-task-duration-ms", 50, 1, Integer.MAX_VALUE));

    Reader gameplay = new Reader(documents.get(ConfigurationId.GAMEPLAY), problems);
    GameplaySettings gameplaySettings =
        new GameplaySettings(
            gameplay.string("combat.profile", "legacy_1_8"),
            gameplay.bool("combat.attack-cooldown-enabled", false),
            gameplay.bool("combat.shields-enabled", false),
            gameplay.bool("combat.friendly-fire", false),
            gameplay.bool("respawn.enabled", true),
            gameplay.integer("respawn.delay-seconds", 5, 0, Integer.MAX_VALUE),
            gameplay.integer("respawn.protection-seconds", 3, 0, Integer.MAX_VALUE),
            gameplay.integer("void.minimum-y", -64, Integer.MIN_VALUE, Integer.MAX_VALUE),
            gameplay.bool("blocks.break-only-player-placed", true),
            gameplay.bool("blocks.restore-after-game", true));

    Reader lobby = new Reader(documents.get(ConfigurationId.LOBBY), problems);
    LobbySettings lobbySettings =
        new LobbySettings(
            lobby.bool("main-lobby.configured", false),
            lobby.string("main-lobby.world", ""),
            lobby.decimal("main-lobby.x", 0),
            lobby.decimal("main-lobby.y", 0),
            lobby.decimal("main-lobby.z", 0),
            (float) lobby.decimal("main-lobby.yaw", 0),
            (float) lobby.decimal("main-lobby.pitch", 0),
            lobby.bool("protection.cancel-block-break", true),
            lobby.bool("protection.cancel-block-place", true),
            lobby.bool("protection.cancel-damage", true),
            lobby.bool("protection.cancel-hunger", true),
            lobby.bool("items.enabled", true));

    Reader storage = new Reader(documents.get(ConfigurationId.STORAGE), problems);
    String storageType = storage.string("storage.type", "sqlite").toLowerCase(Locale.ROOT);
    if (!Set.of("sqlite", "mysql").contains(storageType)) {
      storage.problem(
          "storage.type", storageType, "sqlite or mysql", "sqlite", "Unsupported storage type");
      storageType = "sqlite";
    }
    StorageSettings storageSettings =
        new StorageSettings(
            storageType,
            storage.string("sqlite.file", "data.db"),
            storage.string("mysql.host", "localhost"),
            storage.integer("mysql.port", 3306, 1, 65535),
            storage.string("mysql.database", "heneriabedwars"),
            storage.string("mysql.username", "root"),
            storage.string("mysql.password", ""),
            storage.bool("mysql.use-ssl", false),
            storage.integer("mysql.connection-timeout-ms", 10000, 1, Integer.MAX_VALUE),
            storage.bool("redis.enabled", false),
            storage.string("redis.host", "localhost"),
            storage.integer("redis.port", 6379, 1, 65535));

    Reader menus = new Reader(documents.get(ConfigurationId.MENUS), problems);
    int menuSize = menus.integer("global.default-size", 54, 9, 54);
    if (menuSize % 9 != 0) {
      menus.problem(
          "global.default-size",
          menuSize,
          "multiple of 9 between 9 and 54",
          54,
          "Invalid menu size");
      menuSize = 54;
    }
    MenuSettings menuSettings =
        new MenuSettings(
            menuSize,
            menus.bool("global.fill-empty-slots", true),
            menus.bool("global.close-on-critical-error", true),
            menus.bool("global.play-click-sounds", true),
            menus.integer("pagination.previous-slot", 45, 0, menuSize - 1),
            menus.integer("pagination.next-slot", 53, 0, menuSize - 1),
            menus.integer("pagination.back-slot", 49, 0, menuSize - 1),
            menus.integer("pagination.page-indicator-slot", 50, 0, menuSize - 1),
            menus.bool("navigation.history-enabled", true),
            menus.integer("navigation.max-history-size", 20, 0, 100),
            menus.integer("interaction.default-click-cooldown-ms", 150, 0, 60_000),
            menus.bool("interaction.cancel-player-inventory-clicks", true),
            menus.bool("interaction.cancel-drag-events", true),
            menus.bool("refresh.enabled", true),
            menus.integer("refresh.minimum-interval-ticks", 10, 1, 72_000),
            menus.bool("sounds.enabled", true),
            Map.of(
                "open", sound(menus, "open", "UI_BUTTON_CLICK", problems),
                "click", sound(menus, "click", "UI_BUTTON_CLICK", problems),
                "success", sound(menus, "success", "ENTITY_PLAYER_LEVELUP", problems),
                "error", sound(menus, "error", "ENTITY_VILLAGER_NO", problems),
                "back", sound(menus, "back", "UI_BUTTON_CLICK", problems),
                "close", sound(menus, "close", "UI_BUTTON_CLICK", problems)));

    validateItems(documents.get(ConfigurationId.ITEMS), problems);
    Map<String, TranslationBundle> languages = languages(languageDocuments, problems);
    if (!languages.containsKey(locale)) {
      problems.add(
          new ConfigurationProblem(
              ProblemSeverity.ERROR,
              "config.yml",
              "plugin.language",
              locale,
              "an existing locale",
              "active snapshot unchanged",
              "Unknown locale"));
    }
    return new ConfigurationSnapshot(
        plugin,
        gameplaySettings,
        lobbySettings,
        storageSettings,
        menuSettings,
        documents,
        languages,
        problems,
        Instant.now(clock));
  }

  private static void validateVersion(
      ConfigurationDocument document, List<ConfigurationProblem> problems) {
    if (document.version() != CURRENT_VERSION)
      problems.add(
          new ConfigurationProblem(
              ProblemSeverity.ERROR,
              document.fileName(),
              "config-version",
              document.version(),
              "1",
              "none",
              document.version() < 1
                  ? "Missing configuration version"
                  : "Unsupported configuration version"));
  }

  private static void validateItems(
      ConfigurationDocument document, List<ConfigurationProblem> problems) {
    Reader items = new Reader(document, problems);
    String material = items.string("items.menu-border.material", "GRAY_STAINED_GLASS_PANE");
    if (Material.matchMaterial(material) == null)
      items.problem(
          "items.menu-border.material",
          material,
          "a valid Minecraft material",
          "GRAY_STAINED_GLASS_PANE",
          "Invalid material");
    items.integer("items.menu-border.amount", 1, 1, 99);
  }

  private static SoundSettings sound(
      Reader reader, String id, String fallback, List<ConfigurationProblem> problems) {
    String path = "sounds." + id;
    String value = reader.string(path + ".sound", fallback);
    try {
      org.bukkit.Sound.valueOf(value);
    } catch (IllegalArgumentException exception) {
      reader.problem(path + ".sound", value, "valid Bukkit sound", fallback, "Invalid sound");
      value = fallback;
    }
    float volume = (float) reader.decimal(path + ".volume", 1.0);
    float pitch = (float) reader.decimal(path + ".pitch", 1.0);
    if (volume < 0) {
      reader.problem(path + ".volume", volume, ">= 0", 1.0, "Invalid volume");
      volume = 1;
    }
    if (pitch < 0 || pitch > 2) {
      reader.problem(path + ".pitch", pitch, "between 0 and 2", 1.0, "Invalid pitch");
      pitch = 1;
    }
    return new SoundSettings(value, volume, pitch);
  }

  private static Map<String, TranslationBundle> languages(
      Map<String, ConfigurationDocument> documents, List<ConfigurationProblem> problems) {
    Map<String, TranslationBundle> bundles = new LinkedHashMap<>();
    for (Map.Entry<String, ConfigurationDocument> entry : documents.entrySet()) {
      Map<String, String> messages = new LinkedHashMap<>();
      entry
          .getValue()
          .values()
          .forEach(
              (key, value) -> {
                if (!key.equals("config-version") && value instanceof String text)
                  messages.put(key, text);
              });
      for (TranslationKey key : TranslationKey.values()) {
        if (!messages.containsKey(key.path()))
          problems.add(
              new ConfigurationProblem(
                  ProblemSeverity.ERROR,
                  entry.getValue().fileName(),
                  key.path(),
                  null,
                  "a translated message",
                  "none",
                  "Missing translation"));
      }
      bundles.put(
          entry.getKey(),
          new TranslationBundle(entry.getKey(), entry.getValue().version(), messages));
    }
    if (bundles.containsKey("fr_FR")
        && bundles.containsKey("en_US")
        && !bundles.get("fr_FR").keys().equals(bundles.get("en_US").keys())) {
      problems.add(
          new ConfigurationProblem(
              ProblemSeverity.ERROR,
              "languages",
              "translation-keys",
              "different",
              "identical fr_FR/en_US key sets",
              "none",
              "Language key sets differ"));
    }
    return bundles;
  }

  private static final class Reader {
    private final ConfigurationDocument document;
    private final List<ConfigurationProblem> problems;

    Reader(ConfigurationDocument document, List<ConfigurationProblem> problems) {
      this.document = document;
      this.problems = problems;
    }

    String string(String key, String fallback) {
      Object value = document.value(key);
      if (value instanceof String text) return text;
      problem(key, value, "string", fallback, "Missing or invalid value");
      return fallback;
    }

    boolean bool(String key, boolean fallback) {
      Object value = document.value(key);
      if (value instanceof Boolean result) return result;
      problem(key, value, "boolean", fallback, "Missing or invalid value");
      return fallback;
    }

    int integer(String key, int fallback, int minimum, int maximum) {
      Object value = document.value(key);
      if (value instanceof Number number
          && number.longValue() >= minimum
          && number.longValue() <= maximum) {
        return number.intValue();
      }
      problem(
          key,
          value,
          "integer between " + minimum + " and " + maximum,
          fallback,
          "Out-of-range value");
      return fallback;
    }

    double decimal(String key, double fallback) {
      Object value = document.value(key);
      if (value instanceof Number number) return number.doubleValue();
      problem(key, value, "number", fallback, "Missing or invalid value");
      return fallback;
    }

    void problem(String key, Object value, Object expected, Object fallback, String message) {
      problems.add(
          new ConfigurationProblem(
              ProblemSeverity.WARNING,
              document.fileName(),
              key,
              String.valueOf(value),
              String.valueOf(expected),
              String.valueOf(fallback),
              message));
    }
  }
}
