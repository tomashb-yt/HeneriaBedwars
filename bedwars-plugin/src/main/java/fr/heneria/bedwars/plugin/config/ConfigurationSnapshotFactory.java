package fr.heneria.bedwars.plugin.config;

import fr.heneria.bedwars.core.config.ArenaEditorSettings;
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
import fr.heneria.bedwars.core.config.TextInputSettings;
import fr.heneria.bedwars.core.config.TranslationBundle;
import fr.heneria.bedwars.core.config.TranslationKey;
import fr.heneria.bedwars.core.config.WorldManagerSettings;
import fr.heneria.bedwars.core.item.ItemRegistry;
import fr.heneria.bedwars.plugin.item.ItemDefinitionLoader;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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

    Reader worldReader = new Reader(documents.get(ConfigurationId.WORLDS), problems);
    WorldManagerSettings worldSettings = worldManager(worldReader);

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
                "close", sound(menus, "close", "UI_BUTTON_CLICK", problems)),
            arenaEditor(menus),
            new TextInputSettings(
                java.time.Duration.ofSeconds(
                    menus.integer("text-input.timeout-seconds", 60, 5, 600)),
                menus.stringSet(
                    "text-input.cancel-keywords", java.util.Set.of("annuler", "cancel"))));

    Map<String, TranslationBundle> languages = languages(languageDocuments, problems);
    ItemRegistry items =
        new ItemDefinitionLoader().load(documents.get(ConfigurationId.ITEMS), languages, problems);
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
        worldSettings,
        menuSettings,
        items,
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

  private static WorldManagerSettings worldManager(Reader reader) {
    String material = reader.string("world-manager.void-world.platform-material", "GLASS");
    try {
      org.bukkit.Material parsed = org.bukkit.Material.valueOf(material.toUpperCase(Locale.ROOT));
      material = parsed.name();
    } catch (IllegalArgumentException exception) {
      reader.problem(
          "world-manager.void-world.platform-material",
          material,
          "Bukkit block material",
          "GLASS",
          "Invalid safety platform material");
      material = "GLASS";
    }
    String environment =
        enumValue(
            reader,
            "world-manager.defaults.environment",
            "NORMAL",
            org.bukkit.World.Environment.class);
    String difficulty =
        enumValue(
            reader, "world-manager.defaults.difficulty", "PEACEFUL", org.bukkit.Difficulty.class);
    String templatePrefix =
        safePrefix(
            reader,
            "world-manager.naming.template-world-prefix",
            reader.string("world-manager.naming.template-world-prefix", "hbw_template_"),
            "hbw_template_");
    String instancePrefix =
        safePrefix(
            reader,
            "world-manager.naming.instance-world-prefix",
            reader.string("world-manager.naming.instance-world-prefix", "hbw_game_"),
            "hbw_game_");
    return new WorldManagerSettings(
        reader.bool("world-manager.enabled", true),
        relativePath(
            reader,
            "world-manager.directories.templates",
            reader.string("world-manager.directories.templates", "maps/templates"),
            "maps/templates"),
        relativePath(
            reader,
            "world-manager.directories.metadata",
            reader.string("world-manager.directories.metadata", "maps/metadata"),
            "maps/metadata"),
        relativePath(
            reader,
            "world-manager.directories.instances",
            reader.string("world-manager.directories.instances", "instances"),
            "instances"),
        relativePath(
            reader,
            "world-manager.directories.backups",
            reader.string("world-manager.directories.backups", "backups/maps"),
            "backups/maps"),
        templatePrefix,
        instancePrefix,
        reader.string("world-manager.fallback-world", "world"),
        reader.bool("world-manager.void-world.create-safety-platform", true),
        material,
        reader.integer("world-manager.void-world.platform-radius", 3, 0, 32),
        reader.integer("world-manager.void-world.platform-y", 64, -64, 319),
        environment,
        difficulty,
        (long) reader.decimal("world-manager.defaults.fixed-time", 6000),
        reader.bool("world-manager.defaults.clear-weather", true),
        reader.bool("world-manager.defaults.pvp", false),
        reader.bool("world-manager.defaults.animals", false),
        reader.bool("world-manager.defaults.monsters", false),
        reader.bool("world-manager.auto-save.enabled", false),
        reader.integer("world-manager.auto-save.interval-minutes", 10, 1, 1440),
        reader.bool("world-manager.unload.save-before-unload", true),
        reader.bool("world-manager.unload.refuse-if-players-present", true),
        reader.stringSet("world-manager.copy.excluded-files", Set.of("uid.dat", "session.lock")),
        reader.stringSet(
            "world-manager.copy.excluded-directories",
            Set.of("playerdata", "stats", "advancements")));
  }

  private static <E extends Enum<E>> String enumValue(
      Reader reader, String path, String fallback, Class<E> type) {
    String value = reader.string(path, fallback).toUpperCase(Locale.ROOT);
    try {
      return Enum.valueOf(type, value).name();
    } catch (IllegalArgumentException exception) {
      reader.problem(path, value, type.getSimpleName(), fallback, "Invalid enum value");
      return fallback;
    }
  }

  private static String safePrefix(Reader reader, String path, String value, String fallback) {
    String normalized = value.toLowerCase(Locale.ROOT);
    if (!normalized.matches("[a-z0-9_-]{1,32}")) {
      reader.problem(path, value, "safe world prefix", fallback, "Invalid world prefix");
      return fallback;
    }
    return normalized;
  }

  private static String relativePath(Reader reader, String path, String value, String fallback) {
    try {
      java.nio.file.Path parsed = java.nio.file.Path.of(value).normalize();
      if (parsed.isAbsolute() || parsed.startsWith("..") || value.contains(":"))
        throw new IllegalArgumentException("unsafe path");
      return parsed.toString().replace('\\', '/');
    } catch (RuntimeException exception) {
      reader.error(path, value, "safe relative path", "Unsafe world-manager directory");
      return fallback;
    }
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

  private static ArenaEditorSettings arenaEditor(Reader menus) {
    int listRows = menus.integer("arena-editor.list.rows", 6, 1, 6);
    int listSize = listRows * 9;
    int editorRows = menus.integer("arena-editor.editor.rows", 6, 1, 6);
    int editorSize = editorRows * 9;
    List<Integer> contentSlots =
        menus.integerList(
            "arena-editor.list.content-slots",
            List.of(
                10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34),
            listSize);
    int createSlot = menus.integer("arena-editor.list.create-slot", 49, 0, listSize - 1);
    int filterSlot = menus.integer("arena-editor.list.filter-slot", 47, 0, listSize - 1);
    int sortSlot = menus.integer("arena-editor.list.sort-slot", 51, 0, listSize - 1);
    int previousSlot = menus.integer("arena-editor.list.previous-page-slot", 45, 0, listSize - 1);
    int nextSlot = menus.integer("arena-editor.list.next-page-slot", 53, 0, listSize - 1);

    int informationSlot =
        menus.integer("arena-editor.editor.information-slot", 4, 0, editorSize - 1);
    int displayNameSlot =
        menus.integer("arena-editor.editor.display-name-slot", 10, 0, editorSize - 1);
    int worldSlot = menus.integer("arena-editor.editor.world-slot", 12, 0, editorSize - 1);
    int waitingSlot = menus.integer("arena-editor.editor.waiting-slot", 14, 0, editorSize - 1);
    int spectatorSlot = menus.integer("arena-editor.editor.spectator-slot", 16, 0, editorSize - 1);
    int playersSlot = menus.integer("arena-editor.editor.players-slot", 20, 0, editorSize - 1);
    int teamsSlot = menus.integer("arena-editor.editor.teams-slot", 22, 0, editorSize - 1);
    int boundarySlot = menus.integer("arena-editor.editor.boundary-slot", 24, 0, editorSize - 1);
    int validationSlot =
        menus.integer("arena-editor.editor.validation-slot", 31, 0, editorSize - 1);
    int enableSlot = menus.integer("arena-editor.editor.enable-slot", 39, 0, editorSize - 1);
    int deleteSlot = menus.integer("arena-editor.editor.delete-slot", 41, 0, editorSize - 1);
    int backSlot = menus.integer("arena-editor.editor.back-slot", 45, 0, editorSize - 1);
    int refreshSlot = menus.integer("arena-editor.editor.refresh-slot", 49, 0, editorSize - 1);
    int closeSlot = menus.integer("arena-editor.editor.close-slot", 53, 0, editorSize - 1);

    List<Integer> listSlots =
        combine(
            contentSlots,
            createSlot,
            filterSlot,
            sortSlot,
            previousSlot,
            nextSlot,
            listSize - 8,
            listSize - 6,
            listSize - 4);
    List<Integer> editorSlots =
        List.of(
            informationSlot,
            displayNameSlot,
            worldSlot,
            waitingSlot,
            spectatorSlot,
            playersSlot,
            teamsSlot,
            boundarySlot,
            validationSlot,
            enableSlot,
            deleteSlot,
            backSlot,
            refreshSlot,
            closeSlot);
    validateSlotRange(menus, "arena-editor.list", listSlots, listSize);
    validateSlotRange(menus, "arena-editor.editor", editorSlots, editorSize);
    validateDistinctSlots(menus, "arena-editor.list", listSlots);
    validateDistinctSlots(menus, "arena-editor.editor", editorSlots);
    return new ArenaEditorSettings(
        listRows,
        contentSlots,
        createSlot,
        filterSlot,
        sortSlot,
        previousSlot,
        nextSlot,
        editorRows,
        informationSlot,
        displayNameSlot,
        worldSlot,
        waitingSlot,
        spectatorSlot,
        playersSlot,
        teamsSlot,
        boundarySlot,
        validationSlot,
        enableSlot,
        deleteSlot,
        backSlot,
        refreshSlot,
        closeSlot);
  }

  private static List<Integer> combine(List<Integer> content, Integer... controls) {
    List<Integer> values = new ArrayList<>(content);
    values.addAll(List.of(controls));
    return values;
  }

  private static void validateDistinctSlots(Reader reader, String path, List<Integer> slots) {
    Set<Integer> distinct = new HashSet<>();
    for (int slot : slots) {
      if (!distinct.add(slot)) {
        reader.error(path, slot, "distinct slots", "Duplicate arena editor slot");
      }
    }
  }

  private static void validateSlotRange(
      Reader reader, String path, List<Integer> slots, int inventorySize) {
    for (int slot : slots) {
      if (slot < 0 || slot >= inventorySize) {
        reader.error(
            path,
            slot,
            "slot between 0 and " + (inventorySize - 1),
            "Arena editor slot outside inventory");
      }
    }
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

    List<Integer> integerList(String key, List<Integer> fallback, int exclusiveMaximum) {
      Object value = document.value(key);
      if (value instanceof List<?> list) {
        List<Integer> result =
            list.stream()
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .map(Number::intValue)
                .filter(slot -> slot >= 0 && slot < exclusiveMaximum)
                .distinct()
                .toList();
        if (!result.isEmpty() && result.size() == list.size()) return result;
      }
      problem(key, value, "unique valid slot list", fallback, "Invalid slot list");
      return fallback;
    }

    java.util.Set<String> stringSet(String key, java.util.Set<String> fallback) {
      Object value = document.value(key);
      if (value instanceof List<?> list) {
        java.util.Set<String> result =
            list.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (!result.isEmpty() && result.size() == list.size()) return result;
      }
      problem(key, value, "non-empty string list", fallback, "Invalid string list");
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

    void error(String key, Object value, Object expected, String message) {
      problems.add(
          new ConfigurationProblem(
              ProblemSeverity.ERROR,
              document.fileName(),
              key,
              String.valueOf(value),
              String.valueOf(expected),
              "none",
              message));
    }
  }
}
