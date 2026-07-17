package fr.heneria.bedwars.plugin.config;

import fr.heneria.bedwars.core.config.ArenaEditorSettings;
import fr.heneria.bedwars.core.config.ConfigurationDocument;
import fr.heneria.bedwars.core.config.ConfigurationId;
import fr.heneria.bedwars.core.config.ConfigurationProblem;
import fr.heneria.bedwars.core.config.ConfigurationSnapshot;
import fr.heneria.bedwars.core.config.GameSettings;
import fr.heneria.bedwars.core.config.GameplaySettings;
import fr.heneria.bedwars.core.config.LobbySettings;
import fr.heneria.bedwars.core.config.MapEditorSettings;
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

    Reader game = new Reader(documents.get(ConfigurationId.GAME), problems);
    String waitingGameMode =
        allowedValue(
            game,
            "game.waiting.game-mode",
            "ADVENTURE",
            Set.of("ADVENTURE", "SURVIVAL", "CREATIVE", "SPECTATOR"));
    String bossBarColor =
        allowedValue(
            game,
            "game.countdown.bossbar.color",
            "BLUE",
            Set.of("PINK", "BLUE", "RED", "GREEN", "YELLOW", "PURPLE", "WHITE"));
    String bossBarStyle =
        allowedValue(
            game,
            "game.countdown.bossbar.style",
            "SOLID",
            Set.of("SOLID", "SEGMENTED_6", "SEGMENTED_10", "SEGMENTED_12", "SEGMENTED_20"));
    int leaveItemSlot = game.integer("game.inventory.leave-slot", 8, 0, 8);
    int infoItemSlot = game.integer("game.inventory.info-slot", 4, 0, 8);
    boolean waitingInventorySlotsCollide = leaveItemSlot == infoItemSlot;
    GameSettings gameSettings =
        new GameSettings(
            waitingGameMode,
            game.bool("game.waiting.protect-players", true),
            game.bool("game.waiting.disable-hunger", true),
            game.bool("game.waiting.disable-item-drop", true),
            game.bool("game.waiting.disable-item-pickup", true),
            game.decimal("game.waiting.void-rescue-y", 0),
            game.bool("game.waiting.destroy-empty-instance", true),
            game.integer("game.waiting.empty-destroy-delay-seconds", 30, 0, 3600),
            game.bool("game.countdown.enabled", true),
            game.integer("game.countdown.normal-seconds", 30, 1, 3600),
            game.integer("game.countdown.full-game-seconds", 10, 1, 3600),
            game.bool("game.countdown.cancel-below-minimum", true),
            game.bool("game.countdown.allow-join-during-countdown", true),
            game.integerSet(
                "game.countdown.announcements.chat-seconds", Set.of(30, 20, 10), 1, 3600),
            game.integerSet(
                "game.countdown.announcements.title-seconds", Set.of(5, 4, 3, 2, 1), 1, 3600),
            game.bool("game.countdown.bossbar.enabled", true),
            bossBarColor,
            bossBarStyle,
            game.bool("game.scoreboard.enabled", true),
            game.integer("game.scoreboard.update-interval-ticks", 20, 1, 1200),
            game.bool("game.scoreboard.hide-red-numbers", true),
            game.string("game.scoreboard.title", "<aqua><bold>HENERIA BEDWARS</bold></aqua>"),
            game.stringList(
                "game.scoreboard.waiting.lines",
                List.of(
                    "",
                    "<white>Carte <dark_gray>» <aqua>{arena_name}",
                    "<white>Joueurs <dark_gray>» <green>{players}<gray>/{maximum_players}",
                    "<white>Minimum <dark_gray>» <yellow>{minimum_players}",
                    "<white>État <dark_gray>» {state_color}{state}",
                    "",
                    "{status_message}",
                    "",
                    "<aqua>{server_address}"),
                15),
            game.stringList(
                "game.scoreboard.starting.lines",
                List.of(
                    "",
                    "<white>Carte <dark_gray>» <aqua>{arena_name}",
                    "<white>Joueurs <dark_gray>» <green>{players}<gray>/{maximum_players}",
                    "",
                    "<white>Début dans <dark_gray>» <green>{countdown}s",
                    "",
                    "<yellow>Préparez-vous !",
                    "",
                    "<aqua>{server_address}"),
                15),
            game.stringList(
                "game.scoreboard.playing.lines",
                List.of(
                    "",
                    "<white>Carte <dark_gray>» <aqua>{arena_name}",
                    "<white>Équipe <dark_gray>» <aqua>{team_name}",
                    "<white>Équipes <dark_gray>» <green>{remaining_teams}",
                    "",
                    "<white>Kills <dark_gray>» <green>{kills}",
                    "<white>Lits <dark_gray>» <aqua>{beds_destroyed}",
                    "",
                    "<aqua>{server_address}"),
                15),
            game.string("game.scoreboard.server-name", "Heneria"),
            game.string("game.scoreboard.server-address", "play.heneria.fr"),
            leaveItemSlot,
            waitingInventorySlotsCollide ? (infoItemSlot + 1) % 9 : infoItemSlot,
            game.integer("game.waiting.items.interaction-cooldown-millis", 500, 0, 60_000),
            game.bool("game.forced-start.enabled", true),
            game.integer("game.ending.duration-seconds", 10, 1, 300));
    if (waitingInventorySlotsCollide)
      game.error(
          "game.inventory",
          leaveItemSlot,
          "two distinct hotbar slots",
          "Waiting inventory slots collide");

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
            mapEditor(menus),
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
        gameSettings,
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

  private static String allowedValue(
      Reader reader, String path, String fallback, Set<String> allowed) {
    String value = reader.string(path, fallback).toUpperCase(Locale.ROOT);
    if (allowed.contains(value)) return value;
    reader.problem(path, value, allowed, fallback, "Invalid enum value");
    return fallback;
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
    String assistant = "arena-editor.assistant-v5.";
    int listRows = menus.integer("arena-editor.list.rows", 6, 1, 6);
    int listSize = listRows * 9;
    int editorRows = menus.integer(assistant + "rows", 5, 1, 6);
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

    int informationSlot = menus.integer(assistant + "information-slot", 4, 0, editorSize - 1);
    int worldSlot = menus.integer(assistant + "world-slot", 10, 0, editorSize - 1);
    int waitingSlot = menus.integer(assistant + "waiting-slot", 12, 0, editorSize - 1);
    int spectatorSlot = menus.integer(assistant + "spectator-slot", 14, 0, editorSize - 1);
    int teamsSlot = menus.integer(assistant + "teams-slot", 16, 0, editorSize - 1);
    List<Integer> teamSlots =
        menus.integerList(
            assistant + "team-slots", List.of(19, 20, 21, 22, 23, 24, 25, 31), editorSize);
    int validationSlot = menus.integer(assistant + "validation-slot", 40, 0, editorSize - 1);
    int enableSlot = menus.integer(assistant + "enable-slot", 38, 0, editorSize - 1);
    int deleteSlot = menus.integer(assistant + "delete-slot", 42, 0, editorSize - 1);
    int backSlot = menus.integer(assistant + "back-slot", 36, 0, editorSize - 1);
    int closeSlot = menus.integer(assistant + "close-slot", 44, 0, editorSize - 1);

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
        combine(
            teamSlots,
            informationSlot,
            worldSlot,
            waitingSlot,
            spectatorSlot,
            teamsSlot,
            validationSlot,
            enableSlot,
            deleteSlot,
            backSlot,
            closeSlot);
    validateSlotRange(menus, "arena-editor.list", listSlots, listSize);
    validateSlotRange(menus, "arena-editor.assistant-v5", editorSlots, editorSize);
    validateDistinctSlots(menus, "arena-editor.list", listSlots);
    validateDistinctSlots(menus, "arena-editor.assistant-v5", editorSlots);
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
        worldSlot,
        waitingSlot,
        spectatorSlot,
        teamsSlot,
        teamSlots,
        validationSlot,
        enableSlot,
        deleteSlot,
        backSlot,
        closeSlot);
  }

  private static List<Integer> combine(List<Integer> content, Integer... controls) {
    List<Integer> values = new ArrayList<>(content);
    values.addAll(List.of(controls));
    return values;
  }

  private static MapEditorSettings mapEditor(Reader menus) {
    String root = "map-editor.";
    int listRows = menus.integer(root + "list.rows", 6, 1, 6);
    int listSize = listRows * 9;
    List<Integer> content =
        menus.integerList(
            root + "list.content-slots",
            List.of(
                10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34),
            listSize);
    int guide = menus.integer(root + "list.guide-slot", 4, 0, listSize - 1);
    int previous = menus.integer(root + "list.previous-page-slot", 46, 0, listSize - 1);
    int filter = menus.integer(root + "list.filter-slot", 47, 0, listSize - 1);
    int create = menus.integer(root + "list.create-slot", 49, 0, listSize - 1);
    int refresh = menus.integer(root + "list.refresh-slot", 50, 0, listSize - 1);
    int sort = menus.integer(root + "list.sort-slot", 51, 0, listSize - 1);
    int next = menus.integer(root + "list.next-page-slot", 52, 0, listSize - 1);
    int dashboard = menus.integer(root + "list.dashboard-slot", 45, 0, listSize - 1);
    int close = menus.integer(root + "list.close-slot", 53, 0, listSize - 1);
    List<Integer> listSlots =
        combine(content, guide, previous, filter, create, refresh, sort, next, dashboard, close);
    validateSlotRange(menus, root + "list", listSlots, listSize);
    validateDistinctSlots(menus, root + "list", listSlots);

    int editorRows = menus.integer(root + "editor.rows", 6, 1, 6);
    int editorSize = editorRows * 9;
    int summary = menus.integer(root + "editor.summary-slot", 4, 0, editorSize - 1);
    int enter = menus.integer(root + "editor.enter-slot", 10, 0, editorSize - 1);
    int save = menus.integer(root + "editor.save-slot", 12, 0, editorSize - 1);
    int spawn = menus.integer(root + "editor.spawn-slot", 14, 0, editorSize - 1);
    int worldState = menus.integer(root + "editor.world-state-slot", 16, 0, editorSize - 1);
    int displayName = menus.integer(root + "editor.display-name-slot", 19, 0, editorSize - 1);
    int type = menus.integer(root + "editor.type-slot", 20, 0, editorSize - 1);
    int settings = menus.integer(root + "editor.settings-slot", 21, 0, editorSize - 1);
    int workflow = menus.integer(root + "editor.workflow-slot", 22, 0, editorSize - 1);
    int associations = menus.integer(root + "editor.associations-slot", 23, 0, editorSize - 1);
    int importSlot = menus.integer(root + "editor.import-slot", 24, 0, editorSize - 1);
    int validation = menus.integer(root + "editor.validation-slot", 25, 0, editorSize - 1);
    int backup = menus.integer(root + "editor.backup-slot", 29, 0, editorSize - 1);
    int duplicate = menus.integer(root + "editor.duplicate-slot", 31, 0, editorSize - 1);
    int delete = menus.integer(root + "editor.delete-slot", 33, 0, editorSize - 1);
    int list = menus.integer(root + "editor.list-slot", 45, 0, editorSize - 1);
    int editorRefresh = menus.integer(root + "editor.refresh-slot", 49, 0, editorSize - 1);
    int editorClose = menus.integer(root + "editor.close-slot", 53, 0, editorSize - 1);
    List<Integer> editorSlots =
        List.of(
            summary,
            enter,
            save,
            spawn,
            worldState,
            displayName,
            type,
            settings,
            workflow,
            associations,
            importSlot,
            validation,
            backup,
            duplicate,
            delete,
            list,
            editorRefresh,
            editorClose);
    validateSlotRange(menus, root + "editor", editorSlots, editorSize);
    validateDistinctSlots(menus, root + "editor", editorSlots);
    return new MapEditorSettings(
        listRows,
        guide,
        content,
        previous,
        filter,
        create,
        refresh,
        sort,
        next,
        dashboard,
        close,
        editorRows,
        summary,
        enter,
        save,
        spawn,
        worldState,
        displayName,
        type,
        settings,
        workflow,
        associations,
        importSlot,
        validation,
        backup,
        duplicate,
        delete,
        list,
        editorRefresh,
        editorClose,
        menus.integer(root + "settings.rows", 5, 1, 6),
        menus.integer(root + "associations.rows", 6, 1, 6),
        menus.integer(root + "validation.rows", 6, 1, 6));
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

    List<String> stringList(String key, List<String> fallback, int maximumSize) {
      Object value = document.value(key);
      if (value instanceof List<?> list
          && !list.isEmpty()
          && list.size() <= maximumSize
          && list.stream().allMatch(String.class::isInstance)) {
        return list.stream().map(String.class::cast).toList();
      }
      problem(
          key,
          value,
          "string list with 1 to " + maximumSize + " lines",
          fallback,
          "Invalid text lines");
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

    java.util.Set<Integer> integerSet(
        String key, java.util.Set<Integer> fallback, int minimum, int maximum) {
      Object value = document.value(key);
      if (value instanceof List<?> list) {
        java.util.Set<Integer> result =
            list.stream()
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .map(Number::intValue)
                .filter(number -> number >= minimum && number <= maximum)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (!result.isEmpty() && result.size() == list.size()) return result;
      }
      problem(key, value, "unique bounded integer list", fallback, "Invalid integer list");
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
