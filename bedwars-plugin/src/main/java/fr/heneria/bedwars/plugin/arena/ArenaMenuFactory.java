package fr.heneria.bedwars.plugin.arena;

import fr.heneria.bedwars.core.arena.ArenaDefinition;
import fr.heneria.bedwars.core.arena.ArenaOperationResult;
import fr.heneria.bedwars.core.arena.ArenaService;
import fr.heneria.bedwars.core.command.AdministrativeCommandPolicy;
import fr.heneria.bedwars.core.config.PlaceholderContext;
import fr.heneria.bedwars.core.config.TranslationKey;
import fr.heneria.bedwars.core.gui.ConfirmationGui;
import fr.heneria.bedwars.core.gui.Gui;
import fr.heneria.bedwars.core.gui.GuiButton;
import fr.heneria.bedwars.core.gui.GuiSlots;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import fr.heneria.bedwars.plugin.gui.StandardGuiButtons;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Administrative list, detail and deletion confirmation menus. */
public final class ArenaMenuFactory {
  private final ArenaService arenas;
  private final ConfigurationService configurations;
  private final StandardGuiButtons standard = new StandardGuiButtons();

  public ArenaMenuFactory(ArenaService arenas, ConfigurationService configurations) {
    this.arenas = arenas;
    this.configurations = configurations;
  }

  public Gui list(int page) {
    List<ArenaDefinition> definitions = arenas.list();
    List<Integer> slots = GuiSlots.rectangle(1, 1, 3, 7);
    int maxPage = Math.max(1, (definitions.size() + slots.size() - 1) / slots.size());
    int safePage = Math.max(0, Math.min(page, maxPage - 1));
    Gui.Builder builder =
        Gui.builder()
            .id("arena.list")
            .title(
                text(
                    TranslationKey.ARENA_LIST,
                    PlaceholderContext.builder()
                        .put("count", definitions.size())
                        .put("arenas", "")
                        .build()))
            .rows(6)
            .fillEmptySlots(true)
            .data("max_page", maxPage);
    for (int index = 0; index < slots.size(); index++) {
      int absolute = safePage * slots.size() + index;
      if (absolute >= definitions.size()) break;
      ArenaDefinition arena = definitions.get(absolute);
      builder.button(
          slots.get(index),
          GuiButton.builder()
              .itemKey("arena.entry")
              .itemPlaceholders(context -> placeholders(arena))
              .onLeftClick(context -> context.open(info(arena.id().value())))
              .build());
    }
    if (safePage > 0)
      builder.button(45, standard.previous(context -> context.replace(list(safePage - 1))));
    if (safePage + 1 < maxPage)
      builder.button(53, standard.next(context -> context.replace(list(safePage + 1))));
    return builder
        .button(49, standard.close())
        .button(
            50,
            GuiButton.builder()
                .itemKey("gui.page-indicator")
                .itemPlaceholders(context -> Map.of("page", safePage + 1, "max_page", maxPage))
                .build())
        .build();
  }

  public Gui info(String id) {
    ArenaDefinition arena = arenas.find(id).orElseThrow();
    return Gui.builder()
        .id("arena.info." + id)
        .title(text(TranslationKey.ARENA_INFO, placeholdersContext(arena)))
        .rows(5)
        .fillEmptySlots(true)
        .button(
            13,
            GuiButton.builder()
                .itemKey("arena.info")
                .itemPlaceholders(context -> placeholders(arena))
                .build())
        .button(
            20,
            action(
                "arena.set-world",
                AdministrativeCommandPolicy.ARENA_EDIT,
                context ->
                    withPlayer(
                        context.session().playerId(),
                        player ->
                            notify(context, arenas.setWorld(id, player.getWorld().getName())))))
        .button(
            21,
            action(
                "arena.set-waiting",
                AdministrativeCommandPolicy.ARENA_EDIT,
                context ->
                    withPlayer(
                        context.session().playerId(),
                        player ->
                            notify(
                                context,
                                arenas.setWaiting(
                                    id, BukkitArenaLocations.from(player.getLocation()))))))
        .button(
            22,
            action(
                "arena.set-spectator",
                AdministrativeCommandPolicy.ARENA_EDIT,
                context ->
                    withPlayer(
                        context.session().playerId(),
                        player ->
                            notify(
                                context,
                                arenas.setSpectator(
                                    id, BukkitArenaLocations.from(player.getLocation()))))))
        .button(
            23,
            action(
                "arena.validate",
                AdministrativeCommandPolicy.ARENA_EDIT,
                context ->
                    context.message(
                        arenas.validate(arenas.find(id).orElseThrow()).valid()
                            ? TranslationKey.ARENA_VALID
                            : TranslationKey.ARENA_INVALID,
                        placeholdersContext(arena))))
        .button(
            24,
            action(
                arena.enabled() ? "arena.disable" : "arena.enable",
                arena.enabled()
                    ? AdministrativeCommandPolicy.ARENA_DISABLE
                    : AdministrativeCommandPolicy.ARENA_ENABLE,
                context ->
                    notify(context, arena.enabled() ? arenas.disable(id) : arenas.enable(id))))
        .button(
            31,
            action(
                "arena.delete",
                AdministrativeCommandPolicy.ARENA_DELETE,
                context -> context.open(confirmDelete(id))))
        .button(40, standard.back())
        .build();
  }

  public Gui confirmDelete(String id) {
    return ConfirmationGui.builder()
        .id("arena.delete." + id)
        .title("Delete arena " + id + "?")
        .informationKey("arena.delete")
        .confirmItemKey("gui.confirm")
        .cancelItemKey("gui.cancel")
        .onConfirm(
            context -> {
              ArenaOperationResult result = arenas.delete(id);
              notify(context, result, TranslationKey.ARENA_DELETED);
              if (result.successful()) context.replace(list(0));
            })
        .onCancel(context -> context.back())
        .build();
  }

  private static GuiButton action(
      String key, String permission, fr.heneria.bedwars.core.gui.GuiAction action) {
    return GuiButton.builder()
        .itemKey(key)
        .onLeftClick(
            context -> {
              Player player = Bukkit.getPlayer(context.session().playerId());
              if (player == null) return;
              if (player.hasPermission(permission)) action.execute(context);
              else context.message(TranslationKey.GUI_NO_PERMISSION);
            })
        .build();
  }

  private static void withPlayer(java.util.UUID id, java.util.function.Consumer<Player> action) {
    Player player = Bukkit.getPlayer(id);
    if (player != null) action.accept(player);
  }

  private static void notify(
      fr.heneria.bedwars.core.gui.GuiClickContext context, ArenaOperationResult result) {
    notify(context, result, TranslationKey.ARENA_UPDATED);
  }

  private static void notify(
      fr.heneria.bedwars.core.gui.GuiClickContext context,
      ArenaOperationResult result,
      TranslationKey successKey) {
    ArenaDefinition arena = result.arena().orElse(null);
    PlaceholderContext placeholders =
        PlaceholderContext.builder()
            .put("arena", arena == null ? result.detail() : arena.id().value())
            .build();
    context.message(
        result.successful() ? successKey : TranslationKey.ARENA_STORAGE_ERROR, placeholders);
    context.refresh();
  }

  private static Map<String, Object> placeholders(ArenaDefinition arena) {
    return Map.of(
        "arena",
        arena.id().value(),
        "arena_id",
        arena.id().value(),
        "arena_name",
        arena.displayName(),
        "arena_status",
        arena.status().name(),
        "world",
        arena.worldName().orElse("-"),
        "min_players",
        arena.minimumPlayers(),
        "max_players",
        arena.maximumPlayers(),
        "teams",
        arena.teamCount(),
        "players_per_team",
        arena.playersPerTeam());
  }

  private static PlaceholderContext placeholdersContext(ArenaDefinition arena) {
    PlaceholderContext.Builder builder = PlaceholderContext.builder();
    placeholders(arena).forEach(builder::put);
    return builder.build();
  }

  private String text(TranslationKey key, PlaceholderContext context) {
    return configurations.language().message(key, context);
  }
}
