package fr.heneria.zombie.plugin.editor;

import fr.heneria.zombie.core.editor.Clipboard;
import fr.heneria.zombie.core.editor.EditorTool;
import fr.heneria.zombie.core.editor.MapDefinition;
import fr.heneria.zombie.core.editor.MapEditorService;
import fr.heneria.zombie.core.editor.MapObjectType;
import fr.heneria.zombie.core.editor.MapValidator;
import fr.heneria.zombie.plugin.gui.GuiActionRegistry;
import fr.heneria.zombie.plugin.gui.GuiConfigurationService;
import fr.heneria.zombie.plugin.gui.GuiConfirmation;
import fr.heneria.zombie.plugin.gui.GuiContext;
import fr.heneria.zombie.plugin.gui.GuiId;
import fr.heneria.zombie.plugin.gui.GuiInputRequest;
import fr.heneria.zombie.plugin.gui.GuiPagination;
import fr.heneria.zombie.plugin.gui.GuiRegistry;
import fr.heneria.zombie.plugin.gui.GuiService;
import fr.heneria.zombie.plugin.gui.GuiView;
import fr.heneria.zombie.plugin.gui.StandardGui;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Ticket 004 GUI adapter; business mutations remain in {@link MapEditorService}. */
public final class EditorGuiModule {
  public static final java.util.Set<String> ACTION_IDS =
      java.util.Set.of(
          "nav.editor",
          "editor.info",
          "editor.zones",
          "editor.doors",
          "editor.spawns",
          "editor.barricades",
          "editor.mystery-box",
          "editor.pack-a-punch",
          "editor.perks",
          "editor.traps",
          "editor.teleporters",
          "editor.power",
          "editor.objectives",
          "editor.quests",
          "editor.bosses",
          "editor.validation",
          "editor.save");

  private static final MiniMessage MINI = MiniMessage.miniMessage();
  private final GuiRegistry registry;
  private final GuiActionRegistry actions;
  private final GuiConfigurationService configurations;
  private final GuiService guis;
  private final MapEditorService editors;
  private final MapValidator validator;
  private final EditorItemService items;
  private final Executor mainThread;
  private final Clock clock;

  public EditorGuiModule(
      GuiRegistry registry,
      GuiActionRegistry actions,
      GuiConfigurationService configurations,
      GuiService guis,
      MapEditorService editors,
      MapValidator validator,
      EditorItemService items,
      Executor mainThread,
      Clock clock) {
    this.registry = registry;
    this.actions = actions;
    this.configurations = configurations;
    this.guis = guis;
    this.editors = editors;
    this.validator = validator;
    this.items = items;
    this.mainThread = mainThread;
    this.clock = clock;
  }

  public void register() {
    actions.register(
        "nav.editor",
        context -> {
          if (editors.session(context.player().getUniqueId()).isPresent()) {
            guis.openHome(context.player(), new GuiId("editor-main"));
          } else {
            context.player().sendMessage(MINI.deserialize("<yellow>Utilisez /zmap edit <map>."));
          }
        });
    actions.register("editor.info", context -> open(context.player(), "info"));
    actions.register("editor.zones", context -> open(context.player(), "zones"));
    actions.register("editor.doors", context -> open(context.player(), "doors"));
    actions.register("editor.spawns", context -> open(context.player(), "spawns"));
    actions.register("editor.barricades", context -> open(context.player(), "BARRICADE"));
    actions.register("editor.mystery-box", context -> open(context.player(), "MYSTERY_BOX"));
    actions.register("editor.pack-a-punch", context -> open(context.player(), "PACK_A_PUNCH"));
    actions.register("editor.perks", context -> open(context.player(), "PERK"));
    actions.register("editor.traps", context -> open(context.player(), "TRAP"));
    actions.register("editor.teleporters", context -> open(context.player(), "TELEPORTER"));
    actions.register("editor.power", context -> open(context.player(), "POWER"));
    actions.register("editor.objectives", context -> open(context.player(), "OBJECTIVE"));
    actions.register("editor.quests", context -> open(context.player(), "QUEST"));
    actions.register("editor.bosses", context -> open(context.player(), "BOSS"));
    actions.register("editor.validation", context -> showValidation(context.player()));
    actions.register("editor.save", context -> save(context.player()));
    registry.register(new StandardGui("editor-main", configurations, this::renderMain, () -> 0));
    registry.register(
        new StandardGui("editor-category", configurations, this::renderCategory, () -> 0));
  }

  private void renderMain(GuiView view, GuiContext ignored) {
    var session = editors.session(view.player().getUniqueId()).orElse(null);
    if (session == null) {
      view.information(
          22, Material.BARRIER, "<red>Aucune session", List.of("<gray>/zmap edit <map>"));
      return;
    }
    view.information(
        4,
        Material.FILLED_MAP,
        "<aqua>" + session.definition().displayName(),
        List.of(
            "<gray>ID : <white>" + session.mapId(),
            "<gray>Monde : <white>" + session.definition().world(),
            "<gray>Zones : <white>" + session.definition().zones().size(),
            "<gray>Portes : <white>" + session.definition().doors().size(),
            "<gray>Spawns : <white>" + session.definition().zombieSpawns().size(),
            "<gray>Objets : <white>" + session.definition().objects().size(),
            "<gray>Undo : <white>" + session.historySize()));
    for (String key :
        List.of(
            "info",
            "zones",
            "doors",
            "spawns",
            "barricades",
            "mystery-box",
            "pack-a-punch",
            "perks",
            "traps",
            "teleporters",
            "power",
            "objectives",
            "quests",
            "bosses",
            "validation",
            "save")) {
      view.configured(key);
    }
  }

  private void renderCategory(GuiView view, GuiContext context) {
    var editor = editors.session(view.player().getUniqueId()).orElse(null);
    if (editor == null) {
      return;
    }
    String category = context.value("category", String.class).orElse("info");
    MapDefinition map = editor.definition();
    if (category.equals("info")) {
      view.information(
          4,
          Material.BOOK,
          "<aqua>Informations générales",
          List.of(
              "<gray>Nom : <white>" + map.displayName(),
              "<gray>Auteur : <white>" + map.creator(),
              "<gray>Monde : <white>" + map.world(),
              "<gray>Spawn : <white>" + (map.playerSpawn().isPresent() ? "défini" : "manquant")));
      informationButton(view, 10, Material.NAME_TAG, "Nom", map.displayName(), "name");
      informationButton(
          view, 11, Material.WRITABLE_BOOK, "Description", map.description(), "description");
      informationButton(view, 12, Material.ITEM_FRAME, "Icône", map.icon(), "icon");
      informationButton(view, 13, Material.PAINTING, "Image", map.image(), "image");
      informationButton(
          view,
          14,
          Material.PLAYER_HEAD,
          "Nombre de joueurs",
          Integer.toString(map.maximumPlayers()),
          "maximum-players");
      informationButton(view, 15, Material.MUSIC_DISC_13, "Musique", map.music(), "music");
      informationButton(
          view, 16, Material.ZOMBIE_HEAD, "Difficulté", map.difficulty(), "difficulty");
      informationButton(view, 19, Material.COMMAND_BLOCK, "Mode", map.gameMode(), "game-mode");
      view.button(
          22,
          Material.COMPASS,
          "<green>Placer le spawn joueur",
          List.of("<gray>Cliquez puis utilisez l'outil sur un bloc."),
          "zombie.editor",
          click -> selectTool(click.player(), EditorTool.SET_PLAYER_SPAWN, null),
          null,
          null);
    } else {
      List<EntityEntry> entries = entries(map, category);
      var session = guis.session(view.player());
      var page = GuiPagination.page(entries, session.page(), view.menu().contentSlots().size());
      session.page(page.index());
      for (int index = 0; index < page.items().size(); index++) {
        EntityEntry entry = page.items().get(index);
        view.button(
            view.menu().contentSlots().get(index),
            entry.material(),
            "<aqua>" + entry.id(),
            List.of(
                "<gray>Type : <white>" + entry.kind(),
                "<yellow>Clic gauche : déplacer",
                "<red>Clic droit : supprimer",
                "<aqua>Maj + clic : dupliquer"),
            "zombie.editor",
            click -> move(click.player(), entry),
            click -> confirmDelete(click.player(), entry),
            click -> duplicate(click.player(), entry));
      }
      view.button(
          49,
          Material.LIME_CONCRETE,
          "<green>Ajouter",
          List.of("<gray>Sélectionne l'outil de placement."),
          "zombie.editor",
          click -> add(click.player(), category),
          null,
          null);
    }
    view.configured("back");
  }

  private void open(Player player, String category) {
    guis.open(player, new GuiId("editor-category"), GuiContext.of("category", category));
  }

  private void add(Player player, String category) {
    var session = editors.session(player.getUniqueId()).orElseThrow();
    if (category.equals("zones")) {
      session.tool(EditorTool.ADD_ZONE);
    } else if (category.equals("doors")) {
      session.tool(EditorTool.ADD_DOOR);
    } else if (category.equals("spawns")) {
      session.tool(EditorTool.ADD_ZOMBIE_SPAWN);
    } else {
      session.selectedObjectType(MapObjectType.valueOf(category));
      session.tool(EditorTool.ADD_OBJECT);
    }
    items.give(player, session);
    player.closeInventory();
    player.sendMessage(MINI.deserialize("<green>Outil sélectionné. Cliquez sur un bloc."));
  }

  private void move(Player player, EntityEntry entry) {
    var session = editors.session(player.getUniqueId()).orElseThrow();
    session.clipboard(new Clipboard(entry.kind(), Map.of("id", entry.id())));
    session.tool(EditorTool.MOVE);
    items.give(player, session);
    player.closeInventory();
    player.sendMessage(MINI.deserialize("<yellow>Cliquez sur la nouvelle position."));
  }

  private void confirmDelete(Player player, EntityEntry entry) {
    guis.confirm(
        player,
        new GuiConfirmation(
            MINI.deserialize("<red>Supprimer un élément"),
            MINI.deserialize("<white>" + entry.id()),
            MINI.deserialize("<gray>Les références orphelines seront signalées par la validation."),
            clock.instant().plusSeconds(1),
            click ->
                editors
                    .mutate(
                        player.getUniqueId(),
                        map -> map.without(entry.kind(), entry.id(), clock.instant()))
                    .whenCompleteAsync(
                        (ignored, failure) -> {
                          player.sendMessage(
                              failure == null
                                  ? MINI.deserialize("<green>Élément supprimé.")
                                  : MINI.deserialize("<red>Suppression échouée."));
                          guis.back(player);
                        },
                        mainThread)));
  }

  private void duplicate(Player player, EntityEntry entry) {
    var session = editors.session(player.getUniqueId()).orElseThrow();
    String prefix = entry.id() + "_copy_";
    int suffix = 1;
    while (contains(session.definition(), entry.kind(), prefix + suffix)) {
      suffix++;
    }
    String duplicateId = prefix + suffix;
    editors
        .mutate(
            player.getUniqueId(),
            map -> map.duplicated(entry.kind(), entry.id(), duplicateId, clock.instant()))
        .whenCompleteAsync(
            (ignored, failure) -> {
              player.sendMessage(
                  failure == null
                      ? MINI.deserialize("<green>Élément dupliqué : <white>" + duplicateId)
                      : MINI.deserialize("<red>Duplication échouée."));
              guis.refresh(player);
            },
            mainThread);
  }

  private static boolean contains(MapDefinition map, String kind, String id) {
    return switch (kind) {
      case "zone" -> map.zones().containsKey(id);
      case "door" -> map.doors().containsKey(id);
      case "spawn" -> map.zombieSpawns().containsKey(id);
      case "object" -> map.objects().containsKey(id);
      default -> false;
    };
  }

  private void informationButton(
      GuiView view, int slot, Material material, String label, String value, String field) {
    view.button(
        slot,
        material,
        "<yellow>" + label,
        List.of(
            "<gray>Valeur : <white>" + (value.isBlank() ? "<non définie>" : value),
            "<aqua>Cliquez pour modifier par chat."),
        "zombie.editor",
        click -> editInformation(click.player(), field, label.toLowerCase(java.util.Locale.ROOT)),
        null,
        null);
  }

  private void editInformation(Player player, String field, String label) {
    guis.requestInput(
        player,
        new GuiInputRequest(
            MINI.deserialize("<aqua>Entrez " + label + " dans le chat."),
            guis.inputExpiry(),
            value ->
                validateInformation(field, value)
                    ? GuiInputRequest.Validation.accept()
                    : GuiInputRequest.Validation.reject(MINI.deserialize("<red>Valeur invalide.")),
            value ->
                editors.mutate(
                    player.getUniqueId(),
                    map -> map.withGeneralInformation(field, value, clock.instant())),
            () -> {}));
  }

  private static boolean validateInformation(String field, String value) {
    if (field.equals("maximum-players")) {
      try {
        int amount = Integer.parseInt(value);
        return amount > 0 && amount <= 1000;
      } catch (NumberFormatException ignored) {
        return false;
      }
    }
    int maximum = field.equals("description") ? 512 : field.equals("image") ? 256 : 128;
    boolean mayBeEmpty =
        field.equals("description") || field.equals("image") || field.equals("music");
    return value.length() <= maximum && (mayBeEmpty || !value.isBlank());
  }

  private void selectTool(Player player, EditorTool tool, MapObjectType type) {
    var session = editors.session(player.getUniqueId()).orElseThrow();
    session.tool(tool);
    if (type != null) {
      session.selectedObjectType(type);
    }
    items.give(player, session);
    player.closeInventory();
  }

  private void showValidation(Player player) {
    var session = editors.session(player.getUniqueId()).orElse(null);
    if (session == null) {
      return;
    }
    var report = validator.validate(session.definition());
    player.sendMessage(
        MINI.deserialize(
            report.valid()
                ? "<green>Map valide."
                : "<red>" + report.errors().size() + " erreur(s) détectée(s)."));
    report.errors().forEach(value -> player.sendMessage(MINI.deserialize("<red>✘ " + value)));
    report.warnings().forEach(value -> player.sendMessage(MINI.deserialize("<gold>⚠ " + value)));
  }

  private void save(Player player) {
    editors
        .save(player.getUniqueId())
        .whenCompleteAsync(
            (ignored, failure) ->
                player.sendMessage(
                    failure == null
                        ? MINI.deserialize("<green>Map sauvegardée.")
                        : MINI.deserialize("<red>Sauvegarde échouée.")),
            mainThread);
  }

  private static List<EntityEntry> entries(MapDefinition map, String category) {
    List<EntityEntry> values = new ArrayList<>();
    if (category.equals("zones")) {
      map.zones()
          .values()
          .forEach(zone -> values.add(new EntityEntry("zone", zone.id(), Material.LIGHT_BLUE_DYE)));
    } else if (category.equals("doors")) {
      map.doors()
          .values()
          .forEach(door -> values.add(new EntityEntry("door", door.id(), Material.IRON_DOOR)));
    } else if (category.equals("spawns")) {
      map.zombieSpawns()
          .values()
          .forEach(spawn -> values.add(new EntityEntry("spawn", spawn.id(), Material.ZOMBIE_HEAD)));
    } else {
      MapObjectType type = MapObjectType.valueOf(category);
      map.objects().values().stream()
          .filter(object -> object.type() == type)
          .forEach(object -> values.add(new EntityEntry("object", object.id(), material(type))));
    }
    return List.copyOf(values);
  }

  private static Material material(MapObjectType type) {
    return switch (type) {
      case BARRICADE -> Material.OAK_TRAPDOOR;
      case MYSTERY_BOX -> Material.ENDER_CHEST;
      case PACK_A_PUNCH -> Material.ANVIL;
      case PERK -> Material.POTION;
      case TRAP -> Material.TRIPWIRE_HOOK;
      case TELEPORTER -> Material.END_PORTAL_FRAME;
      case POWER -> Material.LEVER;
      case OBJECTIVE -> Material.TARGET;
      case QUEST -> Material.WRITABLE_BOOK;
      case BOSS -> Material.WITHER_SKELETON_SKULL;
    };
  }

  private record EntityEntry(String kind, String id, Material material) {}
}
