package fr.heneria.bedwars.plugin.map;

import fr.heneria.bedwars.core.arena.ArenaDefinition;
import fr.heneria.bedwars.core.arena.ArenaOperationResult;
import fr.heneria.bedwars.core.arena.ArenaService;
import fr.heneria.bedwars.core.command.AdministrativeCommandPolicy;
import fr.heneria.bedwars.core.config.MapEditorSettings;
import fr.heneria.bedwars.core.config.PlaceholderContext;
import fr.heneria.bedwars.core.config.ProblemSeverity;
import fr.heneria.bedwars.core.gui.ConfirmationGui;
import fr.heneria.bedwars.core.gui.Gui;
import fr.heneria.bedwars.core.gui.GuiButton;
import fr.heneria.bedwars.core.gui.GuiClickContext;
import fr.heneria.bedwars.core.gui.GuiClickType;
import fr.heneria.bedwars.core.gui.TextInputCancelReason;
import fr.heneria.bedwars.core.gui.TextInputRequest;
import fr.heneria.bedwars.core.gui.TextInputService;
import fr.heneria.bedwars.core.logging.ProjectLogger;
import fr.heneria.bedwars.core.map.MapId;
import fr.heneria.bedwars.core.map.MapOperationCode;
import fr.heneria.bedwars.core.map.MapOperationResult;
import fr.heneria.bedwars.core.map.MapSpawn;
import fr.heneria.bedwars.core.map.MapState;
import fr.heneria.bedwars.core.map.MapTemplate;
import fr.heneria.bedwars.core.map.MapTemplateService;
import fr.heneria.bedwars.core.map.MapType;
import fr.heneria.bedwars.core.map.MapValidationProblem;
import fr.heneria.bedwars.core.map.MapValidationResult;
import fr.heneria.bedwars.core.map.MapWorldSettings;
import fr.heneria.bedwars.core.map.editor.MapEditorStateStore;
import fr.heneria.bedwars.core.map.editor.MapEditorViewState;
import fr.heneria.bedwars.core.map.editor.MapListFilter;
import fr.heneria.bedwars.core.map.editor.MapProgress;
import fr.heneria.bedwars.core.map.editor.MapProgressEvaluator;
import fr.heneria.bedwars.core.map.editor.MapSortDirection;
import fr.heneria.bedwars.core.map.operation.MapOperationSnapshot;
import fr.heneria.bedwars.core.map.operation.MapOperationTracker;
import fr.heneria.bedwars.core.map.operation.MapOperationType;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import fr.heneria.bedwars.plugin.gui.GuiService;
import fr.heneria.bedwars.plugin.gui.StandardGuiButtons;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Ticket 008 guided map administration facade; every mutation delegates to domain services. */
public final class MapMenuFactory {
  private static final DateTimeFormatter DATE =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
  private static final Set<String> ALLOWED_TAGS =
      Set.of(
          "black",
          "dark_blue",
          "dark_green",
          "dark_aqua",
          "dark_red",
          "dark_purple",
          "gold",
          "gray",
          "dark_gray",
          "blue",
          "green",
          "aqua",
          "red",
          "light_purple",
          "yellow",
          "white",
          "bold",
          "italic",
          "underlined",
          "strikethrough",
          "reset");

  private final JavaPlugin plugin;
  private final MapTemplateService maps;
  private final BukkitMapWorldService worlds;
  private final ArenaService arenas;
  private final ConfigurationService configurations;
  private final GuiService gui;
  private final TextInputService input;
  private final MapEditorStateStore states;
  private final MapOperationTracker operations;
  private final MapMenuNavigation navigation;
  private final ProjectLogger logger;
  private final Clock clock;
  private final MapProgressEvaluator progress = new MapProgressEvaluator();
  private final StandardGuiButtons standard = new StandardGuiButtons();

  public MapMenuFactory(
      JavaPlugin plugin,
      MapTemplateService maps,
      BukkitMapWorldService worlds,
      ArenaService arenas,
      ConfigurationService configurations,
      GuiService gui,
      TextInputService input,
      MapEditorStateStore states,
      MapOperationTracker operations,
      MapMenuNavigation navigation,
      ProjectLogger logger,
      Clock clock) {
    this.plugin = plugin;
    this.maps = maps;
    this.worlds = worlds;
    this.arenas = arenas;
    this.configurations = configurations;
    this.gui = gui;
    this.input = input;
    this.states = states;
    this.operations = operations;
    this.navigation = navigation;
    this.logger = logger;
    this.clock = clock;
  }

  public Gui list(UUID playerId) {
    MapEditorViewState state = states.state(playerId);
    MapEditorSettings layout = settings();
    List<MapTemplate> all = maps.list();
    List<MapTemplate> visible =
        all.stream()
            .filter(state.filter()::accepts)
            .sorted(state.sort().comparator(state.direction()))
            .toList();
    int pageSize = layout.listContentSlots().size();
    int maximumPage = Math.max(1, (visible.size() + pageSize - 1) / pageSize);
    state.page(Math.min(state.page(), maximumPage - 1));
    Map<String, Object> listValues = listPlaceholders(state, visible.size(), all.size());
    Gui.Builder builder =
        Gui.builder()
            .id("map.list")
            .title(message("map.gui.list-title-v4", listValues))
            .rows(layout.listRows())
            .fillEmptySlots(true)
            .button(
                layout.listGuideSlot(),
                GuiButton.builder()
                    .itemKey("map.list.guide-v4")
                    .itemPlaceholders(context -> listValues)
                    .build());
    if (visible.isEmpty())
      builder.button(
          22,
          GuiButton.builder()
              .itemKey(all.isEmpty() ? "map.list.empty-all-v4" : "map.list.empty-filter-v4")
              .itemPlaceholders(context -> listValues)
              .build());
    for (int index = 0; index < pageSize; index++) {
      int absolute = state.page() * pageSize + index;
      if (absolute >= visible.size()) break;
      MapTemplate template = visible.get(absolute);
      GuiButton.Builder entry =
          GuiButton.builder()
              .itemKey(entryKey(template))
              .itemPlaceholders(context -> menuPlaceholders(template))
              .onLeftClick(context -> context.open(info(playerId, template.id().value())))
              .onRightClick(context -> enter(playerId, template.id().value()));
      if (template.loaded())
        entry
            .on(
                GuiClickType.SHIFT_LEFT,
                context ->
                    mapMutation(
                        playerId,
                        maps.save(template.id().value()),
                        context,
                        template.id().value(),
                        false))
            .on(
                GuiClickType.SHIFT_RIGHT,
                context ->
                    mapMutation(
                        playerId,
                        maps.unload(template.id().value(), false),
                        context,
                        template.id().value(),
                        false));
      builder.button(layout.listContentSlots().get(index), entry.build());
    }
    if (state.page() > 0)
      builder.button(
          layout.listPreviousSlot(),
          standard.previous(
              context -> {
                state.page(state.page() - 1);
                context.replace(list(playerId));
              }));
    if (state.page() + 1 < maximumPage)
      builder.button(
          layout.listNextSlot(),
          standard.next(
              context -> {
                state.page(state.page() + 1);
                context.replace(list(playerId));
              }));
    return builder
        .button(layout.listFilterSlot(), filterButton(playerId, state))
        .button(layout.listSortSlot(), sortButton(playerId, state))
        .button(
            layout.listCreateSlot(),
            GuiButton.builder()
                .itemKey("map.list.create-v4")
                .permission(AdministrativeCommandPolicy.MAP_CREATE)
                .onLeftClick(context -> beginCreate(playerId))
                .onRightClick(context -> context.open(advancedCreation(playerId)))
                .build())
        .button(
            layout.listRefreshSlot(),
            GuiButton.builder()
                .itemKey("map.list.refresh")
                .onLeftClick(context -> context.replace(list(playerId)))
                .build())
        .button(layout.listDashboardSlot(), dashboardButton(playerId))
        .button(layout.listCloseSlot(), standard.close())
        .build();
  }

  public Gui info(UUID playerId, String id) {
    MapTemplate template = maps.find(id).orElse(null);
    if (template == null) return missing(playerId);
    states.state(playerId).observe(id, template.revision());
    MapEditorSettings layout = settings();
    Map<String, Object> values = menuPlaceholders(template);
    Optional<MapOperationSnapshot> operation = operations.find(template.id());
    Gui.Builder builder =
        Gui.builder()
            .id("map.info." + id)
            .title(message("map.gui.editor-title-v4", values))
            .rows(layout.editorRows())
            .fillEmptySlots(true)
            .button(
                layout.summarySlot(),
                GuiButton.builder()
                    .itemKey("map.editor.summary-v4")
                    .itemPlaceholders(context -> menuPlaceholders(id))
                    .build())
            .button(layout.listSlot(), mapListButton(playerId))
            .button(
                layout.refreshSlot(),
                GuiButton.builder()
                    .itemKey("gui.refresh")
                    .onLeftClick(context -> context.replace(info(playerId, id)))
                    .build())
            .button(layout.closeSlot(), standard.close());
    if (operation.filter(snapshot -> snapshot.status().active()).isPresent())
      return builder
          .button(
              layout.workflowSlot(),
              GuiButton.builder()
                  .itemKey("map.editor.operation-v4")
                  .itemPlaceholders(context -> operationPlaceholders(id))
                  .onLeftClick(context -> context.open(operation(playerId, id)))
                  .build())
          .build();
    builder
        .button(
            layout.enterSlot(),
            action(
                template.loaded() ? "map.editor.enter-v4" : "map.editor.open-enter-v4",
                AdministrativeCommandPolicy.MAP_TELEPORT,
                values,
                context -> enter(playerId, id)))
        .button(layout.spawnSlot(), spawnButton(playerId, template))
        .button(
            layout.worldStateSlot(),
            template.loaded()
                ? worldCloseButton(playerId, template)
                : action(
                    "map.editor.open-v4",
                    AdministrativeCommandPolicy.MAP_LOAD,
                    values,
                    context -> mapMutation(playerId, maps.load(id), context, id, true)))
        .button(
            layout.displayNameSlot(),
            action(
                "map.editor.display-name-v4",
                AdministrativeCommandPolicy.MAP_EDIT,
                values,
                context -> requestDisplayName(playerId, template)))
        .button(
            layout.typeSlot(),
            action(
                "map.editor.type-v4",
                AdministrativeCommandPolicy.MAP_EDIT,
                values,
                context -> context.open(typeMenu(playerId, id))))
        .button(
            layout.settingsSlot(),
            action(
                "map.editor.settings-v4",
                AdministrativeCommandPolicy.MAP_EDIT,
                values,
                context -> context.open(worldSettings(playerId, id))))
        .button(
            layout.workflowSlot(),
            GuiButton.builder()
                .itemKey("map.editor.workflow-v4")
                .itemPlaceholders(context -> menuPlaceholders(id))
                .build())
        .button(
            layout.associationsSlot(),
            action(
                "map.editor.associations-v4",
                AdministrativeCommandPolicy.MAP_EDIT,
                values,
                context -> context.open(associations(playerId, id))))
        .button(
            layout.validationSlot(),
            GuiButton.builder()
                .itemKey("map.editor.validation-v4")
                .itemPlaceholders(context -> menuPlaceholders(id))
                .onLeftClick(context -> context.open(validation(playerId, id)))
                .build())
        .button(
            layout.backupSlot(),
            action(
                "map.editor.backup-v4",
                AdministrativeCommandPolicy.MAP_SAVE,
                values,
                context -> beginBackup(playerId, template)))
        .button(
            layout.duplicateSlot(),
            action(
                "map.editor.duplicate-v4",
                AdministrativeCommandPolicy.MAP_DUPLICATE,
                values,
                context -> requestDuplicate(playerId, template)))
        .button(
            layout.deleteSlot(),
            action(
                "map.editor.delete-v4",
                AdministrativeCommandPolicy.MAP_DELETE,
                values,
                context -> context.open(deleteConfirmation(playerId, id))));
    if (template.loaded())
      builder.button(
          layout.saveSlot(),
          action(
              "map.editor.save-v4",
              AdministrativeCommandPolicy.MAP_SAVE,
              values,
              context -> mapMutation(playerId, maps.save(id), context, id, true)));
    return builder.build();
  }

  public Gui deleteConfirmation(UUID playerId, String id) {
    MapTemplate template = maps.find(id).orElse(null);
    if (template == null) return missing(playerId);
    return ConfirmationGui.builder()
        .id("map.delete." + id)
        .title(message("map.gui.delete-v4", menuPlaceholders(template)))
        .informationKey("map.editor.delete-v4")
        .informationPlaceholders(menuPlaceholders(template))
        .confirmItemKey("gui.confirm")
        .cancelItemKey("gui.cancel")
        .permission(AdministrativeCommandPolicy.MAP_DELETE)
        .onCancel(context -> context.replace(info(playerId, id)))
        .onConfirm(context -> confirmDeleteRisk(playerId, template))
        .build();
  }

  public void beginCreate(UUID playerId) {
    beginCreate(playerId, MapType.BEDWARS);
  }

  public Map<String, Object> menuPlaceholders(MapTemplate template) {
    Map<String, Object> values = new LinkedHashMap<>(placeholders(template));
    MapProgress setup = progress.evaluate(template, maps.protectedTemplate(template.id().value()));
    MapValidationResult validation = maps.validate(template.id().value());
    values.put("map_type_label", typeLabel(template.type()));
    values.put("map_state_label", stateLabel(template.state()));
    values.put("dirty_state", dirtyLabel(template));
    values.put("spawn_state", booleanLabel(template.spawn().configured()));
    values.put("progress_current", setup.current());
    values.put("progress_total", setup.total());
    values.put("progress_percent", setup.percent());
    values.put("next_action", message(setup.nextActionKey(), Map.of()));
    values.put("validation_errors", validation.errors());
    values.put("validation_warnings", validation.warnings());
    values.put("operation_active", operations.active(template.id()));
    operations
        .find(template.id())
        .ifPresent(operation -> values.putAll(operationValues(operation)));
    values.putAll(settingPlaceholders(template.settings()));
    return Map.copyOf(values);
  }

  public Map<String, Object> menuPlaceholders(String id) {
    return maps.find(id).map(this::menuPlaceholders).orElse(Map.of("map_id", id));
  }

  /** Stable raw placeholders retained for advanced command compatibility. */
  public static Map<String, Object> placeholders(MapTemplate template) {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("map_id", template.id().value());
    values.put("map_name", template.displayName());
    values.put("map_type", template.type());
    values.put("map_state", template.state());
    values.put("world_name", template.worldName());
    values.put("folder_name", template.folderName());
    values.put("loaded", template.loaded());
    values.put(
        "player_count",
        Bukkit.getWorld(template.worldName()) == null
            ? 0
            : Bukkit.getWorld(template.worldName()).getPlayers().size());
    values.put("arena_count", template.linkedArenaIds().size());
    values.put("linked_arenas", String.join(", ", template.linkedArenaIds()));
    values.put("revision", template.revision());
    values.put("last_saved_at", template.lastSavedAt().map(DATE::format).orElse("-"));
    values.put("last_loaded_at", template.lastLoadedAt().map(DATE::format).orElse("-"));
    values.put("created_at", DATE.format(template.createdAt()));
    values.put("updated_at", DATE.format(template.updatedAt()));
    values.put("author", template.author().isBlank() ? "-" : template.author());
    values.put("description", template.description().isBlank() ? "-" : template.description());
    values.put("spawn_configured", template.spawn().configured());
    values.put("spawn_x", template.spawn().x());
    values.put("spawn_y", template.spawn().y());
    values.put("spawn_z", template.spawn().z());
    values.put("dirty", template.dirty());
    return Map.copyOf(values);
  }

  private Gui advancedCreation(UUID playerId) {
    return Gui.builder()
        .id("map.creation.advanced")
        .title(message("map.gui.creation.advanced-title", Map.of()))
        .rows(3)
        .fillEmptySlots(true)
        .button(11, creationTypeButton(playerId, MapType.BEDWARS))
        .button(13, creationTypeButton(playerId, MapType.LOBBY))
        .button(15, creationTypeButton(playerId, MapType.GENERIC))
        .button(22, mapListButton(playerId))
        .build();
  }

  private GuiButton creationTypeButton(UUID playerId, MapType type) {
    return GuiButton.builder()
        .itemKey("map.creation.type-" + type.name().toLowerCase(Locale.ROOT) + "-v4")
        .itemPlaceholders(context -> Map.of("map_type_label", typeLabel(type)))
        .permission(AdministrativeCommandPolicy.MAP_CREATE)
        .onLeftClick(context -> beginCreate(playerId, type))
        .build();
  }

  private void beginCreate(UUID playerId, MapType type) {
    Player player = Bukkit.getPlayer(playerId);
    if (player == null) return;
    if (!input.begin(
        playerId,
        request(
            playerId,
            "map.gui.creation.input",
            32,
            value -> validateMapId(value),
            value -> gui.open(player, creationConfirmation(playerId, value, type)),
            reason -> gui.open(player, list(playerId))))) {
      send(playerId, "map.input.already-active", Map.of());
      return;
    }
    gui.close(player);
    send(playerId, "map.gui.creation.input", Map.of("map_type_label", typeLabel(type)));
  }

  private Gui creationConfirmation(UUID playerId, String id, MapType type) {
    Map<String, Object> values = creationPlaceholders(id, type);
    return ConfirmationGui.builder()
        .id("map.creation.confirm." + id)
        .title(message("map.gui.creation.confirm-title", values))
        .informationKey("map.creation.summary-v4")
        .informationPlaceholders(values)
        .confirmItemKey("gui.confirm")
        .cancelItemKey("gui.cancel")
        .permission(AdministrativeCommandPolicy.MAP_CREATE)
        .onCancel(context -> context.replace(list(playerId)))
        .onConfirm(context -> create(playerId, id, type))
        .build();
  }

  private void create(UUID playerId, String id, MapType type) {
    Player player = Bukkit.getPlayer(playerId);
    if (player == null) return;
    MapOperationResult result = maps.create(id, type, player.getName());
    feedback(playerId, result);
    if (!result.successful()) {
      gui.open(player, list(playerId));
      return;
    }
    MapTemplate created = result.template().orElseThrow();
    states.forget(created.id().value());
    var teleport = worlds.teleport(player, created);
    gui.close(player);
    send(
        playerId,
        teleport.successful() ? "map.gui.creation.ready" : "map.error.teleport",
        menuPlaceholders(created));
  }

  private Gui typeMenu(UUID playerId, String id) {
    MapTemplate template = maps.find(id).orElse(null);
    if (template == null) return missing(playerId);
    return Gui.builder()
        .id("map.type." + id)
        .title(message("map.gui.type.title", menuPlaceholders(template)))
        .rows(3)
        .fillEmptySlots(true)
        .button(11, typeButton(playerId, template, MapType.BEDWARS))
        .button(13, typeButton(playerId, template, MapType.LOBBY))
        .button(15, typeButton(playerId, template, MapType.GENERIC))
        .button(22, mapEditorBackButton(playerId, id))
        .build();
  }

  private GuiButton typeButton(UUID playerId, MapTemplate template, MapType type) {
    Map<String, Object> values = new LinkedHashMap<>(menuPlaceholders(template));
    values.put("candidate_type", typeLabel(type));
    values.put("selected", booleanLabel(template.type() == type));
    return GuiButton.builder()
        .itemKey("map.editor.type-choice-v4")
        .itemPlaceholders(context -> values)
        .permission(AdministrativeCommandPolicy.MAP_EDIT)
        .onLeftClick(
            context ->
                mapMutation(
                    playerId,
                    maps.setType(template.id().value(), type, template.revision()),
                    context,
                    template.id().value(),
                    true))
        .build();
  }

  private Gui worldSettings(UUID playerId, String id) {
    MapTemplate template = maps.find(id).orElse(null);
    if (template == null) return missing(playerId);
    MapWorldSettings value = template.settings();
    Map<String, Object> placeholders = menuPlaceholders(template);
    return Gui.builder()
        .id("map.settings." + id)
        .title(message("map.gui.settings.title", placeholders))
        .rows(settings().settingsRows())
        .fillEmptySlots(true)
        .button(
            4,
            GuiButton.builder()
                .itemKey("map.settings.summary-v4")
                .itemPlaceholders(context -> placeholders)
                .build())
        .button(10, timeButton(playerId, template))
        .button(
            11,
            settingToggle(
                playerId,
                template,
                "map.settings.day-cycle-v4",
                value.withDaylightCycle(!value.daylightCycle())))
        .button(
            12,
            settingToggle(
                playerId,
                template,
                "map.settings.weather-v4",
                value.withClearWeather(!value.clearWeather())))
        .button(
            13,
            settingToggle(
                playerId,
                template,
                "map.settings.weather-cycle-v4",
                value.withWeatherCycle(!value.weatherCycle())))
        .button(14, difficultyButton(playerId, template))
        .button(
            15,
            settingToggle(playerId, template, "map.settings.pvp-v4", value.withPvp(!value.pvp())))
        .button(
            16,
            settingToggle(
                playerId,
                template,
                "map.settings.creatures-v4",
                value.withCreatures(!(value.allowAnimals() || value.allowMonsters()))))
        .button(
            20,
            settingToggle(
                playerId, template, "map.settings.fire-v4", value.withFireTick(!value.fireTick())))
        .button(
            22,
            settingToggle(
                playerId,
                template,
                "map.settings.environment-v4",
                value.withEnvironmentalDamage(!value.environmentalDamage())))
        .button(
            24,
            settingToggle(
                playerId,
                template,
                "map.settings.autosave-v4",
                value.withAutoSave(!value.autoSave())))
        .button(36, mapEditorBackButton(playerId, id))
        .button(44, standard.close())
        .build();
  }

  private GuiButton timeButton(UUID playerId, MapTemplate template) {
    return GuiButton.builder()
        .itemKey("map.settings.time-v4")
        .itemPlaceholders(context -> menuPlaceholders(template))
        .permission(AdministrativeCommandPolicy.MAP_EDIT)
        .onLeftClick(context -> changeTime(playerId, template, 1000, context))
        .onRightClick(context -> changeTime(playerId, template, -1000, context))
        .on(GuiClickType.SHIFT_LEFT, context -> changeTime(playerId, template, 6000, context))
        .on(GuiClickType.SHIFT_RIGHT, context -> changeTime(playerId, template, -6000, context))
        .on(GuiClickType.MIDDLE, context -> requestTime(playerId, template))
        .build();
  }

  private void changeTime(
      UUID playerId, MapTemplate template, long delta, GuiClickContext context) {
    long value = Math.floorMod(template.settings().fixedTime() + delta, 24_001);
    settingMutation(playerId, template, template.settings().withTime(value), context);
  }

  private void requestTime(UUID playerId, MapTemplate template) {
    Player player = Bukkit.getPlayer(playerId);
    if (player == null) return;
    if (!input.begin(
        playerId,
        request(
            playerId,
            "map.gui.settings.time-input",
            5,
            value -> validLong(value, 0, 24_000),
            value -> {
              MapOperationResult result =
                  maps.setSettings(
                      template.id().value(),
                      template.settings().withTime(Long.parseLong(value)),
                      template.revision());
              feedback(playerId, result);
              gui.open(player, worldSettings(playerId, template.id().value()));
            },
            reason -> gui.open(player, worldSettings(playerId, template.id().value()))))) {
      send(playerId, "map.input.already-active", Map.of());
      return;
    }
    gui.close(player);
    send(playerId, "map.gui.settings.time-input", menuPlaceholders(template));
  }

  private GuiButton difficultyButton(UUID playerId, MapTemplate template) {
    Map<String, Object> values = menuPlaceholders(template);
    return GuiButton.builder()
        .itemKey("map.settings.difficulty-v4")
        .itemPlaceholders(context -> values)
        .permission(AdministrativeCommandPolicy.MAP_EDIT)
        .onLeftClick(context -> changeDifficulty(playerId, template, 1, context))
        .onRightClick(context -> changeDifficulty(playerId, template, -1, context))
        .build();
  }

  private void changeDifficulty(
      UUID playerId, MapTemplate template, int delta, GuiClickContext context) {
    List<String> values = List.of("PEACEFUL", "EASY", "NORMAL", "HARD");
    int index = values.indexOf(template.settings().difficulty());
    String next = values.get(Math.floorMod(index + delta, values.size()));
    settingMutation(playerId, template, template.settings().withDifficulty(next), context);
  }

  private GuiButton settingToggle(
      UUID playerId, MapTemplate template, String key, MapWorldSettings next) {
    return GuiButton.builder()
        .itemKey(key)
        .itemPlaceholders(context -> menuPlaceholders(template))
        .permission(AdministrativeCommandPolicy.MAP_EDIT)
        .onLeftClick(context -> settingMutation(playerId, template, next, context))
        .build();
  }

  private void settingMutation(
      UUID playerId, MapTemplate template, MapWorldSettings next, GuiClickContext context) {
    MapOperationResult result = maps.setSettings(template.id().value(), next, template.revision());
    feedback(playerId, result);
    context.replace(worldSettings(playerId, template.id().value()));
  }

  private Gui associations(UUID playerId, String id) {
    MapTemplate template = maps.find(id).orElse(null);
    if (template == null) return missing(playerId);
    List<ArenaDefinition> visible =
        arenas.list().stream()
            .filter(
                arena ->
                    arena.template().isEmpty()
                        || arena.template().filter(id::equalsIgnoreCase).isPresent())
            .toList();
    List<Integer> slots =
        List.of(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34);
    Gui.Builder builder =
        Gui.builder()
            .id("map.associations." + id)
            .title(message("map.gui.associations.title", menuPlaceholders(template)))
            .rows(settings().associationsRows())
            .fillEmptySlots(true)
            .button(
                4,
                GuiButton.builder()
                    .itemKey("map.association.summary-v4")
                    .itemPlaceholders(context -> menuPlaceholders(template))
                    .build());
    if (template.type() != MapType.BEDWARS)
      builder.button(22, GuiButton.builder().itemKey("map.association.wrong-type-v4").build());
    else if (visible.isEmpty())
      builder.button(22, GuiButton.builder().itemKey("map.association.empty-v4").build());
    else
      for (int index = 0; index < Math.min(slots.size(), visible.size()); index++) {
        ArenaDefinition arena = visible.get(index);
        boolean linked = arena.template().filter(id::equalsIgnoreCase).isPresent();
        builder.button(slots.get(index), associationButton(playerId, template, arena, linked));
      }
    return builder
        .button(
            49,
            action(
                "map.association.create-arena-v4",
                AdministrativeCommandPolicy.ARENA_CREATE,
                menuPlaceholders(template),
                context -> requestLinkedArena(playerId, template)))
        .button(45, mapEditorBackButton(playerId, id))
        .button(53, standard.close())
        .build();
  }

  private GuiButton associationButton(
      UUID playerId, MapTemplate template, ArenaDefinition arena, boolean linked) {
    Map<String, Object> values = new LinkedHashMap<>(menuPlaceholders(template));
    values.put("arena_id", arena.id().value());
    values.put("arena_name", arena.displayName());
    values.put("arena_status", arena.status().name());
    values.put("arena_valid", arenas.validate(arena).valid());
    GuiButton.Builder button =
        GuiButton.builder()
            .itemKey(linked ? "map.association.linked-v4" : "map.association.available-v4")
            .itemPlaceholders(context -> values)
            .permission(AdministrativeCommandPolicy.ARENA_EDIT)
            .onRightClick(context -> openArenaEditor(playerId, arena.id().value()));
    if (linked)
      button
          .onLeftClick(context -> openArenaEditor(playerId, arena.id().value()))
          .on(GuiClickType.SHIFT_RIGHT, context -> unlinkArena(playerId, template, arena, context));
    else button.onLeftClick(context -> linkArena(playerId, template, arena, context));
    return button.build();
  }

  private void linkArena(
      UUID playerId, MapTemplate template, ArenaDefinition arena, GuiClickContext context) {
    ArenaOperationResult result =
        arenas.setMapTemplate(
            arena.id().value(), template.id().value(), template.worldName(), arena.revision());
    arenaFeedback(playerId, result);
    maps.synchronizeLinks(arenas.list());
    context.replace(associations(playerId, template.id().value()));
  }

  private void unlinkArena(
      UUID playerId, MapTemplate template, ArenaDefinition arena, GuiClickContext context) {
    ArenaOperationResult result = arenas.clearMapTemplate(arena.id().value(), arena.revision());
    arenaFeedback(playerId, result);
    maps.synchronizeLinks(arenas.list());
    context.replace(associations(playerId, template.id().value()));
  }

  private void requestLinkedArena(UUID playerId, MapTemplate template) {
    Player player = Bukkit.getPlayer(playerId);
    if (player == null || template.type() != MapType.BEDWARS) return;
    if (!input.begin(
        playerId,
        request(
            playerId,
            "map.gui.associations.create-input",
            32,
            value -> validateArenaId(value),
            value -> createLinkedArena(playerId, template.id().value(), value),
            reason -> gui.open(player, associations(playerId, template.id().value()))))) {
      send(playerId, "map.input.already-active", Map.of());
      return;
    }
    gui.close(player);
    send(
        playerId,
        "map.gui.associations.create-input",
        Map.of("suggested_arena_id", template.id().value(), "map_name", template.displayName()));
  }

  private void createLinkedArena(UUID playerId, String mapId, String arenaId) {
    Player player = Bukkit.getPlayer(playerId);
    MapTemplate template = maps.find(mapId).orElse(null);
    if (player == null || template == null) return;
    ArenaOperationResult created = arenas.create(arenaId);
    if (!created.successful()) {
      arenaFeedback(playerId, created);
      gui.open(player, associations(playerId, mapId));
      return;
    }
    ArenaDefinition arena = created.arena().orElseThrow();
    ArenaOperationResult linked =
        arenas.setMapTemplate(arena.id().value(), mapId, template.worldName(), arena.revision());
    arenaFeedback(playerId, linked);
    maps.synchronizeLinks(arenas.list());
    if (linked.successful()) openArenaEditor(playerId, arena.id().value());
    else gui.open(player, associations(playerId, mapId));
  }

  private Gui validation(UUID playerId, String id) {
    MapTemplate template = maps.find(id).orElse(null);
    if (template == null) return missing(playerId);
    MapValidationResult result = maps.validate(id);
    Gui.Builder builder =
        Gui.builder()
            .id("map.validation." + id)
            .title(message("map.gui.validation.title", menuPlaceholders(template)))
            .rows(settings().validationRows())
            .fillEmptySlots(true)
            .button(
                4,
                GuiButton.builder()
                    .itemKey(
                        result.valid() && result.warnings() == 0
                            ? "map.validation.success-v4"
                            : "map.validation.summary-v4")
                    .itemPlaceholders(context -> menuPlaceholders(template))
                    .build());
    List<Integer> slots =
        List.of(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34);
    for (int index = 0; index < Math.min(slots.size(), result.problems().size()); index++) {
      MapValidationProblem problem = result.problems().get(index);
      Map<String, Object> values = new LinkedHashMap<>(menuPlaceholders(template));
      values.put("problem", message("map.validation.problem." + problem.code(), Map.of()));
      values.put("solution", message("map.validation.solution." + problem.code(), Map.of()));
      builder.button(
          slots.get(index),
          GuiButton.builder()
              .itemKey(validationKey(problem.severity()))
              .itemPlaceholders(context -> values)
              .onLeftClick(context -> openValidationAction(playerId, id, problem.action(), context))
              .build());
    }
    return builder
        .button(45, mapEditorBackButton(playerId, id))
        .button(
            49,
            GuiButton.builder()
                .itemKey("gui.refresh")
                .onLeftClick(context -> context.replace(validation(playerId, id)))
                .build())
        .button(53, standard.close())
        .build();
  }

  private void openValidationAction(
      UUID playerId, String id, String action, GuiClickContext context) {
    switch (action) {
      case "settings" -> context.open(worldSettings(playerId, id));
      case "associations" -> context.open(associations(playerId, id));
      case "operation" -> context.open(operation(playerId, id));
      case "dashboard" -> openDashboard(playerId);
      default -> context.replace(info(playerId, id));
    }
  }

  private Gui operation(UUID playerId, String id) {
    MapTemplate template = maps.find(id).orElse(null);
    if (template == null) return missing(playerId);
    return Gui.builder()
        .id("map.operation." + id)
        .title(message("map.gui.operation.title", menuPlaceholders(template)))
        .rows(3)
        .fillEmptySlots(true)
        .autoRefresh(Duration.ofSeconds(1))
        .button(
            13,
            GuiButton.builder()
                .itemKey(operationItemKey(id))
                .itemPlaceholders(context -> operationPlaceholders(id))
                .build())
        .button(22, mapEditorBackButton(playerId, id))
        .build();
  }

  private void beginBackup(UUID playerId, MapTemplate template) {
    if (operations.start(template.id(), MapOperationType.BACKUP, playerId, "preparing").isEmpty()) {
      send(playerId, "map.error.operation-running", menuPlaceholders(template));
      return;
    }
    states.state(playerId).followOperation(template.id().value());
    MapOperationResult prepared = maps.prepareBackup(template.id().value());
    if (!prepared.successful()) {
      operations.failed(template.id(), prepared.detail());
      feedback(playerId, prepared);
      return;
    }
    Player player = Bukkit.getPlayer(playerId);
    if (player != null) gui.close(player);
    operations.running(template.id(), "copying");
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            plugin,
            () -> {
              MapOperationResult result = maps.completeBackup(template.id().value(), "MANUAL");
              if (result.successful()) operations.success(template.id(), "complete");
              else operations.failed(template.id(), result.detail());
              Bukkit.getScheduler().runTask(plugin, () -> feedback(playerId, result));
            });
  }

  private void requestDisplayName(UUID playerId, MapTemplate template) {
    Player player = Bukkit.getPlayer(playerId);
    if (player == null) return;
    if (!input.begin(
        playerId,
        request(
            playerId,
            "map.gui.display-name-input",
            64,
            this::validateDisplayName,
            value -> {
              MapOperationResult result =
                  maps.setDisplayName(template.id().value(), value, template.revision());
              feedback(playerId, result);
              gui.open(player, info(playerId, template.id().value()));
            },
            reason -> gui.open(player, info(playerId, template.id().value()))))) {
      send(playerId, "map.input.already-active", Map.of());
      return;
    }
    gui.close(player);
    send(playerId, "map.gui.display-name-input", menuPlaceholders(template));
  }

  private GuiButton spawnButton(UUID playerId, MapTemplate template) {
    return GuiButton.builder()
        .itemKey("map.editor.spawn-v4")
        .itemPlaceholders(context -> menuPlaceholders(template))
        .permission(AdministrativeCommandPolicy.MAP_EDIT)
        .onLeftClick(context -> setSpawn(playerId, template, context))
        .onRightClick(context -> teleportToSpawn(playerId, template))
        .on(
            GuiClickType.SHIFT_RIGHT,
            context -> context.open(clearSpawnConfirmation(playerId, template)))
        .build();
  }

  private void setSpawn(UUID playerId, MapTemplate template, GuiClickContext context) {
    Player player = Bukkit.getPlayer(playerId);
    if (player == null || !player.getWorld().getName().equals(template.worldName())) {
      send(playerId, "map.error.invalid-world", menuPlaceholders(template));
      return;
    }
    var location = player.getLocation();
    MapOperationResult result =
        maps.setSpawn(
            template.id().value(),
            new MapSpawn(
                true,
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()),
            template.revision());
    mapMutation(playerId, result, context, template.id().value(), true);
  }

  private void teleportToSpawn(UUID playerId, MapTemplate template) {
    Player player = Bukkit.getPlayer(playerId);
    if (player == null) return;
    if (!template.loaded()) {
      feedback(
          playerId,
          MapOperationResult.failure(MapOperationCode.NOT_LOADED, template, template.id().value()));
      return;
    }
    var result = worlds.teleport(player, template);
    send(
        playerId,
        result.successful() ? "map.feedback.teleported" : "map.error.teleport",
        menuPlaceholders(template));
  }

  private Gui clearSpawnConfirmation(UUID playerId, MapTemplate template) {
    return ConfirmationGui.builder()
        .id("map.spawn.clear." + template.id())
        .title(message("map.gui.spawn.clear-title", menuPlaceholders(template)))
        .informationKey("map.editor.spawn-v4")
        .informationPlaceholders(menuPlaceholders(template))
        .confirmItemKey("gui.confirm")
        .cancelItemKey("gui.cancel")
        .permission(AdministrativeCommandPolicy.MAP_EDIT)
        .onCancel(context -> context.replace(info(playerId, template.id().value())))
        .onConfirm(
            context ->
                mapMutation(
                    playerId,
                    maps.clearSpawn(template.id().value(), template.revision()),
                    context,
                    template.id().value(),
                    true))
        .build();
  }

  private GuiButton worldCloseButton(UUID playerId, MapTemplate template) {
    return GuiButton.builder()
        .itemKey("map.editor.close-v4")
        .itemPlaceholders(context -> menuPlaceholders(template))
        .permission(AdministrativeCommandPolicy.MAP_UNLOAD)
        .onLeftClick(
            context ->
                mapMutation(
                    playerId,
                    maps.unload(template.id().value(), false),
                    context,
                    template.id().value(),
                    true))
        .on(
            GuiClickType.SHIFT_RIGHT,
            context -> context.open(forceUnloadConfirmation(playerId, template)))
        .build();
  }

  private Gui forceUnloadConfirmation(UUID playerId, MapTemplate template) {
    Map<String, Object> values = new LinkedHashMap<>(menuPlaceholders(template));
    return ConfirmationGui.builder()
        .id("map.unload.force." + template.id())
        .title(message("map.gui.unload.force-title", values))
        .informationKey("map.editor.force-close-v4")
        .informationPlaceholders(values)
        .confirmItemKey("gui.confirm")
        .cancelItemKey("gui.cancel")
        .permission(AdministrativeCommandPolicy.MAP_FORCE)
        .onCancel(context -> context.replace(info(playerId, template.id().value())))
        .onConfirm(
            context ->
                mapMutation(
                    playerId,
                    maps.unload(template.id().value(), true),
                    context,
                    template.id().value(),
                    true))
        .build();
  }

  private void enter(UUID playerId, String id) {
    MapTemplate template = maps.find(id).orElse(null);
    if (template == null) {
      openDashboard(playerId);
      return;
    }
    if (!template.loaded()) {
      MapOperationResult result = maps.load(id);
      if (!result.successful()) {
        feedback(playerId, result);
        return;
      }
      template = result.template().orElseThrow();
    }
    Player player = Bukkit.getPlayer(playerId);
    if (player == null) return;
    var result = worlds.teleport(player, template);
    if (result.successful()) {
      gui.close(player);
      send(playerId, "map.feedback.entered", menuPlaceholders(template));
    } else send(playerId, "map.error.teleport", menuPlaceholders(template));
  }

  private void requestDuplicate(UUID playerId, MapTemplate source) {
    Player player = Bukkit.getPlayer(playerId);
    if (player == null) return;
    if (!input.begin(
        playerId,
        request(
            playerId,
            "map.gui.duplicate.input",
            32,
            this::validateMapId,
            value -> gui.open(player, duplicateConfirmation(playerId, source, value)),
            reason -> gui.open(player, info(playerId, source.id().value()))))) {
      send(playerId, "map.input.already-active", Map.of());
      return;
    }
    gui.close(player);
    send(playerId, "map.gui.duplicate.input", menuPlaceholders(source));
  }

  private Gui duplicateConfirmation(UUID playerId, MapTemplate source, String destination) {
    Map<String, Object> values = new LinkedHashMap<>(menuPlaceholders(source));
    values.put("destination_id", destination);
    return ConfirmationGui.builder()
        .id("map.duplicate.confirm." + source.id())
        .title(message("map.gui.duplicate.confirm-title", values))
        .informationKey("map.duplicate.summary-v4")
        .informationPlaceholders(values)
        .confirmItemKey("gui.confirm")
        .cancelItemKey("gui.cancel")
        .permission(AdministrativeCommandPolicy.MAP_DUPLICATE)
        .onCancel(context -> context.replace(info(playerId, source.id().value())))
        .onConfirm(context -> duplicateAsync(playerId, source.id().value(), destination))
        .build();
  }

  private void duplicateAsync(UUID playerId, String sourceId, String destination) {
    MapTemplate source = maps.find(sourceId).orElse(null);
    if (source == null) {
      openDashboard(playerId);
      return;
    }
    if (operations
        .start(source.id(), MapOperationType.DUPLICATE, playerId, destination)
        .isEmpty()) {
      send(playerId, "map.error.operation-running", menuPlaceholders(source));
      return;
    }
    if (source.loaded()) {
      MapOperationResult saved = maps.save(sourceId);
      if (!saved.successful()) {
        operations.failed(source.id(), saved.detail());
        feedback(playerId, saved);
        return;
      }
    }
    Player player = Bukkit.getPlayer(playerId);
    if (player != null) gui.close(player);
    states.state(playerId).followOperation(sourceId);
    operations.running(source.id(), destination);
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            plugin,
            () -> {
              MapOperationResult result =
                  maps.duplicate(sourceId, destination, playerName(playerId));
              if (result.successful()) {
                operations.success(source.id(), destination);
                states.forget(destination);
                logger.info(
                    "[Maps] "
                        + playerName(playerId)
                        + " duplicated '"
                        + sourceId
                        + "' to '"
                        + destination
                        + "'.");
              } else operations.failed(source.id(), result.detail());
              Bukkit.getScheduler().runTask(plugin, () -> feedback(playerId, result));
            });
  }

  private void confirmDeleteRisk(UUID playerId, MapTemplate template) {
    if (!template.linkedArenaIds().isEmpty() || maps.protectedTemplate(template.id().value())) {
      feedback(
          playerId,
          MapOperationResult.failure(
              template.linkedArenaIds().isEmpty()
                  ? MapOperationCode.MAP_PROTECTED
                  : MapOperationCode.MAP_LINKED,
              template,
              template.id().value()));
      return;
    }
    boolean doubleConfirmation =
        template.type() == MapType.LOBBY || template.loaded() || template.state() == MapState.ERROR;
    if (doubleConfirmation) requestDeletePhrase(playerId, template);
    else deleteAsync(playerId, template.id().value());
  }

  private void requestDeletePhrase(UUID playerId, MapTemplate template) {
    Player player = Bukkit.getPlayer(playerId);
    if (player == null) return;
    String phrase = "SUPPRIMER " + template.id().value();
    if (!input.begin(
        playerId,
        request(
            playerId,
            "map.gui.deletion.phrase",
            48,
            value -> value.equals(phrase) ? Optional.empty() : Optional.of("phrase"),
            value -> deleteAsync(playerId, template.id().value()),
            reason -> gui.open(player, info(playerId, template.id().value()))))) {
      send(playerId, "map.input.already-active", Map.of());
      return;
    }
    gui.close(player);
    send(playerId, "map.gui.deletion.phrase", Map.of("delete_phrase", phrase));
  }

  private void deleteAsync(UUID playerId, String id) {
    MapTemplate template = maps.find(id).orElse(null);
    if (template == null) {
      openDashboard(playerId);
      return;
    }
    if (operations.start(template.id(), MapOperationType.DELETE, playerId, "preparing").isEmpty()) {
      send(playerId, "map.error.operation-running", menuPlaceholders(template));
      return;
    }
    MapOperationResult prepared = maps.prepareDelete(id);
    if (!prepared.successful()) {
      operations.failed(template.id(), prepared.detail());
      feedback(playerId, prepared);
      return;
    }
    Player player = Bukkit.getPlayer(playerId);
    if (player != null) gui.close(player);
    operations.running(template.id(), "deleting");
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            plugin,
            () -> {
              MapOperationResult result = maps.completeDelete(id);
              if (result.successful()) operations.success(template.id(), "complete");
              else operations.failed(template.id(), result.detail());
              Bukkit.getScheduler()
                  .runTask(
                      plugin,
                      () -> {
                        feedback(playerId, result);
                        if (result.successful()) {
                          states.forget(id);
                          maps.synchronizeLinks(arenas.list());
                          openDashboard(playerId);
                        } else {
                          Player online = Bukkit.getPlayer(playerId);
                          if (online != null) gui.open(online, info(playerId, id));
                        }
                      });
            });
  }

  private GuiButton filterButton(UUID playerId, MapEditorViewState state) {
    return GuiButton.builder()
        .itemKey("map.list.filter-v4")
        .itemPlaceholders(context -> Map.of("filter", filterLabel(state.filter())))
        .onLeftClick(
            context -> {
              state.filter(state.filter().next());
              context.replace(list(playerId));
            })
        .onRightClick(
            context -> {
              state.filter(state.filter().previous());
              context.replace(list(playerId));
            })
        .on(
            GuiClickType.SHIFT_LEFT,
            context -> {
              state.filter(MapListFilter.ALL);
              context.replace(list(playerId));
            })
        .on(
            GuiClickType.SHIFT_RIGHT,
            context -> {
              state.filter(MapListFilter.ALL);
              context.replace(list(playerId));
            })
        .build();
  }

  private GuiButton sortButton(UUID playerId, MapEditorViewState state) {
    return GuiButton.builder()
        .itemKey("map.list.sort-v4")
        .itemPlaceholders(
            context ->
                Map.of(
                    "sort",
                    sortLabel(state.sort().name()),
                    "sort_direction",
                    directionLabel(state.direction())))
        .onLeftClick(
            context -> {
              state.sort(state.sort().next());
              context.replace(list(playerId));
            })
        .onRightClick(
            context -> {
              state.sort(state.sort().previous());
              context.replace(list(playerId));
            })
        .on(
            GuiClickType.SHIFT_LEFT,
            context -> {
              state.direction(state.direction().opposite());
              context.replace(list(playerId));
            })
        .on(
            GuiClickType.SHIFT_RIGHT,
            context -> {
              state.direction(state.direction().opposite());
              context.replace(list(playerId));
            })
        .build();
  }

  private void mapMutation(
      UUID playerId,
      MapOperationResult result,
      GuiClickContext context,
      String id,
      boolean returnToEditor) {
    feedback(playerId, result);
    if (result.code() == MapOperationCode.NOT_FOUND) {
      openDashboard(playerId);
      return;
    }
    result
        .template()
        .ifPresent(template -> states.state(playerId).observe(id, template.revision()));
    context.replace(returnToEditor ? info(playerId, id) : list(playerId));
  }

  private void feedback(UUID playerId, MapOperationResult result) {
    Map<String, Object> values = new LinkedHashMap<>();
    result.template().ifPresent(template -> values.putAll(menuPlaceholders(template)));
    if (!result.successful() && !result.detail().isBlank())
      logger.warning(
          "[Maps] " + result.code() + " for " + playerName(playerId) + ": " + result.detail());
    send(playerId, result.successful() ? "map.feedback.success" : errorKey(result.code()), values);
  }

  private void arenaFeedback(UUID playerId, ArenaOperationResult result) {
    send(
        playerId,
        result.successful() ? "map.feedback.association-success" : "map.error.association",
        Map.of());
  }

  private TextInputRequest request(
      UUID playerId,
      String prompt,
      int maximumLength,
      java.util.function.Function<String, Optional<String>> validator,
      java.util.function.Consumer<String> success,
      java.util.function.Consumer<TextInputCancelReason> cancel) {
    return new TextInputRequest(
        prompt,
        configurations.snapshot().menus().textInput().timeout(),
        maximumLength,
        configurations.snapshot().menus().textInput().cancelKeywords(),
        validator,
        success,
        problem -> send(playerId, "map.input.invalid", Map.of("problem", problem)),
        reason -> {
          if (reason == TextInputCancelReason.PLAYER || reason == TextInputCancelReason.TIMEOUT)
            cancel.accept(reason);
        });
  }

  private Optional<String> validateMapId(String value) {
    try {
      MapId.parse(value);
      return maps.find(value).isPresent() ? Optional.of("already-exists") : Optional.empty();
    } catch (RuntimeException exception) {
      return Optional.of("invalid-id");
    }
  }

  private Optional<String> validateArenaId(String value) {
    try {
      fr.heneria.bedwars.core.arena.ArenaId.parse(value);
      return arenas.find(value).isPresent() ? Optional.of("already-exists") : Optional.empty();
    } catch (RuntimeException exception) {
      return Optional.of("invalid-id");
    }
  }

  private Optional<String> validateDisplayName(String value) {
    if (value == null || value.isBlank() || value.length() > 64) return Optional.of("length");
    var matcher = java.util.regex.Pattern.compile("<(/?)([^>]+)>").matcher(value);
    while (matcher.find()) {
      String tag = matcher.group(2).toLowerCase(Locale.ROOT);
      if (tag.matches("#[0-9a-f]{6}")) continue;
      if (!ALLOWED_TAGS.contains(tag)) return Optional.of("unsupported-tag");
    }
    return Optional.empty();
  }

  private static Optional<String> validLong(String value, long minimum, long maximum) {
    try {
      long parsed = Long.parseLong(value);
      return parsed >= minimum && parsed <= maximum ? Optional.empty() : Optional.of("range");
    } catch (NumberFormatException exception) {
      return Optional.of("number");
    }
  }

  private GuiButton action(
      String key,
      String permission,
      Map<String, ?> values,
      fr.heneria.bedwars.core.gui.GuiAction action) {
    return GuiButton.builder()
        .itemKey(key)
        .itemPlaceholders(context -> values)
        .permission(permission)
        .onLeftClick(action)
        .build();
  }

  private GuiButton dashboardButton(UUID playerId) {
    return GuiButton.builder()
        .itemKey("admin.main.back-to-dashboard-v3")
        .onLeftClick(context -> openDashboard(playerId))
        .build();
  }

  private GuiButton mapListButton(UUID playerId) {
    return GuiButton.builder()
        .itemKey("gui.back")
        .onLeftClick(context -> context.replace(list(playerId)))
        .build();
  }

  private GuiButton mapEditorBackButton(UUID playerId, String id) {
    return GuiButton.builder()
        .itemKey("gui.back")
        .onLeftClick(context -> context.replace(info(playerId, id)))
        .build();
  }

  private Gui missing(UUID playerId) {
    return Gui.builder()
        .id("map.missing")
        .title(message("map.gui.missing.title", Map.of()))
        .rows(3)
        .fillEmptySlots(true)
        .button(13, GuiButton.builder().itemKey("map.editor.missing-v4").build())
        .button(22, dashboardButton(playerId))
        .build();
  }

  private void openDashboard(UUID playerId) {
    Player player = Bukkit.getPlayer(playerId);
    if (player != null) gui.open(player, navigation.dashboard(playerId));
  }

  private void openArenaEditor(UUID playerId, String arenaId) {
    Player player = Bukkit.getPlayer(playerId);
    if (player != null) gui.open(player, navigation.arenaEditor(playerId, arenaId));
  }

  private MapEditorSettings settings() {
    return configurations.snapshot().menus().mapEditor();
  }

  private Map<String, Object> listPlaceholders(MapEditorViewState state, int visible, int total) {
    return Map.of(
        "visible_count",
        visible,
        "map_count",
        total,
        "filter",
        filterLabel(state.filter()),
        "sort",
        sortLabel(state.sort().name()),
        "sort_direction",
        directionLabel(state.direction()));
  }

  private Map<String, Object> creationPlaceholders(String id, MapType type) {
    return Map.of(
        "map_id",
        id,
        "map_name",
        id,
        "map_type_label",
        typeLabel(type),
        "world_name",
        configurations.snapshot().worlds().templateWorldPrefix() + id);
  }

  private Map<String, Object> settingPlaceholders(MapWorldSettings settings) {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("fixed_time", settings.fixedTime());
    values.put("day_cycle", booleanLabel(settings.daylightCycle()));
    values.put(
        "weather",
        message(
            settings.clearWeather() ? "map.setting.weather.clear" : "map.setting.weather.rain",
            Map.of()));
    values.put("weather_cycle", booleanLabel(settings.weatherCycle()));
    values.put(
        "difficulty",
        message(
            "map.setting.difficulty." + settings.difficulty().toLowerCase(Locale.ROOT), Map.of()));
    values.put("pvp", booleanLabel(settings.pvp()));
    values.put("creatures", booleanLabel(settings.allowAnimals() || settings.allowMonsters()));
    values.put("fire_tick", booleanLabel(settings.fireTick()));
    values.put("environmental_damage", booleanLabel(settings.environmentalDamage()));
    values.put("auto_save", booleanLabel(settings.autoSave()));
    return values;
  }

  private Map<String, Object> operationPlaceholders(String id) {
    MapTemplate template = maps.find(id).orElse(null);
    if (template == null) return Map.of("map_id", id);
    Map<String, Object> values = new LinkedHashMap<>(menuPlaceholders(template));
    operations
        .find(template.id())
        .ifPresent(operation -> values.putAll(operationValues(operation)));
    return Map.copyOf(values);
  }

  private Map<String, Object> operationValues(MapOperationSnapshot operation) {
    long seconds =
        Math.max(0, Duration.between(operation.startedAt(), clock.instant()).toSeconds());
    return Map.of(
        "operation_type",
        message("map.operation.type." + operation.type().name().toLowerCase(Locale.ROOT), Map.of()),
        "operation_status",
        message(
            "map.operation.status." + operation.status().name().toLowerCase(Locale.ROOT), Map.of()),
        "operation_duration",
        seconds + " s",
        "operation_detail",
        operation.detail());
  }

  private String operationItemKey(String id) {
    return maps.find(id)
        .flatMap(template -> operations.find(template.id()))
        .map(
            operation ->
                switch (operation.status()) {
                  case PENDING, RUNNING -> "map.operation.running-v4";
                  case SUCCESS -> "map.operation.success-v4";
                  case FAILED, CANCELLED -> "map.operation.failed-v4";
                })
        .orElse("map.operation.success-v4");
  }

  private String typeLabel(MapType type) {
    return message("map.type." + type.name().toLowerCase(Locale.ROOT), Map.of());
  }

  private String stateLabel(MapState state) {
    return message("map.state." + state.name().toLowerCase(Locale.ROOT), Map.of());
  }

  private String filterLabel(MapListFilter filter) {
    return message("map.filter." + filter.name().toLowerCase(Locale.ROOT), Map.of());
  }

  private String sortLabel(String sort) {
    return message("map.sort." + sort.toLowerCase(Locale.ROOT), Map.of());
  }

  private String directionLabel(MapSortDirection direction) {
    return message("map.sort." + direction.name().toLowerCase(Locale.ROOT), Map.of());
  }

  private String dirtyLabel(MapTemplate template) {
    if (template.dirty()) return message("map.dirty.unsaved", Map.of());
    if (template.lastSavedAt().isPresent()) return message("map.dirty.saved", Map.of());
    return message("map.dirty.possible", Map.of());
  }

  private String booleanLabel(boolean value) {
    return message(value ? "map.setting.enabled" : "map.setting.disabled", Map.of());
  }

  private static String entryKey(MapTemplate template) {
    if (template.state() == MapState.ERROR) return "map.list.entry-error-v4";
    return template.loaded() ? "map.list.entry-loaded-v4" : "map.list.entry-unloaded-v4";
  }

  private static String validationKey(ProblemSeverity severity) {
    return switch (severity) {
      case INFO -> "map.validation.info-v4";
      case WARNING -> "map.validation.warning-v4";
      case ERROR, CRITICAL -> "map.validation.error-v4";
    };
  }

  private static String errorKey(MapOperationCode code) {
    return switch (code) {
      case NOT_FOUND -> "map.error.not-found";
      case ALREADY_EXISTS -> "map.error.already-exists";
      case INVALID_ID, INVALID_ARGUMENT -> "map.error.invalid-argument";
      case ALREADY_LOADED -> "map.error.already-loaded";
      case NOT_LOADED -> "map.error.not-loaded";
      case PLAYERS_PRESENT -> "map.error.players-present";
      case OPERATION_IN_PROGRESS -> "map.error.operation-running";
      case MAP_LINKED -> "map.error.linked";
      case MAP_PROTECTED -> "map.error.protected";
      case WORLD_CREATION_FAILED -> "map.error.world-creation";
      case WORLD_LOAD_FAILED -> "map.error.world-load";
      case WORLD_SAVE_FAILED -> "map.error.world-save";
      case WORLD_UNLOAD_FAILED -> "map.error.world-unload";
      case WORLD_SETTINGS_FAILED -> "map.error.world-settings";
      case COPY_FAILED -> "map.error.copy";
      case BACKUP_FAILED -> "map.error.backup";
      case CONFLICT -> "map.error.conflict";
      case INVALID_TYPE -> "map.error.invalid-type";
      case STORAGE_ERROR -> "map.error.storage";
      case SUCCESS -> "map.feedback.success";
    };
  }

  private String message(String key, Map<String, ?> values) {
    PlaceholderContext.Builder placeholders = PlaceholderContext.builder();
    values.forEach(placeholders::put);
    return configurations
        .language()
        .message(key, configurations.snapshot().plugin().locale(), placeholders.build());
  }

  private void send(UUID playerId, String key, Map<String, ?> values) {
    Player player = Bukkit.getPlayer(playerId);
    if (player != null) player.sendMessage(message(key, values));
  }

  private static String playerName(UUID playerId) {
    Player player = Bukkit.getPlayer(playerId);
    return player == null ? playerId.toString() : player.getName();
  }
}
