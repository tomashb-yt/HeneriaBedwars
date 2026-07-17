package fr.heneria.bedwars.plugin.arena;

import fr.heneria.bedwars.core.arena.ArenaBoundary;
import fr.heneria.bedwars.core.arena.ArenaDefinition;
import fr.heneria.bedwars.core.arena.ArenaLocation;
import fr.heneria.bedwars.core.arena.ArenaOperationCode;
import fr.heneria.bedwars.core.arena.ArenaOperationResult;
import fr.heneria.bedwars.core.arena.ArenaProblem;
import fr.heneria.bedwars.core.arena.ArenaService;
import fr.heneria.bedwars.core.arena.ArenaStatus;
import fr.heneria.bedwars.core.arena.ArenaTeamDefinition;
import fr.heneria.bedwars.core.arena.ArenaValidationResult;
import fr.heneria.bedwars.core.arena.ArenaVector;
import fr.heneria.bedwars.core.arena.TeamColor;
import fr.heneria.bedwars.core.arena.TeamId;
import fr.heneria.bedwars.core.arena.editor.ArenaEditorSection;
import fr.heneria.bedwars.core.arena.editor.ArenaEditorStateStore;
import fr.heneria.bedwars.core.arena.editor.ArenaEditorViewState;
import fr.heneria.bedwars.core.arena.editor.ArenaProblemRouter;
import fr.heneria.bedwars.core.command.AdministrativeCommandPolicy;
import fr.heneria.bedwars.core.config.ArenaEditorSettings;
import fr.heneria.bedwars.core.config.PlaceholderContext;
import fr.heneria.bedwars.core.config.ProblemSeverity;
import fr.heneria.bedwars.core.game.GameInstance;
import fr.heneria.bedwars.core.game.GameInstanceManager;
import fr.heneria.bedwars.core.game.GameOperationResult;
import fr.heneria.bedwars.core.game.lobby.GameJoinResult;
import fr.heneria.bedwars.core.game.lobby.GameLobbyService;
import fr.heneria.bedwars.core.gui.ConfirmationGui;
import fr.heneria.bedwars.core.gui.Gui;
import fr.heneria.bedwars.core.gui.GuiButton;
import fr.heneria.bedwars.core.gui.GuiClickContext;
import fr.heneria.bedwars.core.gui.GuiClickType;
import fr.heneria.bedwars.core.gui.TextInputCancelReason;
import fr.heneria.bedwars.core.gui.TextInputRequest;
import fr.heneria.bedwars.core.gui.TextInputService;
import fr.heneria.bedwars.core.logging.ProjectLogger;
import fr.heneria.bedwars.core.map.MapState;
import fr.heneria.bedwars.core.map.MapTemplateService;
import fr.heneria.bedwars.core.map.MapType;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import fr.heneria.bedwars.plugin.game.GameAdminMenuFactory;
import fr.heneria.bedwars.plugin.gui.GuiService;
import fr.heneria.bedwars.plugin.gui.StandardGuiButtons;
import fr.heneria.bedwars.plugin.map.MapMenuFactory;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntFunction;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Complete Ticket 006 arena administration surface.
 *
 * <p>Menus read only the active {@link ArenaService}; every accepted change is persisted
 * immediately with the revision captured when the view was built.
 */
public final class ArenaEditorMenuFactory {
  private static final List<Integer> VALIDATION_SLOTS =
      List.of(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34);
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
  private final ArenaService arenas;
  private final ConfigurationService configurations;
  private final GuiService gui;
  private final TextInputService input;
  private final ArenaEditorStateStore states;
  private final ProjectLogger logger;
  private final MapTemplateService maps;
  private final MapMenuFactory mapMenus;
  private final GameInstanceManager games;
  private final GameLobbyService lobby;
  private final GameAdminMenuFactory gameMenus;
  private final StandardGuiButtons standard = new StandardGuiButtons();

  public ArenaEditorMenuFactory(
      JavaPlugin plugin,
      ArenaService arenas,
      ConfigurationService configurations,
      GuiService gui,
      TextInputService input,
      ArenaEditorStateStore states,
      ProjectLogger logger,
      MapTemplateService maps,
      MapMenuFactory mapMenus,
      GameInstanceManager games,
      GameLobbyService lobby,
      GameAdminMenuFactory gameMenus) {
    this.plugin = plugin;
    this.arenas = arenas;
    this.configurations = configurations;
    this.gui = gui;
    this.input = input;
    this.states = states;
    this.logger = logger;
    this.maps = maps;
    this.mapMenus = mapMenus;
    this.games = games;
    this.lobby = lobby;
    this.gameMenus = gameMenus;
  }

  public Gui setup(UUID playerId) {
    long enabled = arenas.list().stream().filter(ArenaDefinition::enabled).count();
    long invalid =
        arenas.list().stream()
            .filter(
                arena ->
                    arena.status() == ArenaStatus.INVALID || arena.status() == ArenaStatus.ERROR)
            .count();
    long draft =
        arenas.list().stream().filter(arena -> arena.status() == ArenaStatus.DRAFT).count();
    return Gui.builder()
        .id("admin.setup")
        .title(message("admin.main.title-v2", Map.of()))
        .rows(5)
        .fillEmptySlots(true)
        .button(
            4,
            GuiButton.builder()
                .itemKey("admin.main.overview")
                .itemPlaceholders(
                    context ->
                        Map.of(
                            "arena_count",
                            arenas.list().size(),
                            "map_count",
                            maps.list().size(),
                            "enabled_count",
                            enabled,
                            "invalid_count",
                            invalid))
                .build())
        .button(
            11,
            GuiButton.builder()
                .itemKey("admin.main.arenas-v2")
                .itemPlaceholders(
                    context ->
                        Map.of(
                            "arena_count", arenas.list().size(),
                            "enabled_count", enabled,
                            "invalid_count", invalid,
                            "draft_count", draft))
                .permission(AdministrativeCommandPolicy.ARENA_MENU)
                .onLeftClick(context -> context.open(list(playerId)))
                .build())
        .button(
            15,
            GuiButton.builder()
                .itemKey("admin.main.maps")
                .itemPlaceholders(
                    context ->
                        Map.of(
                            "map_count", maps.list().size(),
                            "loaded_map_count",
                                maps.list().stream().filter(map -> map.loaded()).count()))
                .permission(AdministrativeCommandPolicy.MAP_MENU)
                .onLeftClick(context -> context.open(mapMenus.list(playerId)))
                .build())
        .button(
            20,
            GuiButton.builder()
                .itemKey("admin.main.create-arena")
                .permission(AdministrativeCommandPolicy.ARENA_CREATE)
                .onLeftClick(context -> requestArenaId(playerId))
                .build())
        .button(22, GuiButton.builder().itemKey("admin.main.guide").build())
        .button(
            24,
            GuiButton.builder()
                .itemKey("admin.main.create-map")
                .permission(AdministrativeCommandPolicy.MAP_CREATE)
                .onLeftClick(context -> mapMenus.beginCreate(playerId))
                .build())
        .button(
            29,
            GuiButton.builder()
                .itemKey("admin.main.games-v5")
                .itemPlaceholders(
                    context ->
                        Map.of(
                            "game_count", games.size(),
                            "waiting_count",
                                games.all().stream()
                                    .filter(
                                        game ->
                                            game.state()
                                                == fr.heneria.bedwars.api.game.GameState.WAITING)
                                    .count(),
                            "starting_count",
                                games.all().stream()
                                    .filter(
                                        game ->
                                            game.state()
                                                == fr.heneria.bedwars.api.game.GameState.STARTING)
                                    .count(),
                            "playing_count",
                                games.all().stream()
                                    .filter(
                                        game ->
                                            game.state()
                                                == fr.heneria.bedwars.api.game.GameState.PLAYING)
                                    .count()))
                .permission(AdministrativeCommandPolicy.GAME_LIST)
                .onLeftClick(context -> context.open(gameMenus.list(playerId)))
                .build())
        .button(
            31,
            GuiButton.builder()
                .itemKey("admin.main.configuration-v2")
                .itemPlaceholders(context -> configurationPlaceholders())
                .onLeftClick(context -> context.open(configurationInfo(playerId)))
                .build())
        .button(40, standard.close())
        .build();
  }

  public Gui list(UUID playerId) {
    ArenaEditorViewState state = states.state(playerId);
    ArenaEditorSettings settings = settings();
    List<ArenaDefinition> visible =
        arenas.list().stream()
            .filter(state.filter()::accepts)
            .sorted(state.sort().comparator())
            .toList();
    int pageSize = settings.contentSlots().size();
    int maximumPage = Math.max(1, (visible.size() + pageSize - 1) / pageSize);
    state.page(Math.min(state.page(), maximumPage - 1));
    Gui.Builder builder =
        Gui.builder()
            .id("arena.list")
            .title(
                message(
                    "arena.gui.list.title-v2",
                    Map.of(
                        "count",
                        visible.size(),
                        "filter",
                        displayFilter(state.filter().name()),
                        "sort",
                        displaySort(state.sort().name()))))
            .rows(settings.listRows())
            .fillEmptySlots(true)
            .data("filter", state.filter().name())
            .data("sort", state.sort().name())
            .data("max_page", maximumPage)
            .button(
                4,
                GuiButton.builder()
                    .itemKey("arena.list.guide-v3")
                    .itemPlaceholders(
                        context ->
                            Map.of(
                                "visible_count",
                                visible.size(),
                                "arena_count",
                                arenas.list().size(),
                                "filter",
                                displayFilter(state.filter().name()),
                                "sort",
                                displaySort(state.sort().name())))
                    .build());
    if (visible.isEmpty())
      builder.button(
          22,
          GuiButton.builder()
              .itemKey("arena.list.empty-v3")
              .itemPlaceholders(context -> Map.of("filter", displayFilter(state.filter().name())))
              .build());
    for (int index = 0; index < pageSize; index++) {
      int absolute = state.page() * pageSize + index;
      if (absolute >= visible.size()) break;
      ArenaDefinition arena = visible.get(absolute);
      ArenaValidationResult validation = arenas.validate(arena);
      builder.button(
          settings.contentSlots().get(index),
          GuiButton.builder()
              .itemKey(entryKey(arena))
              .itemPlaceholders(context -> placeholders(arena, validation))
              .onLeftClick(context -> context.open(editor(playerId, arena.id().value())))
              .onRightClick(context -> context.open(validation(playerId, arena.id().value())))
              .on(
                  GuiClickType.SHIFT_RIGHT,
                  context -> context.open(deleteConfirmation(playerId, arena.id().value(), false)))
              .build());
    }
    if (state.page() > 0)
      builder.button(
          settings.previousPageSlot(),
          standard.previous(
              context -> {
                state.page(state.page() - 1);
                context.replace(list(playerId));
              }));
    if (state.page() + 1 < maximumPage)
      builder.button(
          settings.nextPageSlot(),
          standard.next(
              context -> {
                state.page(state.page() + 1);
                context.replace(list(playerId));
              }));
    return builder
        .button(
            settings.filterSlot(),
            GuiButton.builder()
                .itemKey("arena.list.filter-v2")
                .itemPlaceholders(context -> Map.of("filter", displayFilter(state.filter().name())))
                .onLeftClick(
                    context -> {
                      state.filter(state.filter().next());
                      context.replace(list(playerId));
                    })
                .build())
        .button(
            settings.sortSlot(),
            GuiButton.builder()
                .itemKey("arena.list.sort-v2")
                .itemPlaceholders(context -> Map.of("sort", displaySort(state.sort().name())))
                .onLeftClick(
                    context -> {
                      state.sort(state.sort().next());
                      context.replace(list(playerId));
                    })
                .build())
        .button(
            settings.createSlot(),
            GuiButton.builder()
                .itemKey("arena.list.create-v2")
                .permission(AdministrativeCommandPolicy.ARENA_CREATE)
                .onLeftClick(context -> requestArenaId(playerId))
                .build())
        .button(settings.listRows() * 9 - 8, dashboardButton(playerId))
        .button(settings.listRows() * 9 - 6, standard.close())
        .button(
            settings.listRows() * 9 - 4,
            GuiButton.builder()
                .itemKey("gui.refresh")
                .onLeftClick(context -> context.replace(list(playerId)))
                .build())
        .build();
  }

  private Gui configurationInfo(UUID playerId) {
    return Gui.builder()
        .id("admin.configuration")
        .title(message("admin.main.configuration-title-v3", Map.of()))
        .rows(4)
        .fillEmptySlots(true)
        .button(
            4,
            GuiButton.builder()
                .itemKey("admin.configuration.summary-v3")
                .itemPlaceholders(context -> configurationPlaceholders())
                .build())
        .button(
            11,
            GuiButton.builder()
                .itemKey("admin.configuration.language-v3")
                .itemPlaceholders(context -> configurationPlaceholders())
                .build())
        .button(
            13,
            GuiButton.builder()
                .itemKey("admin.configuration.resources-v3")
                .itemPlaceholders(context -> configurationPlaceholders())
                .build())
        .button(
            15,
            GuiButton.builder()
                .itemKey("admin.configuration.storage-v3")
                .itemPlaceholders(context -> configurationPlaceholders())
                .build())
        .button(27, dashboardButton(playerId))
        .button(35, standard.close())
        .build();
  }

  public Gui editor(UUID playerId, String id) {
    ArenaDefinition arena = arenas.find(id).orElse(null);
    if (arena == null) return missing(playerId);
    states.state(playerId).observe(id, arena.revision());
    long expected = arena.revision();
    ArenaValidationResult validation = arenas.validate(arena);
    ArenaEditorSettings settings = settings();
    Gui.Builder builder =
        Gui.builder()
            .id("arena.editor." + id)
            .title(message("arena.gui.editor.assistant-title", placeholders(arena, validation)))
            .rows(settings.editorRows())
            .fillEmptySlots(true)
            .data("arena_id", id)
            .data("arena_revision", expected)
            .button(settings.informationSlot(), infoButton(arena, validation))
            .button(
                settings.worldSlot(),
                GuiButton.builder()
                    .itemKey("arena.editor.assistant-map-v5")
                    .itemPlaceholders(context -> placeholders(arena, validation))
                    .permission(AdministrativeCommandPolicy.ARENA_EDIT)
                    .onLeftClick(context -> context.open(mapTemplates(playerId, id, expected)))
                    .onRightClick(
                        context ->
                            withPlayer(
                                playerId,
                                player ->
                                    mutate(
                                        context,
                                        playerId,
                                        id,
                                        arenas.setWorld(id, player.getWorld().getName(), expected),
                                        "world")))
                    .build())
            .button(
                settings.waitingSlot(), positionButton(playerId, arena, expected, true, validation))
            .button(
                settings.spectatorSlot(),
                positionButton(playerId, arena, expected, false, validation))
            .button(
                settings.teamsSlot(),
                action(
                    "arena.editor.assistant-teams-v5",
                    AdministrativeCommandPolicy.ARENA_EDIT,
                    placeholders(arena, validation),
                    context -> context.open(teams(playerId, id, expected))))
            .button(
                settings.validationSlot(),
                GuiButton.builder()
                    .itemKey(
                        validation.valid()
                            ? "arena.editor.assistant-review-ready-v5"
                            : "arena.editor.assistant-review-v5")
                    .itemPlaceholders(context -> placeholders(arena, validation))
                    .onLeftClick(context -> context.open(validation(playerId, id)))
                    .build())
            .button(settings.enableSlot(), activationButton(playerId, arena, expected, validation))
            .button(
                settings.deleteSlot(),
                action(
                    "arena.editor.assistant-delete-v5",
                    AdministrativeCommandPolicy.ARENA_DELETE,
                    placeholders(arena, validation),
                    context -> context.open(deleteConfirmation(playerId, id, false))))
            .button(
                settings.editorBackSlot(),
                GuiButton.builder()
                    .itemKey("gui.back")
                    .onLeftClick(context -> context.replace(list(playerId)))
                    .build())
            .button(settings.editorCloseSlot(), standard.close());
    for (int index = 0;
        index < Math.min(settings.teamSlots().size(), arena.teams().size());
        index++) {
      ArenaTeamDefinition team = arena.teams().get(index);
      Map<String, Object> values = teamPlaceholders(arena, validation, team);
      builder.button(
          settings.teamSlots().get(index),
          GuiButton.builder()
              .itemKey(teamEntryKey(team.color()))
              .itemPlaceholders(context -> values)
              .permission(AdministrativeCommandPolicy.ARENA_EDIT)
              .onLeftClick(context -> context.open(teamEditor(playerId, id, team.id(), expected)))
              .build());
    }
    return builder.build();
  }

  public Gui worlds(UUID playerId, String id, long expected) {
    ArenaDefinition arena = arenas.find(id).orElse(null);
    if (arena == null) return missing(playerId);
    List<World> worlds = Bukkit.getWorlds();
    Gui.Builder builder =
        Gui.builder()
            .id("arena.worlds." + id)
            .title(message("arena.world.title", Map.of("arena_name", arena.displayName())))
            .rows(6)
            .fillEmptySlots(true);
    List<Integer> slots = settings().contentSlots();
    for (int index = 0; index < Math.min(slots.size(), worlds.size()); index++) {
      World world = worlds.get(index);
      Map<String, Object> values =
          Map.of(
              "world", world.getName(),
              "environment", world.getEnvironment().name(),
              "world_players", world.getPlayers().size(),
              "world_current", currentWorld(playerId, world),
              "world_selected", arena.worldName().filter(world.getName()::equals).isPresent());
      builder.button(
          slots.get(index),
          GuiButton.builder()
              .itemKey("arena.world.entry")
              .itemPlaceholders(context -> values)
              .onLeftClick(
                  context ->
                      mutate(
                          context,
                          playerId,
                          id,
                          arenas.setWorld(id, world.getName(), expected),
                          "world"))
              .onRightClick(context -> teleportWorld(context, playerId, world))
              .build());
    }
    return builder.button(45, editorBackButton(playerId, id)).button(49, standard.close()).build();
  }

  /** Selects a persistent BedWars map id; arena YAML remains the relation source of truth. */
  public Gui mapTemplates(UUID playerId, String id, long expected) {
    ArenaDefinition arena = arenas.find(id).orElse(null);
    if (arena == null) return missing(playerId);
    var templates =
        maps.list().stream()
            .filter(template -> template.type() == MapType.BEDWARS)
            .filter(template -> template.state() != MapState.ERROR)
            .toList();
    Gui.Builder builder =
        Gui.builder()
            .id("arena.map-templates." + id)
            .title(message("arena.map.assistant-title", Map.of("arena_name", arena.displayName())))
            .rows(6)
            .fillEmptySlots(true)
            .button(
                4,
                GuiButton.builder()
                    .itemKey("arena.map.assistant-guide")
                    .itemPlaceholders(context -> placeholders(arena, arenas.validate(arena)))
                    .build());
    List<Integer> slots = settings().contentSlots();
    if (templates.isEmpty())
      builder.button(
          22,
          GuiButton.builder()
              .itemKey("arena.map.empty")
              .itemPlaceholders(context -> Map.of("arena_name", arena.displayName()))
              .build());
    for (int index = 0; index < Math.min(slots.size(), templates.size()); index++) {
      var template = templates.get(index);
      Map<String, Object> values = new LinkedHashMap<>(mapMenus.menuPlaceholders(template));
      boolean selected = arena.template().filter(template.id().value()::equals).isPresent();
      values.put("selected", selected);
      values.put("selected_label", selected ? "✓ Déjà sélectionnée" : "Cliquez pour choisir");
      builder.button(
          slots.get(index),
          GuiButton.builder()
              .itemKey(
                  template.loaded()
                      ? "arena.map.assistant-choice-loaded"
                      : "arena.map.assistant-choice-unloaded")
              .itemPlaceholders(context -> values)
              .permission(AdministrativeCommandPolicy.ARENA_EDIT)
              .onLeftClick(
                  context -> {
                    ArenaOperationResult result =
                        arenas.setMapTemplate(
                            id, template.id().value(), template.worldName(), expected);
                    if (result.successful()) maps.synchronizeLinks(arenas.list());
                    mutate(context, playerId, id, result, "map-template");
                  })
              .build());
    }
    return builder
        .button(45, editorBackButton(playerId, id))
        .button(
            49,
            GuiButton.builder()
                .itemKey("arena.map.create")
                .permission(AdministrativeCommandPolicy.MAP_CREATE)
                .onLeftClick(context -> requestMapForArena(playerId, id, expected))
                .build())
        .button(53, standard.close())
        .build();
  }

  public Gui players(UUID playerId, String id, long expected) {
    ArenaDefinition arena = arenas.find(id).orElse(null);
    if (arena == null) return missing(playerId);
    ArenaValidationResult validation = arenas.validate(arena);
    return Gui.builder()
        .id("arena.players." + id)
        .title(message("arena.players.assistant-title", placeholders(arena, validation)))
        .rows(3)
        .fillEmptySlots(true)
        .button(11, numberButton(playerId, arena, expected, true))
        .button(15, numberButton(playerId, arena, expected, false))
        .button(
            13,
            GuiButton.builder()
                .itemKey("arena.players.assistant-summary")
                .itemPlaceholders(context -> placeholders(arena, validation))
                .build())
        .button(22, editorBackButton(playerId, id))
        .build();
  }

  public Gui teams(UUID playerId, String id, long expected) {
    ArenaDefinition arena = arenas.find(id).orElse(null);
    if (arena == null) return missing(playerId);
    ArenaValidationResult validation = arenas.validate(arena);
    Gui.Builder builder =
        Gui.builder()
            .id("arena.teams." + id)
            .title(message("arena.teams.assistant-title", placeholders(arena, validation)))
            .rows(5)
            .fillEmptySlots(true)
            .button(10, teamNumberButton(playerId, arena, expected, true))
            .button(16, teamNumberButton(playerId, arena, expected, false))
            .button(
                13,
                GuiButton.builder()
                    .itemKey("arena.teams.assistant-overview-v2")
                    .itemPlaceholders(context -> placeholders(arena, validation))
                    .build())
            .button(40, editorBackButton(playerId, id))
            .button(44, standard.close());
    List<Integer> slots = List.of(19, 20, 21, 22, 23, 24, 25, 31);
    for (int index = 0; index < Math.min(slots.size(), arena.teams().size()); index++) {
      ArenaTeamDefinition team = arena.teams().get(index);
      Map<String, Object> values = teamPlaceholders(arena, validation, team);
      builder.button(
          slots.get(index),
          GuiButton.builder()
              .itemKey(teamEntryKey(team.color()))
              .itemPlaceholders(context -> values)
              .permission(AdministrativeCommandPolicy.ARENA_EDIT)
              .onLeftClick(context -> context.open(teamEditor(playerId, id, team.id(), expected)))
              .build());
    }
    return builder.build();
  }

  private Gui teamEditor(UUID playerId, String id, TeamId teamId, long expected) {
    ArenaDefinition arena = arenas.find(id).orElse(null);
    if (arena == null) return missing(playerId);
    ArenaTeamDefinition team =
        arena.teams().stream().filter(value -> value.id().equals(teamId)).findFirst().orElse(null);
    if (team == null) return teams(playerId, id, arena.revision());
    ArenaValidationResult validation = arenas.validate(arena);
    Map<String, Object> values = teamPlaceholders(arena, validation, team);
    Gui.Builder builder =
        Gui.builder()
            .id("arena.team." + id + '.' + team.id().value())
            .title(
                message(
                    "arena.teams.team-title-"
                        + team.color().name().toLowerCase(Locale.ROOT)
                        + "-v6",
                    values))
            .rows(5)
            .fillEmptySlots(true)
            .button(
                4,
                GuiButton.builder()
                    .itemKey(teamEntryKey(team.color()))
                    .itemPlaceholders(context -> values)
                    .build())
            .button(
                11,
                GuiButton.builder()
                    .itemKey(
                        team.spawn().isPresent()
                            ? "arena.teams.spawn-configured-v6"
                            : "arena.teams.spawn-missing-v6")
                    .itemPlaceholders(context -> values)
                    .build())
            .button(
                15,
                GuiButton.builder()
                    .itemKey(
                        team.bedDefinition().isPresent()
                            ? "arena.teams.bed-configured-v6"
                            : "arena.teams.bed-missing-v6")
                    .itemPlaceholders(context -> values)
                    .build())
            .button(
                13,
                GuiButton.builder()
                    .itemKey("arena.teams.guide-v6")
                    .itemPlaceholders(context -> values)
                    .build())
            .button(
                19,
                action(
                    "arena.teams.set-spawn-v6",
                    AdministrativeCommandPolicy.ARENA_EDIT,
                    values,
                    context -> setTeamSpawn(context, playerId, arena, team, expected)))
            .button(
                23,
                action(
                    "arena.teams.set-bed-v6",
                    AdministrativeCommandPolicy.ARENA_EDIT,
                    values,
                    context -> selectTeamBed(context, playerId, arena, team, expected)))
            .button(
                36,
                GuiButton.builder()
                    .itemKey("gui.back")
                    .onLeftClick(context -> context.replace(editor(playerId, id)))
                    .build())
            .button(
                40,
                GuiButton.builder()
                    .itemKey("arena.teams.review")
                    .itemPlaceholders(context -> values)
                    .onLeftClick(context -> context.open(validation(playerId, id)))
                    .build())
            .button(44, standard.close());
    if (team.spawn().isPresent()) {
      builder
          .button(
              20,
              action(
                  "arena.teams.teleport-spawn-v6",
                  AdministrativeCommandPolicy.ARENA_TELEPORT,
                  values,
                  context ->
                      teleportTeamSpawn(context, playerId, team, team.spawn().orElseThrow())))
          .button(
              21,
              action(
                  "arena.teams.clear-spawn-v6",
                  AdministrativeCommandPolicy.ARENA_EDIT,
                  values,
                  context ->
                      mutateTeam(
                          context,
                          playerId,
                          id,
                          team.id(),
                          arenas.clearTeamSpawn(id, team.id(), expected),
                          "team-spawn-clear")));
    } else
      builder
          .button(
              20,
              GuiButton.builder()
                  .itemKey("arena.teams.teleport-spawn-disabled-v6")
                  .itemPlaceholders(context -> values)
                  .build())
          .button(
              21,
              GuiButton.builder()
                  .itemKey("arena.teams.clear-spawn-disabled-v6")
                  .itemPlaceholders(context -> values)
                  .build());
    if (team.bedDefinition().isPresent()) {
      builder
          .button(
              24,
              action(
                  "arena.teams.teleport-bed-v6",
                  AdministrativeCommandPolicy.ARENA_TELEPORT,
                  values,
                  context ->
                      teleportBed(context, playerId, team, team.bedLocation().orElseThrow())))
          .button(
              25,
              action(
                  "arena.teams.clear-bed-v6",
                  AdministrativeCommandPolicy.ARENA_EDIT,
                  values,
                  context ->
                      mutateTeam(
                          context,
                          playerId,
                          id,
                          team.id(),
                          arenas.clearTeamBed(id, team.id(), expected),
                          "team-bed-clear")));
    } else
      builder
          .button(
              24,
              GuiButton.builder()
                  .itemKey("arena.teams.teleport-bed-disabled-v6")
                  .itemPlaceholders(context -> values)
                  .build())
          .button(
              25,
              GuiButton.builder()
                  .itemKey("arena.teams.clear-bed-disabled-v6")
                  .itemPlaceholders(context -> values)
                  .build());
    return builder.build();
  }

  public Gui boundary(UUID playerId, String id, long expected) {
    ArenaDefinition arena = arenas.find(id).orElse(null);
    if (arena == null) return missing(playerId);
    ArenaBoundary value = arena.boundary().orElseGet(ArenaBoundary::empty);
    Map<String, Object> placeholders = placeholders(arena, arenas.validate(arena));
    return Gui.builder()
        .id("arena.boundary." + id)
        .title(message("arena.boundary.assistant-title", placeholders))
        .rows(4)
        .fillEmptySlots(true)
        .button(
            10,
            GuiButton.builder()
                .itemKey("arena.boundary.assistant-minimum")
                .itemPlaceholders(context -> boundaryPlaceholders(placeholders, value))
                .permission(AdministrativeCommandPolicy.ARENA_EDIT)
                .onLeftClick(
                    context ->
                        withPlayer(
                            playerId,
                            player ->
                                mutate(
                                    context,
                                    playerId,
                                    id,
                                    arenas.setBoundaryMinimum(
                                        id, vector(player.getLocation()), expected),
                                    "boundary-minimum")))
                .onRightClick(
                    context ->
                        value
                            .minimum()
                            .ifPresentOrElse(
                                point -> teleportBoundary(playerId, arena, point),
                                () -> send(playerId, "arena.gui.position.missing", Map.of())))
                .build())
        .button(
            12,
            GuiButton.builder()
                .itemKey("arena.boundary.assistant-maximum")
                .itemPlaceholders(context -> boundaryPlaceholders(placeholders, value))
                .permission(AdministrativeCommandPolicy.ARENA_EDIT)
                .onLeftClick(
                    context ->
                        withPlayer(
                            playerId,
                            player ->
                                mutate(
                                    context,
                                    playerId,
                                    id,
                                    arenas.setBoundaryMaximum(
                                        id, vector(player.getLocation()), expected),
                                    "boundary-maximum")))
                .onRightClick(
                    context ->
                        value
                            .maximum()
                            .ifPresentOrElse(
                                point -> teleportBoundary(playerId, arena, point),
                                () -> send(playerId, "arena.gui.position.missing", Map.of())))
                .build())
        .button(
            14,
            action(
                value.enabled()
                    ? "arena.boundary.assistant-enabled"
                    : "arena.boundary.assistant-disabled",
                AdministrativeCommandPolicy.ARENA_EDIT,
                boundaryPlaceholders(placeholders, value),
                context ->
                    mutate(
                        context,
                        playerId,
                        id,
                        arenas.setBoundaryEnabled(id, !value.enabled(), expected),
                        "boundary-enabled")))
        .button(
            16,
            action(
                "arena.boundary.assistant-reset",
                AdministrativeCommandPolicy.ARENA_EDIT,
                placeholders,
                context -> context.open(clearBoundaryConfirmation(playerId, id, expected))))
        .button(31, editorBackButton(playerId, id))
        .build();
  }

  public Gui validation(UUID playerId, String id) {
    ArenaDefinition arena = arenas.find(id).orElse(null);
    if (arena == null) return missing(playerId);
    ArenaValidationResult result = arenas.validate(arena);
    Gui.Builder builder =
        Gui.builder()
            .id("arena.validation." + id)
            .title(message("arena.validation.title-v2", placeholders(arena, result)))
            .rows(5)
            .fillEmptySlots(true)
            .button(
                4,
                GuiButton.builder()
                    .itemKey(
                        result.valid()
                            ? "arena.validation.summary-valid"
                            : "arena.validation.summary-incomplete")
                    .itemPlaceholders(context -> placeholders(arena, result))
                    .build());
    List<Integer> slots = VALIDATION_SLOTS;
    for (int index = 0; index < Math.min(slots.size(), result.problems().size()); index++) {
      ArenaProblem problem = result.problems().get(index);
      Map<String, Object> values = new LinkedHashMap<>(placeholders(arena, result));
      values.put("severity", problem.severity().name());
      values.put("code", problem.code());
      values.put("field", problem.field());
      values.put("problem", localizedProblem(problem));
      values.put("solution", solution(problem));
      builder.button(
          slots.get(index),
          GuiButton.builder()
              .itemKey(validationItemKey(problem.severity()))
              .itemPlaceholders(context -> values)
              .onLeftClick(
                  context ->
                      context.open(
                          section(
                              playerId,
                              arena.id().value(),
                              ArenaProblemRouter.section(problem.field()))))
              .build());
    }
    return builder
        .button(36, editorBackButton(playerId, id))
        .button(
            40,
            GuiButton.builder()
                .itemKey("gui.refresh")
                .onLeftClick(context -> context.replace(validation(playerId, id)))
                .build())
        .button(44, standard.close())
        .build();
  }

  public Gui deleteMenu(UUID playerId, String id) {
    return deleteConfirmation(playerId, id, false);
  }

  private Gui section(UUID playerId, String id, ArenaEditorSection section) {
    ArenaDefinition arena = arenas.find(id).orElse(null);
    if (arena == null) return missing(playerId);
    return switch (section) {
      case WORLD -> mapTemplates(playerId, id, arena.revision());
      case PLAYERS -> players(playerId, id, arena.revision());
      case TEAMS -> teams(playerId, id, arena.revision());
      case BOUNDARY -> boundary(playerId, id, arena.revision());
      default -> editor(playerId, id);
    };
  }

  private GuiButton positionButton(
      UUID playerId,
      ArenaDefinition arena,
      long expected,
      boolean waiting,
      ArenaValidationResult validation) {
    Optional<ArenaLocation> location =
        waiting ? arena.waitingLocation() : arena.spectatorLocation();
    String key =
        waiting ? "arena.editor.assistant-waiting-v5" : "arena.editor.assistant-spectator-v5";
    return GuiButton.builder()
        .itemKey(key)
        .itemPlaceholders(context -> positionPlaceholders(arena, validation, location))
        .permission(AdministrativeCommandPolicy.ARENA_EDIT)
        .onLeftClick(
            context ->
                withPlayer(
                    playerId,
                    player -> {
                      ArenaOperationResult result =
                          waiting
                              ? arenas.setWaiting(
                                  arena.id().value(),
                                  BukkitArenaLocations.from(player.getLocation()),
                                  expected)
                              : arenas.setSpectator(
                                  arena.id().value(),
                                  BukkitArenaLocations.from(player.getLocation()),
                                  expected);
                      mutate(
                          context,
                          playerId,
                          arena.id().value(),
                          result,
                          waiting ? "waiting" : "spectator");
                    }))
        .onRightClick(
            context ->
                location.ifPresentOrElse(
                    value -> teleport(context, playerId, value),
                    () -> send(playerId, "arena.gui.position.missing", Map.of())))
        .on(
            GuiClickType.SHIFT_RIGHT,
            context -> {
              if (location.isPresent())
                context.open(
                    clearPositionConfirmation(playerId, arena.id().value(), expected, waiting));
            })
        .build();
  }

  private GuiButton numberButton(
      UUID playerId, ArenaDefinition arena, long expected, boolean minimum) {
    String key = minimum ? "arena.players.minimum-assisted" : "arena.players.maximum-assisted";
    return GuiButton.builder()
        .itemKey(key)
        .itemPlaceholders(context -> placeholders(arena, arenas.validate(arena)))
        .permission(AdministrativeCommandPolicy.ARENA_EDIT)
        .on(
            GuiClickType.LEFT,
            context -> changePlayers(context, playerId, arena, expected, minimum, 1))
        .on(
            GuiClickType.RIGHT,
            context -> changePlayers(context, playerId, arena, expected, minimum, -1))
        .on(
            GuiClickType.SHIFT_LEFT,
            context -> changePlayers(context, playerId, arena, expected, minimum, 5))
        .on(
            GuiClickType.SHIFT_RIGHT,
            context -> changePlayers(context, playerId, arena, expected, minimum, -5))
        .on(GuiClickType.MIDDLE, context -> requestPlayerNumber(playerId, arena, expected, minimum))
        .build();
  }

  private GuiButton teamNumberButton(
      UUID playerId, ArenaDefinition arena, long expected, boolean count) {
    String key = count ? "arena.teams.count-assisted" : "arena.teams.size-assisted";
    return GuiButton.builder()
        .itemKey(key)
        .itemPlaceholders(context -> placeholders(arena, arenas.validate(arena)))
        .permission(AdministrativeCommandPolicy.ARENA_EDIT)
        .on(
            GuiClickType.LEFT,
            context -> proposeTeams(context, playerId, arena, expected, count, 1))
        .on(
            GuiClickType.RIGHT,
            context -> proposeTeams(context, playerId, arena, expected, count, -1))
        .on(
            GuiClickType.SHIFT_LEFT,
            context -> proposeTeams(context, playerId, arena, expected, count, 2))
        .on(
            GuiClickType.SHIFT_RIGHT,
            context -> proposeTeams(context, playerId, arena, expected, count, -2))
        .on(GuiClickType.MIDDLE, context -> requestTeamNumber(playerId, arena, expected, count))
        .build();
  }

  private GuiButton activationButton(
      UUID playerId, ArenaDefinition arena, long expected, ArenaValidationResult validation) {
    if (!arena.enabled() && !validation.valid())
      return GuiButton.builder()
          .itemKey("arena.editor.assistant-activation-blocked-v5")
          .itemPlaceholders(context -> placeholders(arena, validation))
          .onLeftClick(context -> context.open(validation(playerId, arena.id().value())))
          .build();
    if (arena.enabled())
      return GuiButton.builder()
          .itemKey("arena.editor.assistant-runtime-ready-v5")
          .itemPlaceholders(context -> placeholders(arena, validation))
          .onLeftClick(context -> launchOrJoin(context, playerId, arena.id().value()))
          .onRightClick(
              context ->
                  context.open(activationConfirmation(playerId, arena.id().value(), expected)))
          .build();
    return action(
        "arena.editor.assistant-activation-ready-v5",
        AdministrativeCommandPolicy.ARENA_ENABLE,
        placeholders(arena, validation),
        context -> context.open(activationConfirmation(playerId, arena.id().value(), expected)));
  }

  private void launchOrJoin(GuiClickContext context, UUID playerId, String arenaId) {
    Player player = player(playerId);
    if (!player.hasPermission(AdministrativeCommandPolicy.GAME_JOIN)
        || (!games.byArena(arenaId).isPresent()
            && !player.hasPermission(AdministrativeCommandPolicy.GAME_CREATE))) {
      send(playerId, "general.no-permission", Map.of());
      return;
    }
    Optional<GameInstance> current = games.byPlayer(playerId);
    if (current.isPresent()) {
      send(
          playerId,
          current.orElseThrow().arena().definition().id().value().equalsIgnoreCase(arenaId)
              ? "arena.runtime.already-joined"
              : "arena.runtime.player-occupied",
          Map.of());
      return;
    }
    context.close();
    Optional<GameInstance> existing = games.byArena(arenaId);
    if (existing.isPresent()) {
      joinRuntime(playerId, existing.orElseThrow());
      return;
    }
    send(playerId, "arena.runtime.creating", Map.of("arena_id", arenaId));
    games
        .create(arenaId)
        .whenComplete(
            (result, failure) ->
                main(
                    () -> {
                      if (failure != null || result == null || !result.successful()) {
                        runtimeFailure(playerId, result);
                        return;
                      }
                      joinRuntime(playerId, result.instance().orElseThrow());
                    }));
  }

  private void joinRuntime(UUID playerId, GameInstance instance) {
    lobby
        .join(instance.id(), playerId)
        .whenComplete(
            (result, failure) ->
                main(
                    () -> {
                      if (failure != null || result == null || !result.successful()) {
                        runtimeFailure(playerId, result);
                        return;
                      }
                      send(
                          playerId,
                          "arena.runtime.joined",
                          Map.of(
                              "arena_id", instance.arena().definition().id().value(),
                              "game_id", instance.id().shortId()));
                    }));
  }

  private void runtimeFailure(UUID playerId, GameOperationResult result) {
    send(
        playerId,
        "arena.runtime.failed",
        Map.of("code", result == null ? "INTERNAL_ERROR" : result.code().name()));
  }

  private void runtimeFailure(UUID playerId, GameJoinResult result) {
    send(
        playerId,
        "arena.runtime.failed",
        Map.of("code", result == null ? "INTERNAL_ERROR" : result.code().name()));
  }

  private void main(Runnable action) {
    if (Bukkit.isPrimaryThread()) action.run();
    else plugin.getServer().getScheduler().runTask(plugin, action);
  }

  private GuiButton infoButton(ArenaDefinition arena, ArenaValidationResult validation) {
    return GuiButton.builder()
        .itemKey("arena.editor.assistant-progress-v5")
        .itemPlaceholders(context -> placeholders(arena, validation))
        .build();
  }

  private Gui activationConfirmation(UUID playerId, String id, long expected) {
    ArenaDefinition arena = arenas.find(id).orElseThrow();
    return ConfirmationGui.builder()
        .id("arena.activation." + id)
        .title(
            message(
                arena.enabled() ? "arena.gui.editor.disable" : "arena.gui.editor.enable",
                Map.of("arena_name", arena.displayName())))
        .informationKey(
            arena.enabled()
                ? "arena.editor.assistant-disable"
                : "arena.editor.assistant-activation-ready")
        .informationPlaceholders(placeholders(arena, arenas.validate(arena)))
        .confirmItemKey("gui.confirm")
        .cancelItemKey("gui.cancel")
        .permission(
            arena.enabled()
                ? AdministrativeCommandPolicy.ARENA_DISABLE
                : AdministrativeCommandPolicy.ARENA_ENABLE)
        .onCancel(context -> context.replace(editor(playerId, id)))
        .onConfirm(
            context ->
                mutate(
                    context,
                    playerId,
                    id,
                    arena.enabled() ? arenas.disable(id, expected) : arenas.enable(id, expected),
                    arena.enabled() ? "disable" : "enable"))
        .build();
  }

  private Gui deleteConfirmation(UUID playerId, String id, boolean enabledConfirmed) {
    ArenaDefinition arena = arenas.find(id).orElse(null);
    if (arena == null) return missing(playerId);
    return ConfirmationGui.builder()
        .id("arena.delete." + id + '.' + enabledConfirmed)
        .title(message("arena.delete.title", Map.of("arena_name", arena.displayName())))
        .informationKey("arena.editor.assistant-delete")
        .informationPlaceholders(placeholders(arena, arenas.validate(arena)))
        .confirmItemKey("gui.confirm")
        .cancelItemKey("gui.cancel")
        .permission(AdministrativeCommandPolicy.ARENA_DELETE)
        .onCancel(context -> context.replace(editor(playerId, id)))
        .onConfirm(
            context -> {
              if (arena.enabled() && !enabledConfirmed) {
                context.open(deleteConfirmation(playerId, id, true));
                return;
              }
              ArenaOperationResult result = arenas.delete(id, arena.revision());
              if (result.successful()) {
                logger.info("[Arena] " + playerName(playerId) + " deleted '" + id + "'.");
                send(playerId, "arena.delete.success", Map.of("arena_id", id));
                states.state(playerId).forget(id);
                openDashboard(playerId);
              } else mutationFailure(playerId, result);
            })
        .build();
  }

  private Gui clearPositionConfirmation(UUID playerId, String id, long expected, boolean waiting) {
    ArenaDefinition arena = arenas.find(id).orElse(null);
    if (arena == null) return missing(playerId);
    return ConfirmationGui.builder()
        .id("arena.position.clear." + id + '.' + waiting)
        .title(message("arena.gui.position.clear-title", Map.of("arena_id", id)))
        .informationKey(
            waiting ? "arena.editor.assistant-waiting" : "arena.editor.assistant-spectator")
        .informationPlaceholders(placeholders(arena, arenas.validate(arena)))
        .confirmItemKey("gui.confirm")
        .cancelItemKey("gui.cancel")
        .permission(AdministrativeCommandPolicy.ARENA_EDIT)
        .onCancel(context -> context.replace(editor(playerId, id)))
        .onConfirm(
            context ->
                mutate(
                    context,
                    playerId,
                    id,
                    waiting
                        ? arenas.clearWaiting(id, expected)
                        : arenas.clearSpectator(id, expected),
                    waiting ? "waiting-clear" : "spectator-clear"))
        .build();
  }

  private Gui clearBoundaryConfirmation(UUID playerId, String id, long expected) {
    return ConfirmationGui.builder()
        .id("arena.boundary.clear." + id)
        .title(message("arena.boundary.reset", Map.of("arena_id", id)))
        .informationKey("arena.boundary.assistant-reset")
        .informationPlaceholders(Map.of("arena_id", id))
        .confirmItemKey("gui.confirm")
        .cancelItemKey("gui.cancel")
        .permission(AdministrativeCommandPolicy.ARENA_EDIT)
        .onCancel(context -> context.replace(boundary(playerId, id, expected)))
        .onConfirm(
            context ->
                mutate(context, playerId, id, arenas.clearBoundary(id, expected), "boundary-clear"))
        .build();
  }

  private Gui teamConfirmation(
      UUID playerId, ArenaDefinition arena, long expected, int count, int perTeam) {
    Map<String, Object> values = new LinkedHashMap<>(placeholders(arena, arenas.validate(arena)));
    values.put("new_team_count", count);
    values.put("new_players_per_team", perTeam);
    values.put("new_capacity", count * perTeam);
    return ConfirmationGui.builder()
        .id("arena.teams.confirm." + arena.id())
        .title(message("arena.teams.confirm-title", values))
        .informationKey("arena.teams.confirm-assisted")
        .informationPlaceholders(values)
        .confirmItemKey("gui.confirm")
        .cancelItemKey("gui.cancel")
        .permission(AdministrativeCommandPolicy.ARENA_EDIT)
        .onCancel(context -> context.replace(teams(playerId, arena.id().value(), expected)))
        .onConfirm(
            context ->
                mutate(
                    context,
                    playerId,
                    arena.id().value(),
                    arenas.setTeams(arena.id().value(), count, perTeam, expected),
                    "teams"))
        .build();
  }

  private void requestArenaId(UUID playerId) {
    Player player = Bukkit.getPlayer(playerId);
    if (player == null) return;
    if (!input.begin(
        playerId,
        request(
            playerId,
            "arena.input.arena-id",
            32,
            value -> {
              try {
                fr.heneria.bedwars.core.arena.ArenaId.parse(value);
                return arenas.find(value).isPresent() ? Optional.of("exists") : Optional.empty();
              } catch (RuntimeException exception) {
                return Optional.of("invalid-id");
              }
            },
            value -> {
              ArenaOperationResult result = arenas.create(value);
              if (result.successful()) {
                logger.info("[Arena] " + player.getName() + " created '" + value + "'.");
                ArenaDefinition created = result.arena().orElseThrow();
                gui.open(player, mapTemplates(playerId, created.id().value(), created.revision()));
              } else {
                mutationFailure(playerId, result);
                gui.open(player, list(playerId));
              }
            },
            reason -> gui.open(player, list(playerId))))) {
      send(playerId, "arena.input.already-active", Map.of());
      return;
    }
    gui.close(player);
    send(playerId, "arena.input.arena-id", Map.of());
  }

  private void requestMapForArena(UUID playerId, String arenaId, long expected) {
    Player player = Bukkit.getPlayer(playerId);
    if (player == null) return;
    if (!input.begin(
        playerId,
        request(
            playerId,
            "arena.input.map-id",
            32,
            value -> {
              try {
                fr.heneria.bedwars.core.map.MapId.parse(value);
                return maps.find(value).isPresent() ? Optional.of("exists") : Optional.empty();
              } catch (RuntimeException exception) {
                return Optional.of("invalid-id");
              }
            },
            value -> {
              var created = maps.create(value, MapType.BEDWARS, player.getName());
              if (!created.successful()) {
                send(
                    playerId,
                    "map.command.failed",
                    Map.of("code", created.code(), "detail", created.detail()));
                gui.open(player, mapTemplates(playerId, arenaId, expected));
                return;
              }
              var template = created.template().orElseThrow();
              ArenaOperationResult linked =
                  arenas.setMapTemplate(
                      arenaId, template.id().value(), template.worldName(), expected);
              if (linked.successful()) {
                maps.synchronizeLinks(arenas.list());
                send(playerId, "arena.map.created-and-linked", Map.of("map_id", value));
                gui.open(player, editor(playerId, arenaId));
              } else {
                mutationFailure(playerId, linked);
                gui.open(player, mapTemplates(playerId, arenaId, expected));
              }
            },
            reason -> gui.open(player, mapTemplates(playerId, arenaId, expected))))) {
      send(playerId, "arena.input.already-active", Map.of());
      return;
    }
    gui.close(player);
    send(playerId, "arena.input.map-id", Map.of("arena_id", arenaId));
  }

  private void requestDisplayName(UUID playerId, String id, long expected) {
    Player player = Bukkit.getPlayer(playerId);
    if (player == null) return;
    if (!input.begin(
        playerId,
        request(
            playerId,
            "arena.input.display-name",
            64,
            ArenaEditorMenuFactory::validateDisplayName,
            value -> {
              ArenaOperationResult result = arenas.setDisplayName(id, value, expected);
              if (result.successful())
                logger.info(
                    "[Arena] " + player.getName() + " changed display name of '" + id + "'.");
              mutationFeedback(playerId, result);
              gui.open(player, editor(playerId, id));
            },
            reason -> gui.open(player, editor(playerId, id))))) {
      send(playerId, "arena.input.already-active", Map.of());
      return;
    }
    gui.close(player);
    send(playerId, "arena.input.display-name", Map.of("arena_id", id));
  }

  private void requestPlayerNumber(
      UUID playerId, ArenaDefinition arena, long expected, boolean minimum) {
    requestNumber(
        playerId,
        arena,
        minimum ? "minimum_players" : "maximum_players",
        value -> {
          int min = minimum ? value : arena.minimumPlayers();
          int max = minimum ? arena.maximumPlayers() : value;
          return arenas.setPlayers(arena.id().value(), min, max, expected);
        },
        () -> players(playerId, arena.id().value(), expected));
  }

  private void requestTeamNumber(
      UUID playerId, ArenaDefinition arena, long expected, boolean count) {
    requestNumber(
        playerId,
        arena,
        count ? "team_count" : "players_per_team",
        value -> null,
        () -> teams(playerId, arena.id().value(), expected),
        value -> {
          int newCount = count ? value : arena.teamCount();
          int newPerTeam = count ? arena.playersPerTeam() : value;
          gui.open(
              player(playerId), teamConfirmation(playerId, arena, expected, newCount, newPerTeam));
        });
  }

  private void requestNumber(
      UUID playerId,
      ArenaDefinition arena,
      String field,
      IntFunction<ArenaOperationResult> operation,
      java.util.function.Supplier<Gui> reopen) {
    requestNumber(
        playerId,
        arena,
        field,
        operation,
        reopen,
        value -> {
          ArenaOperationResult result = operation.apply(value);
          mutationFeedback(playerId, result);
          gui.open(player(playerId), reopen.get());
        });
  }

  private void requestNumber(
      UUID playerId,
      ArenaDefinition arena,
      String field,
      IntFunction<ArenaOperationResult> operation,
      java.util.function.Supplier<Gui> reopen,
      java.util.function.IntConsumer accepted) {
    Player player = player(playerId);
    if (!input.begin(
        playerId,
        request(
            playerId,
            "arena.input.number",
            6,
            value -> positiveInteger(value).isPresent() ? Optional.empty() : Optional.of("number"),
            value -> accepted.accept(Integer.parseInt(value)),
            reason -> gui.open(player, reopen.get())))) {
      send(playerId, "arena.input.already-active", Map.of());
      return;
    }
    gui.close(player);
    send(playerId, "arena.input.number", Map.of("field", field, "arena_id", arena.id().value()));
  }

  private TextInputRequest request(
      UUID playerId,
      String promptKey,
      int maximumLength,
      java.util.function.Function<String, Optional<String>> validator,
      java.util.function.Consumer<String> success,
      java.util.function.Consumer<TextInputCancelReason> cancel) {
    return new TextInputRequest(
        promptKey,
        configurations.snapshot().menus().textInput().timeout(),
        maximumLength,
        configurations.snapshot().menus().textInput().cancelKeywords(),
        validator,
        success,
        problem -> send(playerId, "arena.input.invalid", Map.of("problem", problem)),
        reason -> {
          if (reason == TextInputCancelReason.TIMEOUT)
            send(playerId, "arena.input.timeout", Map.of());
          else if (reason == TextInputCancelReason.PLAYER)
            send(playerId, "arena.input.cancel", Map.of());
          if (reason == TextInputCancelReason.TIMEOUT || reason == TextInputCancelReason.PLAYER)
            cancel.accept(reason);
        });
  }

  private void changePlayers(
      GuiClickContext context,
      UUID playerId,
      ArenaDefinition arena,
      long expected,
      boolean minimum,
      int delta) {
    int min = minimum ? Math.max(1, arena.minimumPlayers() + delta) : arena.minimumPlayers();
    int max = minimum ? arena.maximumPlayers() : Math.max(1, arena.maximumPlayers() + delta);
    mutate(
        context,
        playerId,
        arena.id().value(),
        arenas.setPlayers(arena.id().value(), min, max, expected),
        "players");
  }

  private void proposeTeams(
      GuiClickContext context,
      UUID playerId,
      ArenaDefinition arena,
      long expected,
      boolean count,
      int delta) {
    int newCount = count ? Math.max(2, arena.teamCount() + delta) : arena.teamCount();
    int newPerTeam = count ? arena.playersPerTeam() : Math.max(1, arena.playersPerTeam() + delta);
    context.open(teamConfirmation(playerId, arena, expected, newCount, newPerTeam));
  }

  private void setTeamSpawn(
      GuiClickContext context,
      UUID playerId,
      ArenaDefinition arena,
      ArenaTeamDefinition team,
      long expected) {
    Player player = player(playerId);
    if (!correctArenaWorld(playerId, player, arena)) return;
    mutateTeam(
        context,
        playerId,
        arena.id().value(),
        team.id(),
        arenas.setTeamSpawn(
            arena.id().value(),
            team.id(),
            BukkitArenaLocations.from(player.getLocation()),
            expected),
        "team-spawn");
  }

  private void selectTeamBed(
      GuiClickContext context,
      UUID playerId,
      ArenaDefinition arena,
      ArenaTeamDefinition team,
      long expected) {
    Player player = player(playerId);
    BukkitArenaBeds.Selection selection = BukkitArenaBeds.select(player, arena);
    if (!selection.successful()) {
      String key =
          switch (selection.code()) {
            case WRONG_WORLD -> "arena.team.wrong-world";
            case NOT_A_BED -> "arena.team.bed.invalid-block";
            case INCOMPLETE_BED -> "arena.team.bed.incomplete";
            case SUCCESS ->
                throw new IllegalArgumentException("Successful selection is not a failure");
          };
      send(
          playerId,
          key,
          Map.of(
              "world", arena.worldName().orElse("-"),
              "current_world", player.getWorld().getName()));
      return;
    }
    ArenaOperationResult operation =
        arenas.setTeamBed(arena.id().value(), team.id(), selection.bed().orElseThrow(), expected);
    if (operation.code() == ArenaOperationCode.INVALID_ARGUMENT
        && operation.detail().startsWith("Bed already belongs to team ")) {
      send(
          playerId,
          "arena.team.bed.duplicate",
          Map.of("team", operation.detail().substring("Bed already belongs to team ".length())));
      return;
    }
    mutateTeam(context, playerId, arena.id().value(), team.id(), operation, "team-bed");
  }

  private boolean correctArenaWorld(UUID playerId, Player player, ArenaDefinition arena) {
    String expected = arena.worldName().orElse("-");
    if (player.getWorld().getName().equals(expected)) return true;
    send(
        playerId,
        "arena.team.wrong-world",
        Map.of("world", expected, "current_world", player.getWorld().getName()));
    return false;
  }

  private void mutateTeam(
      GuiClickContext context,
      UUID playerId,
      String id,
      TeamId teamId,
      ArenaOperationResult result,
      String operation) {
    if (result.successful()) {
      logger.info(
          "[Arena] " + playerName(playerId) + " changed " + operation + " of '" + id + "'.");
      send(playerId, "arena.gui.editor.autosave", Map.of("arena_id", id));
      ArenaDefinition updated = result.arena().orElseThrow();
      context.replace(teamEditor(playerId, id, teamId, updated.revision()));
    } else {
      mutationFailure(playerId, result);
      if (result.code() == ArenaOperationCode.NOT_FOUND) openDashboard(playerId);
      else if (result.code() == ArenaOperationCode.CONFLICT) context.replace(editor(playerId, id));
    }
  }

  private void mutate(
      GuiClickContext context,
      UUID playerId,
      String id,
      ArenaOperationResult result,
      String operation) {
    if (result.successful()) {
      logger.info(
          "[Arena] " + playerName(playerId) + " changed " + operation + " of '" + id + "'.");
      send(playerId, "arena.gui.editor.autosave", Map.of("arena_id", id));
      context.replace(editor(playerId, id));
    } else {
      mutationFailure(playerId, result);
      if (result.code() == ArenaOperationCode.NOT_FOUND) openDashboard(playerId);
    }
  }

  private void mutationFeedback(UUID playerId, ArenaOperationResult result) {
    if (result.successful()) send(playerId, "arena.gui.editor.autosave", Map.of());
    else mutationFailure(playerId, result);
  }

  private void mutationFailure(UUID playerId, ArenaOperationResult result) {
    if (result.code() == ArenaOperationCode.CONFLICT)
      send(playerId, "arena.gui.editor.conflict", Map.of());
    else if (result.code() == ArenaOperationCode.VALIDATION_FAILED)
      send(
          playerId,
          "arena.validation.errors",
          Map.of("validation_errors", result.problems().size()));
    else send(playerId, "arena.gui.editor.failed", Map.of("detail", result.detail()));
  }

  private void teleportWorld(GuiClickContext context, UUID playerId, World world) {
    Player player = player(playerId);
    if (!player.hasPermission(AdministrativeCommandPolicy.ARENA_TELEPORT)) {
      send(playerId, "general.no-permission", Map.of());
      return;
    }
    player.teleport(world.getSpawnLocation());
    send(playerId, "arena.world.teleport-success", Map.of("world", world.getName()));
  }

  private void teleport(GuiClickContext context, UUID playerId, ArenaLocation value) {
    Player player = player(playerId);
    if (!player.hasPermission(AdministrativeCommandPolicy.ARENA_TELEPORT)) {
      send(playerId, "general.no-permission", Map.of());
      return;
    }
    World world = Bukkit.getWorld(value.world());
    if (world == null) {
      send(playerId, "arena.world.not-found", Map.of("world", value.world()));
      return;
    }
    player.teleport(
        new Location(
            world,
            value.position().x(),
            value.position().y(),
            value.position().z(),
            value.yaw(),
            value.pitch()));
    send(playerId, "arena.world.teleport-success", Map.of("world", world.getName()));
  }

  private void teleportTeamSpawn(
      GuiClickContext context, UUID playerId, ArenaTeamDefinition team, ArenaLocation value) {
    teleportTeamPosition(context, playerId, team, value, false);
  }

  private void teleportBed(
      GuiClickContext context, UUID playerId, ArenaTeamDefinition team, ArenaLocation value) {
    teleportTeamPosition(context, playerId, team, value, true);
  }

  private void teleportTeamPosition(
      GuiClickContext context,
      UUID playerId,
      ArenaTeamDefinition team,
      ArenaLocation value,
      boolean bed) {
    Player player = player(playerId);
    if (!player.hasPermission(AdministrativeCommandPolicy.ARENA_TELEPORT)) {
      send(playerId, "general.no-permission", Map.of());
      return;
    }
    World world = Bukkit.getWorld(value.world());
    if (world == null) {
      send(playerId, "arena.world.not-found", Map.of("world", value.world()));
      return;
    }
    player.teleport(
        new Location(
            world,
            value.position().x() + (bed ? 0.5 : 0.0),
            value.position().y() + (bed ? 1.0 : 0.0),
            value.position().z() + (bed ? 0.5 : 0.0),
            value.yaw(),
            value.pitch()));
    send(
        playerId,
        bed ? "arena.team.bed.teleported" : "arena.team.spawn.teleported",
        Map.of("team", team.displayName()));
  }

  private void teleportBoundary(UUID playerId, ArenaDefinition arena, ArenaVector point) {
    Player player = player(playerId);
    if (!player.hasPermission(AdministrativeCommandPolicy.ARENA_TELEPORT)) {
      send(playerId, "general.no-permission", Map.of());
      return;
    }
    World world = arena.worldName().map(Bukkit::getWorld).orElse(null);
    if (world == null) {
      send(playerId, "arena.world.not-found", Map.of("world", arena.worldName().orElse("-")));
      return;
    }
    player.teleport(new Location(world, point.x(), point.y(), point.z()));
    send(playerId, "arena.world.teleport-success", Map.of("world", world.getName()));
  }

  private Gui missing(UUID playerId) {
    return Gui.builder()
        .id("arena.missing")
        .title(message("arena.gui.editor.missing-title-v3", Map.of()))
        .rows(3)
        .fillEmptySlots(true)
        .button(13, GuiButton.builder().itemKey("arena.missing.information-v3").build())
        .button(22, dashboardButton(playerId))
        .build();
  }

  private GuiButton dashboardButton(UUID playerId) {
    return GuiButton.builder()
        .itemKey("admin.main.back-to-dashboard-v3")
        .onLeftClick(context -> openDashboard(playerId))
        .build();
  }

  private GuiButton editorBackButton(UUID playerId, String id) {
    return GuiButton.builder()
        .itemKey("gui.back")
        .onLeftClick(context -> context.replace(editor(playerId, id)))
        .build();
  }

  private void openDashboard(UUID playerId) {
    Player player = Bukkit.getPlayer(playerId);
    if (player != null) gui.open(player, setup(playerId));
  }

  private GuiButton action(
      String key,
      String permission,
      Map<String, ?> placeholders,
      fr.heneria.bedwars.core.gui.GuiAction action) {
    return GuiButton.builder()
        .itemKey(key)
        .itemPlaceholders(context -> placeholders)
        .permission(permission)
        .onLeftClick(action)
        .build();
  }

  private ArenaEditorSettings settings() {
    return configurations.snapshot().menus().arenaEditor();
  }

  private Map<String, Object> configurationPlaceholders() {
    var snapshot = configurations.snapshot();
    return Map.of(
        "language", snapshot.plugin().locale(),
        "debug", snapshot.plugin().debug(),
        "storage", snapshot.storage().type(),
        "items", snapshot.items().size(),
        "loaded_files", snapshot.documents().size() + snapshot.languages().size(),
        "arena_count", arenas.list().size(),
        "enabled_count", arenas.list().stream().filter(ArenaDefinition::enabled).count(),
        "map_count", maps.list().size(),
        "loaded_map_count", maps.list().stream().filter(map -> map.loaded()).count());
  }

  private static String entryKey(ArenaDefinition arena) {
    return switch (arena.status()) {
      case ENABLED -> "arena.list.entry-enabled-v2";
      case INVALID, ERROR -> "arena.list.entry-invalid-v2";
      case DRAFT -> "arena.list.entry-draft-v2";
      default -> "arena.list.entry-disabled-v2";
    };
  }

  private static String displayFilter(String filter) {
    return switch (filter) {
      case "ENABLED" -> "Prêtes";
      case "DISABLED" -> "Désactivées";
      case "INVALID" -> "À terminer";
      case "DRAFT" -> "Brouillons";
      default -> "Toutes";
    };
  }

  private static String displaySort(String sort) {
    return switch (sort) {
      case "NAME" -> "Nom";
      case "STATUS" -> "État";
      case "UPDATED" -> "Modifiées récemment";
      default -> "Identifiant";
    };
  }

  private static Map<String, Object> placeholders(
      ArenaDefinition arena, ArenaValidationResult validation) {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("arena_id", arena.id().value());
    values.put("arena_name", arena.displayName());
    values.put("arena_status", arena.status().name());
    values.put(
        "arena_status_label",
        switch (arena.status()) {
          case ENABLED -> "Prête";
          case DISABLED -> "Désactivée";
          case INVALID, ERROR -> "À terminer";
          case DRAFT -> "Brouillon";
          case READY -> "Prête à activer";
        });
    values.put("arena_enabled", arena.enabled());
    values.put("arena_revision", arena.revision());
    values.put("world", arena.worldName().orElse("-"));
    values.put("map_template", arena.template().orElse("-"));
    values.put("minimum_players", arena.minimumPlayers());
    values.put("maximum_players", arena.maximumPlayers());
    values.put("min_players", arena.minimumPlayers());
    values.put("max_players", arena.maximumPlayers());
    values.put("team_count", arena.teamCount());
    values.put("teams", arena.teamCount());
    values.put("players_per_team", arena.playersPerTeam());
    values.put("capacity", arena.teamCount() * arena.playersPerTeam());
    values.put(
        "team_spawns_configured",
        arena.teams().stream().filter(team -> team.spawn().isPresent()).count());
    values.put(
        "team_beds_configured",
        arena.teams().stream().filter(team -> team.bedDefinition().isPresent()).count());
    values.put("validation_errors", errors(validation));
    values.put("validation_warnings", validation.warnings());
    boolean mapConfigured = arena.template().isPresent();
    boolean waitingConfigured = arena.waitingLocation().isPresent();
    boolean spectatorConfigured = arena.spectatorLocation().isPresent();
    boolean teamsConfigured =
        arena.teamCount() >= 2
            && arena.playersPerTeam() >= 1
            && arena.maximumPlayers() == arena.teamCount() * arena.playersPerTeam();
    boolean playersConfigured =
        arena.minimumPlayers() >= 1 && arena.maximumPlayers() >= arena.minimumPlayers();
    int completed =
        (mapConfigured ? 1 : 0)
            + (waitingConfigured ? 1 : 0)
            + (teamsConfigured ? 1 : 0)
            + (playersConfigured ? 1 : 0);
    values.put("map_configured", mapConfigured);
    values.put("waiting_configured", waitingConfigured);
    values.put("spectator_configured", spectatorConfigured);
    values.put("teams_configured", teamsConfigured);
    values.put("players_configured", playersConfigured);
    values.put("map_step", stepLabel(mapConfigured));
    values.put("waiting_step", stepLabel(waitingConfigured));
    values.put("spectator_step", stepLabel(spectatorConfigured));
    values.put("teams_step", stepLabel(teamsConfigured));
    values.put("players_step", stepLabel(playersConfigured));
    values.put("setup_steps_complete", completed);
    values.put("setup_steps_total", 4);
    values.put("setup_percent", completed * 25);
    values.put(
        "next_step",
        !mapConfigured
            ? "Choisir la carte BedWars"
            : !waitingConfigured
                ? "Placer le point d'attente"
                : !teamsConfigured
                    ? "Configurer les équipes"
                    : !playersConfigured
                        ? "Vérifier les joueurs"
                        : !spectatorConfigured
                            ? "Ajouter le point spectateur (conseillé)"
                            : validation.valid()
                                ? "Activer l'arène"
                                : "Consulter les étapes restantes");
    values.put("created_at", DATE.format(arena.metadata().createdAt()));
    values.put("updated_at", DATE.format(arena.metadata().updatedAt()));
    values.put("author", arena.metadata().attributes().getOrDefault("author", "unknown"));
    values.put("description", arena.metadata().attributes().getOrDefault("description", "-"));
    values.put("arena_file", "arenas/" + arena.id().value() + ".yml");
    return Map.copyOf(values);
  }

  private Map<String, Object> teamPlaceholders(
      ArenaDefinition arena, ArenaValidationResult validation, ArenaTeamDefinition team) {
    Map<String, Object> values = new LinkedHashMap<>(placeholders(arena, validation));
    values.put("team_id", team.id().value());
    values.put("team_name", team.displayName());
    values.put("team_color", team.color().name());
    values.put("team_color_display", teamColorDisplay(team.color()));
    values.put("team_capacity", team.capacity());
    values.put(
        "team_spawn_status",
        message(
            team.spawn().isPresent()
                ? "arena.teams.status.configured"
                : "arena.teams.status.missing",
            Map.of()));
    values.put(
        "team_bed_status",
        message(
            team.bedDefinition().isPresent()
                ? "arena.teams.status.configured"
                : "arena.teams.status.missing",
            Map.of()));
    values.put(
        "team_ready",
        message(
            team.spawn().isPresent() && team.bedDefinition().isPresent()
                ? "arena.teams.status.configured"
                : "arena.teams.status.missing",
            Map.of()));
    putLocation(values, "spawn", team.spawn());
    putLocation(values, "bed", team.bedLocation());
    return Map.copyOf(values);
  }

  private static void putLocation(
      Map<String, Object> values, String prefix, Optional<ArenaLocation> location) {
    values.put(prefix + "_world", location.map(ArenaLocation::world).orElse("-"));
    values.put(prefix + "_x", location.map(value -> value.position().x()).orElse(0.0));
    values.put(prefix + "_y", location.map(value -> value.position().y()).orElse(0.0));
    values.put(prefix + "_z", location.map(value -> value.position().z()).orElse(0.0));
  }

  private static String teamEntryKey(TeamColor color) {
    return "arena.teams.entry-" + color.name().toLowerCase(Locale.ROOT) + "-v6";
  }

  private static String teamColorDisplay(TeamColor color) {
    String code =
        switch (color) {
          case RED -> "§c";
          case BLUE -> "§9";
          case GREEN -> "§2";
          case YELLOW -> "§e";
          case AQUA -> "§b";
          case WHITE -> "§f";
          case PINK -> "§d";
          case GRAY -> "§7";
          case LIME -> "§a";
          case ORANGE -> "§6";
          case PURPLE -> "§5";
          case BLACK -> "§8";
        };
    return code + color.name() + "§f";
  }

  private static Map<String, Object> positionPlaceholders(
      ArenaDefinition arena, ArenaValidationResult validation, Optional<ArenaLocation> location) {
    Map<String, Object> values = new LinkedHashMap<>(placeholders(arena, validation));
    values.put("position_configured", location.isPresent());
    values.put("position_status", stepLabel(location.isPresent()));
    values.put("position_world", location.map(ArenaLocation::world).orElse("-"));
    values.put("x", location.map(value -> value.position().x()).orElse(0.0));
    values.put("y", location.map(value -> value.position().y()).orElse(0.0));
    values.put("z", location.map(value -> value.position().z()).orElse(0.0));
    values.put("yaw", location.map(ArenaLocation::yaw).orElse(0f));
    values.put("pitch", location.map(ArenaLocation::pitch).orElse(0f));
    return Map.copyOf(values);
  }

  private static Map<String, Object> boundaryPlaceholders(
      Map<String, Object> base, ArenaBoundary boundary) {
    Map<String, Object> values = new LinkedHashMap<>(base);
    values.put("boundary_enabled", boundary.enabled());
    values.put("minimum_configured", boundary.minimum().isPresent());
    values.put("maximum_configured", boundary.maximum().isPresent());
    values.put("boundary_valid", boundary.ordered());
    values.put(
        "boundary_status",
        !boundary.enabled()
            ? "Option désactivée"
            : boundary.minimum().isPresent() && boundary.maximum().isPresent() && boundary.ordered()
                ? "✓ Limites prêtes"
                : "À terminer");
    return Map.copyOf(values);
  }

  private static String stepLabel(boolean complete) {
    return complete ? "✓ Terminé" : "À faire";
  }

  private String message(String key, Map<String, ?> values) {
    PlaceholderContext.Builder context = PlaceholderContext.builder();
    values.forEach(context::put);
    return configurations
        .language()
        .message(key, configurations.snapshot().plugin().locale(), context.build());
  }

  private void send(UUID playerId, String key, Map<String, ?> values) {
    Player player = Bukkit.getPlayer(playerId);
    if (player != null) player.sendMessage(message(key, values));
  }

  private static Optional<String> validateDisplayName(String value) {
    if (value.isBlank() || value.length() > 64) return Optional.of("length");
    java.util.regex.Matcher matcher =
        java.util.regex.Pattern.compile("<(/?)([^>]+)>").matcher(value);
    while (matcher.find()) {
      String tag = matcher.group(2).toLowerCase(Locale.ROOT);
      if (tag.matches("#[0-9a-f]{6}")) continue;
      if (!ALLOWED_TAGS.contains(tag)) return Optional.of("unsupported-tag");
    }
    return Optional.empty();
  }

  private static Optional<Integer> positiveInteger(String value) {
    try {
      int parsed = Integer.parseInt(value);
      return parsed > 0 && parsed <= 10_000 ? Optional.of(parsed) : Optional.empty();
    } catch (NumberFormatException exception) {
      return Optional.empty();
    }
  }

  private static long errors(ArenaValidationResult validation) {
    return validation.problems().stream()
        .filter(
            problem ->
                problem.severity() == ProblemSeverity.ERROR
                    || problem.severity() == ProblemSeverity.CRITICAL)
        .count();
  }

  private String localizedProblem(ArenaProblem problem) {
    return message("arena.validation.problems." + problemKey(problem) + ".message", Map.of());
  }

  private String solution(ArenaProblem problem) {
    return message("arena.validation.problems." + problemKey(problem) + ".solution", Map.of());
  }

  private static String problemKey(ArenaProblem problem) {
    return problem.code().toLowerCase(Locale.ROOT).replace('_', '-');
  }

  private static String validationItemKey(ProblemSeverity severity) {
    return switch (severity) {
      case INFO -> "arena.validation.step-info";
      case WARNING -> "arena.validation.step-warning";
      case ERROR -> "arena.validation.step-error";
      case CRITICAL -> "arena.validation.step-critical";
    };
  }

  private static boolean currentWorld(UUID playerId, World world) {
    Player player = Bukkit.getPlayer(playerId);
    return player != null && player.getWorld().equals(world);
  }

  private static ArenaVector vector(Location location) {
    return new ArenaVector(location.getX(), location.getY(), location.getZ());
  }

  private static void withPlayer(UUID id, java.util.function.Consumer<Player> action) {
    Player player = Bukkit.getPlayer(id);
    if (player != null) action.accept(player);
  }

  private static Player player(UUID id) {
    Player player = Bukkit.getPlayer(id);
    if (player == null) throw new IllegalStateException("Player is no longer online");
    return player;
  }

  private static String playerName(UUID id) {
    Player player = Bukkit.getPlayer(id);
    return player == null ? id.toString() : player.getName();
  }
}
