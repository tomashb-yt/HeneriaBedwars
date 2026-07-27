package fr.heneria.zombie.plugin.gui;

import fr.heneria.zombie.core.editor.MapDefinition;
import fr.heneria.zombie.core.editor.MapEditorService;
import fr.heneria.zombie.core.editor.MapPoint;
import fr.heneria.zombie.core.editor.MapPublication;
import fr.heneria.zombie.core.editor.MapPublicationService;
import fr.heneria.zombie.core.editor.MapStatus;
import fr.heneria.zombie.core.editor.MapValidator;
import fr.heneria.zombie.core.editor.PublishedMapVersion;
import fr.heneria.zombie.core.instance.GameInstanceSnapshot;
import fr.heneria.zombie.core.instance.GameInstanceState;
import fr.heneria.zombie.plugin.editor.EditorItemService;
import fr.heneria.zombie.plugin.editor.MapVisualizationService;
import fr.heneria.zombie.plugin.editor.MapWorldPublicationService;
import fr.heneria.zombie.plugin.game.PaperGameRuntime;
import fr.heneria.zombie.plugin.instance.InstanceCoordinator;
import fr.heneria.zombie.plugin.instance.PlayerInstanceResult;
import fr.heneria.zombie.plugin.map.MapPreviewService;
import fr.heneria.zombie.plugin.map.MapTemplateCatalog;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

/** Player catalogue and administrative publication workflow built on the shared GUI engine. */
public final class MapMenuModule {
  public static final java.util.Set<String> ACTION_IDS =
      java.util.Set.of(
          "maps.player",
          "maps.admin",
          "maps.create",
          "maps.duplicate",
          "maps.archive",
          "maps.delete",
          "maps.visit",
          "maps.edit",
          "maps.validate",
          "maps.test",
          "maps.publish",
          "maps.unpublish",
          "maps.history",
          "maps.join",
          "maps.leave");

  private static final MiniMessage MINI = MiniMessage.miniMessage();
  private final GuiRegistry registry;
  private final GuiActionRegistry actions;
  private final GuiConfigurationService configurations;
  private final GuiService guis;
  private final MapEditorService editors;
  private final MapValidator validator;
  private final MapPublicationService publications;
  private final MapTemplateCatalog templates;
  private final MapPreviewService previews;
  private final MapWorldPublicationService worldPublications;
  private final EditorItemService editorItems;
  private final MapVisualizationService visualizations;
  private final InstanceCoordinator instances;
  private final PaperGameRuntime games;
  private final Executor mainThread;
  private final Clock clock;

  public MapMenuModule(
      GuiRegistry registry,
      GuiActionRegistry actions,
      GuiConfigurationService configurations,
      GuiService guis,
      MapEditorService editors,
      MapValidator validator,
      MapPublicationService publications,
      MapTemplateCatalog templates,
      MapPreviewService previews,
      MapWorldPublicationService worldPublications,
      EditorItemService editorItems,
      MapVisualizationService visualizations,
      InstanceCoordinator instances,
      PaperGameRuntime games,
      Executor mainThread,
      Clock clock) {
    this.registry = registry;
    this.actions = actions;
    this.configurations = configurations;
    this.guis = guis;
    this.editors = editors;
    this.validator = validator;
    this.publications = publications;
    this.templates = templates;
    this.previews = previews;
    this.worldPublications = worldPublications;
    this.editorItems = editorItems;
    this.visualizations = visualizations;
    this.instances = instances;
    this.games = games;
    this.mainThread = mainThread;
    this.clock = clock;
  }

  /** Registers catalogue, management, detail and version-history screens. */
  public void register() {
    registry.register(
        new StandardGui("player-maps", configurations, this::renderPlayerMaps, () -> 20));
    registry.register(
        new StandardGui("admin-maps", configurations, this::renderAdminMaps, () -> 20));
    registry.register(
        new StandardGui("admin-map-detail", configurations, this::renderMapDetail, () -> 20));
    registry.register(
        new StandardGui("admin-map-history", configurations, this::renderHistory, () -> 0));
    registry.register(
        new StandardGui("admin-map-validation", configurations, this::renderValidation, () -> 0));
    actions.register(
        "maps.player", context -> guis.open(context.player(), new GuiId("player-maps")));
    actions.register("maps.admin", context -> guis.open(context.player(), new GuiId("admin-maps")));
    actions.register("maps.create", context -> requestCreate(context.player()));
    actions.register(
        "maps.duplicate", context -> requestDuplicate(context.player(), selected(context)));
    actions.register(
        "maps.archive", context -> requestArchive(context.player(), selected(context)));
    actions.register(
        "maps.delete", context -> requestDeletion(context.player(), selected(context)));
    actions.register("maps.visit", context -> visit(context.player(), selected(context)));
    actions.register("maps.edit", context -> edit(context.player(), selected(context)));
    actions.register(
        "maps.validate",
        context ->
            guis.open(
                context.player(),
                new GuiId("admin-map-validation"),
                GuiContext.of("map", selected(context))));
    actions.register("maps.test", context -> test(context.player(), selected(context)));
    actions.register("maps.publish", context -> publish(context.player(), selected(context)));
    actions.register("maps.unpublish", context -> unpublish(context.player(), selected(context)));
    actions.register(
        "maps.history",
        context ->
            guis.open(
                context.player(),
                new GuiId("admin-map-history"),
                GuiContext.of("map", selected(context))));
    actions.register("maps.join", context -> joinPublished(context.player(), selected(context)));
    actions.register(
        "maps.leave",
        context -> {
          games.left(context.player().getUniqueId());
          instances.leave(context.player());
          context.player().closeInventory();
          context.player().sendMessage(MINI.deserialize("<green>Retour au lobby."));
        });
  }

  private void renderPlayerMaps(GuiView view, GuiContext ignored) {
    GuiSession session = guis.session(view.player());
    List<PublishedMapVersion> maps =
        publications.published().stream()
            .filter(
                version ->
                    session.search().isBlank()
                        || version
                            .definition()
                            .displayName()
                            .toLowerCase(Locale.ROOT)
                            .contains(session.search().toLowerCase(Locale.ROOT)))
            .toList();
    GuiPagination.Page<PublishedMapVersion> page =
        GuiPagination.page(maps, session.page(), view.menu().contentSlots().size());
    session.page(page.index());
    for (int index = 0; index < page.items().size(); index++) {
      PublishedMapVersion version = page.items().get(index);
      MapDefinition map = version.definition();
      List<GameInstanceSnapshot> active = publicInstances(map.id());
      int players = active.stream().mapToInt(value -> value.players().size()).sum();
      view.button(
          view.menu().contentSlots().get(index),
          material(map.icon()),
          "<green>" + map.displayName(),
          List.of(
              "<gray>" + empty(map.description(), "Aucune description"),
              "",
              "<gray>DifficultÃ© : <white>" + map.difficulty(),
              "<gray>Mode : <white>" + map.gameMode(),
              "<gray>Joueurs : <white>" + map.minimumPlayers() + "-" + map.maximumPlayers(),
              "<gray>Ã‰tat : <white>" + playerStatus(active),
              "<gray>Parties : <white>" + active.size(),
              "<gray>En jeu : <white>" + players,
              "<gray>Version : <white>v" + version.version(),
              "",
              "<yellow>Cliquez pour rejoindre"),
          "zombies.menu.player",
          context -> joinPublished(context.player(), map.id()),
          null,
          null);
    }
    navigation(view, page, maps.size(), "map(s) publiÃ©e(s)");
  }

  private void renderAdminMaps(GuiView view, GuiContext ignored) {
    GuiSession session = guis.session(view.player());
    List<MapDefinition> maps =
        editors.registry().all().stream()
            .filter(
                map ->
                    session.search().isBlank()
                        || map.id()
                            .toLowerCase(Locale.ROOT)
                            .contains(session.search().toLowerCase(Locale.ROOT))
                        || map.displayName()
                            .toLowerCase(Locale.ROOT)
                            .contains(session.search().toLowerCase(Locale.ROOT)))
            .sorted(Comparator.comparing(MapDefinition::id))
            .toList();
    GuiPagination.Page<MapDefinition> page =
        GuiPagination.page(maps, session.page(), view.menu().contentSlots().size());
    session.page(page.index());
    for (int index = 0; index < page.items().size(); index++) {
      MapDefinition map = page.items().get(index);
      MapPublication publication = publications.publication(map.id());
      var report = validator.validate(map);
      view.button(
          view.menu().contentSlots().get(index),
          statusMaterial(publication.status()),
          "<aqua>" + map.displayName(),
          List.of(
              "<gray>ID : <white>" + map.id(),
              "<gray>Ã‰tat : <white>" + publication.status(),
              "<gray>Version active : <white>"
                  + publication.activeVersion().map(value -> "v" + value).orElse("aucune"),
              "<gray>Erreurs : "
                  + (report.errors().isEmpty() ? "<green>0" : "<red>" + report.errors().size()),
              "<gray>Avertissements : <gold>" + report.warnings().size(),
              "<gray>Ã‰diteur : <white>"
                  + editors.editorOf(map.id()).map(UUID::toString).orElse("libre"),
              "",
              "<yellow>Cliquez pour gÃ©rer"),
          "zombies.admin.maps.view",
          context ->
              guis.open(
                  context.player(), new GuiId("admin-map-detail"), GuiContext.of("map", map.id())),
          null,
          null);
    }
    navigation(view, page, maps.size(), "map(s)");
    view.configured("create");
  }

  private void renderMapDetail(GuiView view, GuiContext context) {
    String id = context.value("map", String.class).orElse("");
    MapDefinition map = editors.registry().find(id).orElse(null);
    if (map == null) {
      view.information(22, Material.BARRIER, "<red>Map introuvable", List.of());
      view.configured("back");
      return;
    }
    MapPublication publication = publications.publication(id);
    var report = validator.validate(map);
    view.information(
        4,
        statusMaterial(publication.status()),
        "<aqua>" + map.displayName(),
        List.of(
            "<gray>ID : <white>" + id,
            "<gray>Ã‰tat : <white>" + publication.status(),
            "<gray>Monde de travail : <white>" + map.world(),
            "<gray>Template visitable : "
                + (templates.knownMapIds().contains(id) ? "<green>disponible" : "<red>absent"),
            "<gray>Version publiÃ©e : <white>"
                + publication.activeVersion().map(value -> "v" + value).orElse("aucune"),
            "<gray>Validation : "
                + (report.valid()
                    ? "<green>valide"
                    : "<red>" + report.errors().size() + " erreur(s)"),
            "",
            "<gray>Zones : <white>"
                + map.zones().size()
                + " <dark_gray>â€¢ <gray>Spawns : <white>"
                + map.zombieSpawns().size()
                + " <dark_gray>â€¢ <gray>Objets : <white>"
                + map.objects().size()));
    view.information(
        1,
        Material.COMPASS,
        "<aqua>Ã‰tape 1 â€” Explorer et prÃ©parer",
        List.of(
            "<gray>Visitez d'abord le template.",
            "<gray>Modifiez ensuite la copie de travail.",
            "<gray>Terminez par la vÃ©rification."));
    view.information(
        19,
        Material.CLOCK,
        "<light_purple>Ã‰tape 2 â€” Tester et publier",
        List.of(
            "<gray>Le test lance une partie privÃ©e.",
            "<gray>La publication rend un snapshot",
            "<gray>accessible depuis /zombies."));
    view.information(
        37,
        Material.CHEST,
        "<gold>Ã‰tape 3 â€” Cycle de vie",
        List.of(
            "<gray>Archiver conserve les fichiers.",
            "<red>Supprimer efface dÃ©finitivement",
            "<red>tout le contenu possÃ©dÃ©."));
    for (String key :
        List.of("visit", "edit", "duplicate", "validate", "test", "history", "archive", "delete")) {
      view.configured(key);
    }
    if (publication.status() == MapStatus.PUBLISHED) {
      view.configured("unpublish");
    } else if (report.valid()) {
      view.configured("publish");
    }
    view.configured("back");
    view.configured("home");
  }

  private void renderHistory(GuiView view, GuiContext context) {
    String id = context.value("map", String.class).orElse("");
    List<PublishedMapVersion> versions = publications.publication(id).versions();
    GuiSession session = guis.session(view.player());
    GuiPagination.Page<PublishedMapVersion> page =
        GuiPagination.page(versions.reversed(), session.page(), view.menu().contentSlots().size());
    session.page(page.index());
    for (int index = 0; index < page.items().size(); index++) {
      PublishedMapVersion version = page.items().get(index);
      view.button(
          view.menu().contentSlots().get(index),
          Material.BOOK,
          "<gold>Version " + version.version(),
          List.of(
              "<gray>Publication : <white>" + version.publishedAt(),
              "<gray>Auteur : <white>" + version.publishedBy(),
              "<gray>Nom : <white>" + version.definition().displayName(),
              "",
              "<yellow>Cliquez pour restãÎ¸¶‰žËkºwµçI•¹Ñ	±½­•È€ô‘•±•Ñ¥½¹	±½­•È¡µ…Á%¤ì4(€€€€€€€€€€€€€¥˜€¡ÕÉÉ•¹Ñ	±½­•È€„ô¹Õ±°¤ì4(€€€€€€€€€€€€€€€½¹Ñ•áÐ¹Í•ÍÍ¥½¸ ¤¹½¹™¥Éµ…Ñ¥½¸¡¹Õ±°¤ì4(€€€€€€€€€€€€€€€Á±…å•È¹Í•¹‘5•ÍÍ…” 4(€€€€€€€€€€€€€€€€€€€5%9$¹‘•Í•É¥…±¥é” ˆñÉ•ùMÕÁÁÉ•ÍÍ¥½¸É•™ÕÏ¥”€è€ˆ€¬ÕÉÉ•¹Ñ	±½­•È¤¤ì4(€€€€€€€€€€€€€€€Õ¥Ì¹‰…¬¡Á±…å•È¤ì4(€€€€€€€€€€€€€€€É•ÑÕÉ¸ì4(€€€€€€€€€€€€€ô4(€€€€€€€€€€€€€Ý½É±‘AÕ‰±¥…Ñ¥½¹Ì4(€€€€€€€€€€€€€€€€€€¹ÁÉ•Á…É••±•Ñ¥½¸¡‘•™¥¹¥Ñ¥½¸¤4(€€€€€€€€€€€€€€€€€€¹Ñ¡•¹½µÁ½Í”¡¥¹½É•€´øÁÕ‰±¥…Ñ¥½¹Ì¹‘•±•Ñ”¡µ…Á%°€ ¤€´ø•‘¥Ñ½ÉÌ¹‘•±•Ñ”¡µ…Á%¤¤¤4(€€€€€€€€€€€€€€€€€€¹Ñ¡•¹IÕ¸ 4(€€€€€€€€€€€€€€€€€€€€€€ ¤€´øì4(€€€€€€€€€€€€€€€€€€€€€€€Ñ•µÁ±…Ñ•Ì¹É•™É•Í¡½Õ¹Ð ¤ì4(€€€€€€€€€€€€€€€€€€€€€ô¤4(€€€€€€€€€€€€€€€€€€¹Ý¡•¹½µÁ±•Ñ•Íå¹Œ 4(€€€€€€€€€€€€€€€€€€€€€€¡‘•±•Ñ•°™…¥±ÕÉ”¤€´øì4(€€€€€€€€€€€€€€€€€€€€€€€½¹Ñ•áÐ¹Í•ÍÍ¥½¸ ¤¹½¹™¥Éµ…Ñ¥½¸¡¹Õ±°¤ì4(€€€€€€€€€€€€€€€€€€€€€€€¥˜€¡™…¥±ÕÉ”€ôô¹Õ±°¤ì4(€€€€€€€€€€€€€€€€€€€€€€€€€Á±…å•È¹Í•¹‘5•ÍÍ…” 4(€€€€€€€€€€€€€€€€€€€€€€€€€€€€€5%9$¹‘•Í•É¥…±¥é” 4(€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€ˆñÉ••¸ù5…ÀÍÕÁÁÉ¥·¥”…Ù•ŒÍ„½¹™¥ÕÉ…Ñ¥½¸°Í•ÌÙ•ÉÍ¥½¹Ì•ÐÍ•Ìµ½¹‘•Ì¸ˆ¤¤ì4(€€€€€€€€€€€€€€€€€€€€€€€€€Õ¥Ì¹½Á•¸¡Á±…å•È°¹•ÜÕ¥% ‰…‘µ¥¸µµ…ÁÌˆ¤¤ì4(€€€€€€€€€€€€€€€€€€€€€€€ô•±Í”ì4(€€€€€€€€€€€€€€€€€€€€€€€€€Á±…å•È¹Í•¹‘5•ÍÍ…” 4(€€€€€€€€€€€€€€€€€€€€€€€€€€€€€5%9$¹‘•Í•É¥…±¥é” ˆñÉ•ùMÕÁÁÉ•ÍÍ¥½¸¥µÁ½ÍÍ¥‰±”€è€ˆ€¬Í…™”¡™…¥±ÕÉ”¤¤¤ì4(€€€€€€€€€€€€€€€€€€€€€€€€€Õ¥Ì¹‰…¬¡Á±…å•È¤ì4(€€€€€€€€€€€€€€€€€€€€€€€ô4(€€€€€€€€€€€€€€€€€€€€€ô°4(€€€€€€€€€€€€€€€€€€€€€µ…¥¹Q¡É•…¤ì4(€€€€€€€€€€€ô¤¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”MÑÉ¥¹œ‘•±•Ñ¥½¹	±½­•È¡MÑÉ¥¹œµ…Á%¤ì4(€€€¥˜€¡¥¹ÍÑ…¹•Ì¹…Ñ¥Ù•%¹ÍÑ…¹•Ì ¤¹ÍÑÉ•…´ ¤¹…¹å5…Ñ ¡Ù…±Õ”€´øÙ…±Õ”¹µ…Á% ¤¹•ÅÕ…±Ì¡µ…Á%¤¤¤ì4(€€€€€É•ÑÕÉ¸€‰Õ¹”¥¹ÍÑ…¹”ÕÑ¥±¥Í”•¹½É”•ÑÑ”µ…À¸ˆì4(€€€ô4(€€€¥˜€¡•‘¥Ñ½ÉÌ¹•‘¥Ñ½É=˜¡µ…Á%¤¹¥ÍAÉ•Í•¹Ð ¤¤ì4(€€€€€É•ÑÕÉ¸€‰±„µ…À•ÍÐ•¹½É”½ÕÙ•ÉÑ”‘…¹Ì°Ÿ¥‘¥Ñ•ÕÈ¸ˆì4(€€€ô4(€€€É•ÑÕÉ¸¹Õ±°ì4(€ô4(4(€ÁÉ¥Ù…Ñ”Ù½¥Ñ•ÍÐ¡A±…å•ÈÁ±…å•È°MÑÉ¥¹œµ…Á%¤ì4(€€€5…Á•™¥¹¥Ñ¥½¸µ…À€ô•‘¥Ñ½ÉÌ¹É•¥ÍÑÉä ¤¹™¥¹¡µ…Á%¤¹½É±Í”¡¹Õ±°¤ì4(€€€¥˜€¡µ…À€ôô¹Õ±°ñð€…Ù…±¥‘…Ñ½È¹Ù…±¥‘…Ñ”¡µ…À¤¹Ù…±¥ ¤¤ì4(€€€€€Á±…å•È¹Í•¹‘5•ÍÍ…”¡5%9$¹‘•Í•É¥…±¥é” ˆñÉ•ù1„µ…À‘½¥Ðƒ©ÑÉ”Ù…±¥‘”…Ù…¹Ð±”Ñ•ÍÐ¸ˆ¤¤ì4(€€€€€É•ÑÕÉ¸ì4(€€€ô4(€€€¥˜€¡±½…‘‘¥Ñ¥¹]½É±¡µ…À¤€ôô¹Õ±°¤ì4(€€€€€Á±…å•È¹Í•¹‘5•ÍÍ…”¡5%9$¹‘•Í•É¥…±¥é” ˆñÉ•ù1”µ½¹‘”Ÿ¥‘¥Ñ¥½¸¹”Á•ÕÐÁ…Ìƒ©ÑÉ”¡…ÉŸ¤¸ˆ¤¤ì4(€€€€€É•ÑÕÉ¸ì4(€€€ô4(€€€=ÁÑ¥½¹…°ñUU%ø•‘¥Ñ½È€ô•‘¥Ñ½ÉÌ¹•‘¥Ñ½É=˜¡µ…Á%¤ì4(€€€¥˜€¡•‘¥Ñ½È¹¥ÍAÉ•Í•¹Ð ¤€˜˜€…•‘¥Ñ½È¹½É±Í•Q¡É½Ü ¤¹•ÅÕ…±Ì¡Á±…å•È¹•ÑU¹¥ÅÕ•% ¤¤¤ì4(€€€€€Á±…å•È¹Í•¹‘5•ÍÍ…”¡5%9$¹‘•Í•É¥…±¥é” ˆñÉ•ù1„µ…À•ÍÐ•¹½É”µ½‘¥™§¥”Á…ÈÕ¸…ÕÑÉ”…‘µ¥¸¸ˆ¤¤ì4(€€€€€É•ÑÕÉ¸ì4(€€€ô4(€€€Á±…å•È¹±½Í•%¹Ù•¹Ñ½Éä ¤ì4(€€€½µÁ±•Ñ…‰±•ÕÑÕÉ”ñY½¥ø±½Í•‘‘¥Ñ½Èì4(€€€¥˜€¡•‘¥Ñ½ÉÌ4(€€€€€€€€¹Í•ÍÍ¥½¸¡Á±…å•È¹•ÑU¹¥ÅÕ•% ¤¤4(€€€€€€€€¹™¥±Ñ•È¡Í•ÍÍ¥½¸€´øÍ•ÍÍ¥½¸¹‘•™¥¹¥Ñ¥½¸ ¤¹¥ ¤¹•ÅÕ…±Ì¡µ…Á%¤¤4(€€€€€€€€¹¥ÍAÉ•Í•¹Ð ¤¤ì4(€€€€€Ù¥ÍÕ…±¥é…Ñ¥½¹Ì¹±•…É‘¥Ñ½È¡Á±…å•È¹•ÑU¹¥ÅÕ•% ¤¤ì4(€€€€€•‘¥Ñ½É%Ñ•µÌ¹É•µ½Ù”¡Á±…å•È¤ì4(€€€€€±½Í•‘‘¥Ñ½È€ô•‘¥Ñ½ÉÌ¹±•…Ù”¡Á±…å•È¹•ÑU¹¥ÅÕ•% ¤¤¹Ñ¡•¹ÁÁ±ä¡¥¹½É•€´ø¹Õ±°¤ì4(€€€€€¥¹ÍÑ…¹•Ì¹±•…Ù”¡Á±…å•È¤ì4(€€€ô•±Í”ì4(€€€€€±½Í•‘‘¥Ñ½È€ô½µÁ±•Ñ…‰±•ÕÑÕÉ”¹½µÁ±•Ñ•‘ÕÑÕÉ”¡¹Õ±°¤ì4(€€€ô4(€€€±½Í•‘‘¥Ñ½È4(€€€€€€€€¹Ñ¡•¹½µÁ½Í•Íå¹Œ¡¥¹½É•€´øÁÉ•Ù¥•ÝÌ¹±•…Ù”¡Á±…å•È¤°µ…¥¹Q¡É•…¤4(€€€€€€€€¹Ñ¡•¹½µÁ½Í•Íå¹Œ 4(€€€€€€€€€€€¥¹½É•€´øì4(€€€€€€€€€€€€€…µ•Ì¹±•™Ð¡Á±…å•È¹•ÑU¹¥ÅÕ•% ¤¤ì4(€€€€€€€€€€€€€¥¹ÍÑ…¹•Ì¹±•…Ù”¡Á±…å•È¤ì4(€€€€€€€€€€€€€É•ÑÕÉ¸Ý½É±‘AÕ‰±¥…Ñ¥½¹Ì¹ÕÁ‘…Ñ•Q•µÁ±…Ñ”¡µ…À¤ì4(€€€€€€€€€€€ô°4(€€€€€€€€€€€µ…¥¹Q¡É•…¤4(€€€€€€€€¹Ñ¡•¹½µÁ½Í” 4(€€€€€€€€€€€¥¹½É•€´ø4(€€€€€€€€€€€€€€€¥¹ÍÑ…¹•Ì¹É•…Ñ”¡µ…Á%°=ÁÑ¥½¹…°¹½˜¡Á±…å•È¹•ÑU¹¥ÅÕ•% ¤¤°µ…À¹µ…á¥µÕµA±…å•ÉÌ ¤¤¤4(€€€€€€€€¹Ñ¡•¹½µÁ½Í” 4(€€€€€€€€€€€É•…Ñ•€´ø4(€€€€€€€€€€€€€€€¥¹ÍÑ…¹•Ì4(€€€€€€€€€€€€€€€€€€€€¹©½¥¸¡Á±…å•È°É•…Ñ•¹¥ ¤¤4(€€€€€€€€€€€€€€€€€€€€¹Ñ¡•¹ÁÁ±ä 4(€€€€€€€€€€€€€€€€€€€€€€€É•ÍÕ±Ð€´øì4(€€€€€€€€€€€€€€€€€€€€€€€€€¥˜€¡É•ÍÕ±Ð€„ôA±…å•É%¹ÍÑ…¹•I•ÍÕ±Ð¹MUML¤ì4(€€€€€€€€€€€€€€€€€€€€€€€€€€€Ñ¡É½Ü¹•Ü%±±•…±MÑ…Ñ•á•ÁÑ¥½¸ ‰¹ÑË¥”É•™ÕÏ¥”€è€ˆ€¬É•ÍÕ±Ð¤ì4(€€€€€€€€€€€€€€€€€€€€€€€€€ô4(€€€€€€€€€€€€€€€€€€€€€€€€€…µ•Ì¹ÍÑ…ÉÐ¡É•…Ñ•¹¥ ¤¤ì4(€€€€€€€€€€€€€€€€€€€€€€€€€É•ÑÕÉ¸É•…Ñ•ì4(€€€€€€€€€€€€€€€€€€€€€€€ô¤¤4(€€€€€€€€¹Ý¡•¹½µÁ±•Ñ•Íå¹Œ 4(€€€€€€€€€€€€¡É•…Ñ•°™…¥±ÕÉ”¤€´ø4(€€€€€€€€€€€€€€€Á±…å•È¹Í•¹‘5•ÍÍ…” 4(€€€€€€€€€€€€€€€€€€€™…¥±ÕÉ”€ôô¹Õ±°4(€€€€€€€€€€€€€€€€€€€€€€€€ü5%9$¹‘•Í•É¥…±¥é” ˆñÉ••¸ùA…ÉÑ¥”‘”Ñ•ÍÐ“¥µ…ÉË¥”¸ˆ¤4(€€€€€€€€€€€€€€€€€€€€€€€€è5%9$¹‘•Í•É¥…±¥é” ˆñÉ•ùQ•ÍÐ¥µÁ½ÍÍ¥‰±”€è€ˆ€¬Í…™”¡™…¥±ÕÉ”¤¤¤°4(€€€€€€€€€€€µ…¥¹Q¡É•…¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”Ù½¥©½¥¹AÕ‰±¥Í¡•¡A±…å•ÈÁ±…å•È°MÑÉ¥¹œµ…Á%¤ì4(€€€¥˜€¡ÁÕ‰±¥…Ñ¥½¹Ì¹ÁÕ‰±¥Í¡•‘•™¥¹¥Ñ¥½¸¡µ…Á%¤¹¥ÍµÁÑä ¤¤ì4(€€€€€Á±…å•È¹Í•¹‘5•ÍÍ…”¡5%9$¹‘•Í•É¥…±¥é” ˆñÉ•ù•ÑÑ”µ…À¸•ÍÐÁ±ÕÌÁÕ‰±§¥”¸ˆ¤¤ì4(€€€€€É•ÑÕÉ¸ì4(€€€ô4(€€€Á±…å•È¹±½Í•%¹Ù•¹Ñ½Éä ¤ì4(€€€=ÁÑ¥½¹…°ñ…µ•%¹ÍÑ…¹•M¹…ÁÍ¡½Ðø…Ù…¥±…‰±”€ô4(€€€€€€€ÁÕ‰±¥%¹ÍÑ…¹•Ì¡µ…Á%¤¹ÍÑÉ•…´ ¤4(€€€€€€€€€€€€¹™¥±Ñ•È 4(€€€€€€€€€€€€€€€Ù…±Õ”€´ø4(€€€€€€€€€€€€€€€€€€€Ù…±Õ”¹Á±…å•ÉÌ ¤¹Í¥é” ¤€ðÙ…±Õ”¹µ…á¥µÕµA±…å•ÉÌ ¤4(€€€€€€€€€€€€€€€€€€€€€€€€˜˜€¡Ù…±Õ”¹ÍÑ…Ñ” ¤€ôô…µ•%¹ÍÑ…¹•MÑ…Ñ”¹]%Q%94(€€€€€€€€€€€€€€€€€€€€€€€€€€€ñðÙ…±Õ”¹ÍÑ…Ñ” ¤€ôô…µ•%¹ÍÑ…¹•MÑ…Ñ”¹MQIQ%94(€€€€€€€€€€€€€€€€€€€€€€€€€€€ñðÙ…±Õ”¹ÍÑ…Ñ” ¤€ôô…µ•%¹ÍÑ…¹•MÑ…Ñ”¹IU99%9¤¤4(€€€€€€€€€€€€¹™¥¹‘¥ÉÍÐ ¤ì4(€€€¥˜€¡…Ù…¥±…‰±”¹¥ÍAÉ•Í•¹Ð ¤¤ì4(€€€€€©½¥¹á¥ÍÑ¥¹œ¡Á±…å•È°…Ù…¥±…‰±”¹½É±Í•Q¡É½Ü ¤¤ì4(€€€€€É•ÑÕÉ¸ì4(€€€ô4(€€€Á±…å•È¹Í•¹‘5•ÍÍ…”¡5%9$¹‘•Í•É¥…±¥é” ˆñå•±±½ÜùË¥…Ñ¥½¸…ÕÑ½µ…Ñ¥ÅÕ”Õ¹”Á…ÉÑ¥”¸¸¸ˆ¤¤ì4(€€€¥¹ÍÑ…¹•Ì4(€€€€€€€€¹É•…Ñ” 4(€€€€€€€€€€€µ…Á%°4(€€€€€€€€€€€=ÁÑ¥½¹…°¹•µÁÑä ¤°4(€€€€€€€€€€€ÁÕ‰±¥…Ñ¥½¹Ì¹ÁÕ‰±¥Í¡•‘•™¥¹¥Ñ¥½¸¡µ…Á%¤¹½É±Í•Q¡É½Ü ¤¹µ…á¥µÕµA±…å•ÉÌ ¤¤4(€€€€€€€€¹Ñ¡•¹½µÁ½Í” 4(€€€€€€€€€€€É•…Ñ•€´ø4(€€€€€€€€€€€€€€€¥¹ÍÑ…¹•Ì4(€€€€€€€€€€€€€€€€€€€€¹©½¥¸¡Á±…å•È°É•…Ñ•¹¥ ¤¤4(€€€€€€€€€€€€€€€€€€€€¹Ñ¡•¹ÁÁ±ä 4(€€€€€€€€€€€€€€€€€€€€€€€É•ÍÕ±Ð€´øì4(€€€€€€€€€€€€€€€€€€€€€€€€€¥˜€¡É•ÍÕ±Ð€„ôA±…å•É%¹ÍÑ…¹•I•ÍÕ±Ð¹MUML¤ì4(€€€€€€€€€€€€€€€€€€€€€€€€€€€Ñ¡É½Ü¹•Ü%±±•…±MÑ…Ñ•á•ÁÑ¥½¸ ‰¹ÑË¥”É•™ÕÏ¥”€è€ˆ€¬É•ÍÕ±Ð¤ì4(€€€€€€€€€€€€€€€€€€€€€€€€€ô4(€€€€€€€€€€€€€€€€€€€€€€€€€5…Á•™¥¹¥Ñ¥½¸ÁÕ‰±¥Í¡•€ô4(€€€€€€€€€€€€€€€€€€€€€€€€€€€€€ÁÕ‰±¥…Ñ¥½¹Ì¹ÁÕ‰±¥Í¡•‘•™¥¹¥Ñ¥½¸¡µ…Á%¤¹½É±Í•Q¡É½Ü ¤ì4(€€€€€€€€€€€€€€€€€€€€€€€€€¥¹ÐÁ±…å•ÉÌ€ô4(€€€€€€€€€€€€€€€€€€€€€€€€€€€€€¥¹ÍÑ…¹•Ì¹…Ñ¥Ù•%¹ÍÑ…¹•Ì ¤¹ÍÑÉ•…´ ¤4(€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€¹™¥±Ñ•È¡Ù…±Õ”€´øÙ…±Õ”¹¥ ¤¹•ÅÕ…±Ì¡É•…Ñ•¹¥ ¤¤¤4(€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€¹µ…ÁQ½%¹Ð¡Ù…±Õ”€´øÙ…±Õ”¹Á±…å•ÉÌ ¤¹Í¥é” ¤¤4(€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€¹™¥¹‘¥ÉÍÐ ¤4(€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€€¹½É±Í” À¤ì4(€€€€€€€€€€€€€€€€€€€€€€€€€¥˜€¡Á±…å•ÉÌ€øôÁÕ‰±¥Í¡•¹µ¥¹¥µÕµA±…å•ÉÌ ¤¤ì4(€€€€€€€€€€€€€€€€€€€€€€€€€€€…µ•Ì¹ÍÑ…ÉÐ¡É•…Ñ•¹¥ ¤¤ì4(€€€€€€€€€€€€€€€€€€€€€€€€€ô4(€€€€€€€€€€€€€€€€€€€€€€€€€É•ÑÕÉ¸É•…Ñ•ì4(€€€€€€€€€€€€€€€€€€€€€€€ô¤¤4(€€€€€€€€¹Ý¡•¹½µÁ±•Ñ•Íå¹Œ 4(€€€€€€€€€€€€¡É•…Ñ•°™…¥±ÕÉ”¤€´ø4(€€€€€€€€€€€€€€€Á±…å•È¹Í•¹‘5•ÍÍ…” 4(€€€€€€€€€€€€€€€€€€€™…¥±ÕÉ”€ôô¹Õ±°4(€€€€€€€€€€€€€€€€€€€€€€€€ü5%9$¹‘•Í•É¥…±¥é” ˆñÉ••¸ùA…ÉÑ¥”Ë§¥”•ÐÉ•©½¥¹Ñ”¸ˆ¤4(€€€€€€€€€€€€€€€€€€€€€€€€è5%9$¹‘•Í•É¥…±¥é” ˆñÉ•ù%µÁ½ÍÍ¥‰±”‘”É•©½¥¹‘É”€è€ˆ€¬Í…™”¡™…¥±ÕÉ”¤¤¤°4(€€€€€€€€€€€µ…¥¹Q¡É•…¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”Ù½¥©½¥¹á¥ÍÑ¥¹œ¡A±…å•ÈÁ±…å•È°…µ•%¹ÍÑ…¹•M¹…ÁÍ¡½Ð¥¹ÍÑ…¹”¤ì4(€€€¥¹ÍÑ…¹•Ì4(€€€€€€€€¹©½¥¸¡Á±…å•È°¥¹ÍÑ…¹”¹¥ ¤¤4(€€€€€€€€¹Ý¡•¹½µÁ±•Ñ•Íå¹Œ 4(€€€€€€€€€€€€¡É•ÍÕ±Ð°™…¥±ÕÉ”¤€´øì4(€€€€€€€€€€€€€¥˜€¡™…¥±ÕÉ”€„ô¹Õ±°ñðÉ•ÍÕ±Ð€„ôA±…å•É%¹ÍÑ…¹•I•ÍÕ±Ð¹MUML¤ì4(€€€€€€€€€€€€€€€Á±…å•È¹Í•¹‘5•ÍÍ…” 4(€€€€€€€€€€€€€€€€€€€5%9$¹‘•Í•É¥…±¥é” 4(€€€€€€€€€€€€€€€€€€€€€€€€ˆñÉ•ù½¹¹•á¥½¸É•™ÕÏ¥”€è€ˆ€¬€¡™…¥±ÕÉ”€ôô¹Õ±°€üÉ•ÍÕ±Ð€èÍ…™”¡™…¥±ÕÉ”¤¤¤¤ì4(€€€€€€€€€€€€€ô•±Í”ì4(€€€€€€€€€€€€€€€¥˜€¡…µ•Ì¹Í¹…ÁÍ¡½Ð¡¥¹ÍÑ…¹”¹¥ ¤¤¹¥ÍAÉ•Í•¹Ð ¤¤ì4(€€€€€€€€€€€€€€€€€…µ•Ì¹©½¥¹•¡¥¹ÍÑ…¹”¹¥ ¤°Á±…å•È¹•ÑU¹¥ÅÕ•% ¤¤ì4(€€€€€€€€€€€€€€€€€É•ÑÕÉ¸ì4(€€€€€€€€€€€€€€€ô4(€€€€€€€€€€€€€€€¥¹ÐÁ±…å•ÉÌ€ô4(€€€€€€€€€€€€€€€€€€€¥¹ÍÑ…¹•Ì¹…Ñ¥Ù•%¹ÍÑ…¹•Ì ¤¹ÍÑÉ•…´ ¤4(€€€€€€€€€€€€€€€€€€€€€€€€¹™¥±Ñ•È¡Ù…±Õ”€´øÙ…±Õ”¹¥ ¤¹•ÅÕ…±Ì¡¥¹ÍÑ…¹”¹¥ ¤¤¤4(€€€€€€€€€€€€€€€€€€€€€€€€¹µ…ÁQ½%¹Ð¡Ù…±Õ”€´øÙ…±Õ”¹Á±…å•ÉÌ ¤¹Í¥é” ¤¤4(€€€€€€€€€€€€€€€€€€€€€€€€¹™¥¹‘¥ÉÍÐ ¤4(€€€€€€€€€€€€€€€€€€€€€€€€¹½É±Í” À¤ì4(€€€€€€€€€€€€€€€¥¹Ðµ¥¹¥µÕ´€ô4(€€€€€€€€€€€€€€€€€€€ÁÕ‰±¥…Ñ¥½¹Ì4(€€€€€€€€€€€€€€€€€€€€€€€€¹ÁÕ‰±¥Í¡•‘•™¥¹¥Ñ¥½¸¡¥¹ÍÑ…¹”¹µ…Á% ¤¤4(€€€€€€€€€€€€€€€€€€€€€€€€¹µ…À¡5…Á•™¥¹¥Ñ¥½¸èéµ¥¹¥µÕµA±…å•ÉÌ¤4(€€€€€€€€€€€€€€€€€€€€€€€€¹½É±Í” Ä¤ì4(€€€€€€€€€€€€€€€¥˜€¡Á±…å•ÉÌ€øôµ¥¹¥µÕ´¤ì4(€€€€€€€€€€€€€€€€€…µ•Ì¹ÍÑ…ÉÐ¡¥¹ÍÑ…¹”¹¥ ¤¤ì4(€€€€€€€€€€€€€€€ô•±Í”ì4(€€€€€€€€€€€€€€€€€Á±…å•È¹Í•¹‘5•ÍÍ…” 4(€€€€€€€€€€€€€€€€€€€€€5%9$¹‘•Í•É¥…±¥é” 4(€€€€€€€€€€€€€€€€€€€€€€€€€€ˆñå•±±½Üù¸…ÑÑ•¹Ñ”‘”©½Õ•ÕÉÌ€è€ñÝ¡¥Ñ”øˆ€¬Á±…å•ÉÌ€¬€ˆ¼ˆ€¬µ¥¹¥µÕ´¤¤ì4(€€€€€€€€€€€€€€€ô4(€€€€€€€€€€€€€ô4(€€€€€€€€€€€ô°4(€€€€€€€€€€€µ…¥¹Q¡É•…¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”1¥ÍÐñ…µ•%¹ÍÑ…¹•M¹…ÁÍ¡½ÐøÁÕ‰±¥%¹ÍÑ…¹•Ì¡MÑÉ¥¹œµ…Á%¤ì4(€€€É•ÑÕÉ¸¥¹ÍÑ…¹•Ì¹…Ñ¥Ù•%¹ÍÑ…¹•Ì ¤¹ÍÑÉ•…´ ¤4(€€€€€€€€¹™¥±Ñ•È¡Ù…±Õ”€´øÙ…±Õ”¹µ…Á% ¤¹•ÅÕ…±Ì¡µ…Á%¤€˜˜Ù…±Õ”¹½Ý¹•È ¤¹¥ÍµÁÑä ¤¤4(€€€€€€€€¹Ñ½1¥ÍÐ ¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”Ù½¥¹…Ù¥…Ñ¥½¸¡Õ¥Y¥•ÜÙ¥•Ü°Õ¥A…¥¹…Ñ¥½¸¹A…”ðüøÁ…”°¥¹ÐÑ½Ñ…°°MÑÉ¥¹œ±…‰•°¤ì4(€€€Ù¥•Ü¹½¹™¥ÕÉ• ‰‰…¬ˆ¤ì4(€€€Ù¥•Ü¹½¹™¥ÕÉ• ‰¡½µ”ˆ¤ì4(€€€Ù¥•Ü¹½¹™¥ÕÉ• ‰Í•…É ˆ¤ì4(€€€¥˜€¡Á…”¹¥¹‘•à ¤€ø€À¤ì4(€€€€€Ù¥•Ü¹½¹™¥ÕÉ• ‰ÁÉ•Ù¥½ÕÌˆ¤ì4(€€€ô4(€€€¥˜€¡Á…”¹¥¹‘•à ¤€¬€Ä€ðÁ…”¹Á…•½Õ¹Ð ¤¤ì4(€€€€€Ù¥•Ü¹½¹™¥ÕÉ• ‰¹•áÐˆ¤ì4(€€€ô4(€€€Ù¥•Ü¹¥¹™½Éµ…Ñ¥½¸ 4(€€€€€€€€Ðä°4(€€€€€€€5…Ñ•É¥…°¹AAH°4(€€€€€€€€ˆñÝ¡¥Ñ”ùA…”€ˆ€¬€¡Á…”¹¥¹‘•à ¤€¬€Ä¤€¬€ˆ¼ˆ€¬Á…”¹Á…•½Õ¹Ð ¤°4(€€€€€€€1¥ÍÐ¹½˜ ˆñÉ…äøˆ€¬Ñ½Ñ…°€¬€ˆ€ˆ€¬±…‰•°¤¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”ÍÑ…Ñ¥ŒMÑÉ¥¹œÍ•±•Ñ•¡Õ¥±¥­½¹Ñ•áÐ½¹Ñ•áÐ¤ì4(€€€É•ÑÕÉ¸½¹Ñ•áÐ¹Í•ÍÍ¥½¸ ¤¹ÕÉÉ•¹Ñ½¹Ñ•áÐ ¤¹Ù…±Õ” ‰µ…Àˆ°MÑÉ¥¹œ¹±…ÍÌ¤¹½É±Í” ˆˆ¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”ÍÑ…Ñ¥Œ5…Ñ•É¥…°µ…Ñ•É¥…°¡MÑÉ¥¹œ¹…µ”¤ì4(€€€5…Ñ•É¥…°µ…Ñ•É¥…°€ô5…Ñ•É¥…°¹µ…Ñ¡5…Ñ•É¥…°¡¹…µ”¤ì4(€€€É•ÑÕÉ¸µ…Ñ•É¥…°€ôô¹Õ±°ñð€…µ…Ñ•É¥…°¹¥Í%Ñ•´ ¤€ü5…Ñ•É¥…°¹%11}5@€èµ…Ñ•É¥…°ì4(€ô4(4(€ÁÉ¥Ù…Ñ”ÍÑ…Ñ¥Œ5…Ñ•É¥…°ÍÑ…ÑÕÍ5…Ñ•É¥…°¡5…ÁMÑ…ÑÕÌÍÑ…ÑÕÌ¤ì4(€€€É•ÑÕÉ¸ÍÝ¥Ñ €¡ÍÑ…ÑÕÌ¤ì4(€€€€€…Í”AU	1%M!€´ø5…Ñ•É¥…°¹1%5}=9IQì4(€€€€€…Í”%9Y1%€´ø5…Ñ•É¥…°¹I}=9IQì4(€€€€€…Í”QMQ%9€´ø5…Ñ•É¥…°¹9I}eì4(€€€€€…Í”5%9Q99€´ø5…Ñ•É¥…°¹=I9}=9IQì4(€€€€€…Í”I!%Y€´ø5…Ñ•É¥…°¹Ie}=9IQì4(€€€€€‘•™…Õ±Ð€´ø5…Ñ•É¥…°¹%11}5@ì4(€€€ôì4(€ô4(4(€ÁÉ¥Ù…Ñ”ÍÑ…Ñ¥ŒMÑÉ¥¹œ•µÁÑä¡MÑÉ¥¹œÙ…±Õ”°MÑÉ¥¹œ™…±±‰…¬¤ì4(€€€É•ÑÕÉ¸Ù…±Õ”¹¥Í	±…¹¬ ¤€ü™…±±‰…¬€èÙ…±Õ”ì4(€ô4(4(€ÁÉ¥Ù…Ñ”MÑÉ¥¹œÁ±…å•ÉMÑ…ÑÕÌ¡1¥ÍÐñ…µ•%¹ÍÑ…¹•M¹…ÁÍ¡½Ðø¥¹ÍÑ…¹•Ì¤ì4(€€€¥˜€¡¥¹ÍÑ…¹•Ì¹¥ÍµÁÑä ¤¤ì4(€€€€€É•ÑÕÉ¸€‰¥ÍÁ½¹¥‰±”ˆì4(€€€ô4(€€€¥˜€¡¥¹ÍÑ…¹•Ì¹ÍÑÉ•…´ ¤¹…±±5…Ñ ¡Ù…±Õ”€´øÙ…±Õ”¹Á±…å•ÉÌ ¤¹Í¥é” ¤€øôÙ…±Õ”¹µ…á¥µÕµA±…å•ÉÌ ¤¤¤ì4(€€€€€É•ÑÕÉ¸€‰½µÁ³¡Ñ”ˆì4(€€€ô4(€€€¥˜€¡¥¹ÍÑ…¹•Ì¹ÍÑÉ•…´ ¤4(€€€€€€€€¹…¹å5…Ñ  4(€€€€€€€€€€€Ù…±Õ”€´ø4(€€€€€€€€€€€€€€€Ù…±Õ”¹ÍÑ…Ñ” ¤€ôô…µ•%¹ÍÑ…¹•MÑ…Ñ”¹IU99%94(€€€€€€€€€€€€€€€€€€€ñð…µ•Ì¹Í¹…ÁÍ¡½Ð¡Ù…±Õ”¹¥ ¤¤¹¥ÍAÉ•Í•¹Ð ¤¤¤ì4(€€€€€É•ÑÕÉ¸€‰A…ÉÑ¥”•¸½ÕÉÌˆì4(€€€ô4(€€€¥˜€¡¥¹ÍÑ…¹•Ì¹ÍÑÉ•…´ ¤¹…¹å5…Ñ ¡Ù…±Õ”€´ø€…Ù…±Õ”¹Á±…å•ÉÌ ¤¹¥ÍµÁÑä ¤¤¤ì4(€€€€€É•ÑÕÉ¸€‰¸…ÑÑ•¹Ñ”‘”©½Õ•ÕÉÌˆì4(€€€ô4(€€€É•ÑÕÉ¸€‰¥ÍÁ½¹¥‰±”ˆì4(€ô4(4(€ÁÉ¥Ù…Ñ”1¥ÍÐñY…±¥‘…Ñ¥½¹%Ñ•´øÙ…±¥‘…Ñ¥½¹%Ñ•µÌ¡5…Á•™¥¹¥Ñ¥½¸µ…À¤ì4(€€€Ù…ÈÉ•Á½ÉÐ€ôÙ…±¥‘…Ñ½È¹Ù…±¥‘…Ñ”¡µ…À¤ì4(€€€©…Ù„¹ÕÑ¥°¹ÉÉ…å1¥ÍÐñY…±¥‘…Ñ¥½¹%Ñ•´øÉ•ÍÕ±Ð€ô¹•Ü©…Ù„¹ÕÑ¥°¹ÉÉ…å1¥ÍÐðø ¤ì4(€€€É•Á½ÉÐ4(€€€€€€€€¹•ÉÉ½ÉÌ ¤4(€€€€€€€€¹™½É…  4(€€€€€€€€€€€µ•ÍÍ…”€´ø4(€€€€€€€€€€€€€€€É•ÍÕ±Ð¹…‘ 4(€€€€€€€€€€€€€€€€€€€¹•ÜY…±¥‘…Ñ¥½¹%Ñ•´ 4(€€€€€€€€€€€€€€€€€€€€€€€€ˆñÉ•ùÉÉ•ÕÈ€è€ˆ°4(€€€€€€€€€€€€€€€€€€€€€€€€‰	1=EU9Qˆ°4(€€€€€€€€€€€€€€€€€€€€€€€µ•ÍÍ…”°4(€€€€€€€€€€€€€€€€€€€€€€€€‰½ÉÉ¥•È•Ðƒ¥³¥µ•¹ÐÁÕ¥ÌÉ•±…¹•È±„Ù…±¥‘…Ñ¥½¸¸ˆ°4(€€€€€€€€€€€€€€€€€€€€€€€±½…Ñ”¡µ…À°µ•ÍÍ…”¤°4(€€€€€€€€€€€€€€€€€€€€€€€5…Ñ•É¥…°¹I}=9IQ¤¤¤ì4(€€€É•Á½ÉÐ4(€€€€€€€€¹Ý…É¹¥¹Ì ¤4(€€€€€€€€¹™½É…  4(€€€€€€€€€€€µ•ÍÍ…”€´ø4(€€€€€€€€€€€€€€€É•ÍÕ±Ð¹…‘ 4(€€€€€€€€€€€€€€€€€€€¹•ÜY…±¥‘…Ñ¥½¹%Ñ•´ 4(€€€€€€€€€€€€€€€€€€€€€€€€ˆñ½±ùÙ•ÉÑ¥ÍÍ•µ•¹Ð€è€ˆ°4(€€€€€€€€€€€€€€€€€€€€€€€€‰YIQ%MM59Pˆ°4(€€€€€€€€€€€€€€€€€€€€€€€µ•ÍÍ…”°4(€€€€€€€€€€€€€€€€€€€€€€€€‰[¥É¥™¥•È•Ðƒ¥³¥µ•¹Ð…Ù…¹Ð±„ÁÕ‰±¥…Ñ¥½¸¸ˆ°4(€€€€€€€€€€€€€€€€€€€€€€€±½…Ñ”¡µ…À°µ•ÍÍ…”¤°4(€€€€€€€€€€€€€€€€€€€€€€€5…Ñ•É¥…°¹=I9}=9IQ¤¤¤ì4(€€€É•Á½ÉÐ4(€€€€€€€€¹…‘Ù¥” ¤4(€€€€€€€€¹™½É…  4(€€€€€€€€€€€µ•ÍÍ…”€´ø4(€€€€€€€€€€€€€€€É•ÍÕ±Ð¹…‘ 4(€€€€€€€€€€€€€€€€€€€¹•ÜY…±¥‘…Ñ¥½¹%Ñ•´ 4(€€€€€€€€€€€€€€€€€€€€€€€€ˆñ…ÅÕ„ù%¹™½Éµ…Ñ¥½¸€è€ˆ°4(€€€€€€€€€€€€€€€€€€€€€€€€‰%9=I5Q%=8ˆ°4(€€€€€€€€€€€€€€€€€€€€€€€µ•ÍÍ…”°4(€€€€€€€€€€€€€€€€€€€€€€€€‰½µÁ³¥Ñ•È±„½¹™¥ÕÉ…Ñ¥½¸É•½µµ…¹“¥”¸ˆ°4(€€€€€€€€€€€€€€€€€€€€€€€±½…Ñ”¡µ…À°µ•ÍÍ…”¤°4(€€€€€€€€€€€€€€€€€€€€€€€5…Ñ•É¥…°¹1%!Q}	1U}=9IQ¤¤¤ì4(€€€¥˜€¡É•ÍÕ±Ð¹¥ÍµÁÑä ¤¤ì4(€€€€€É•ÍÕ±Ð¹…‘ 4(€€€€€€€€€¹•ÜY…±¥‘…Ñ¥½¹%Ñ•´ 4(€€€€€€€€€€€€€€ˆñÉ••¸ùY…±¥‘…Ñ¥½¸€è€ˆ°4(€€€€€€€€€€€€€€‰MU!Lˆ°4(€€€€€€€€€€€€€€‰ÕÕ¸ÁÉ½‰³¡µ”“¥Ñ•Ó¤ˆ°4(€€€€€€€€€€€€€€‰1„µ…ÀÁ•ÕÐƒ©ÑÉ”Ñ•ÍÓ¥”½ÔÁÕ‰±§¥”¸ˆ°4(€€€€€€€€€€€€€=ÁÑ¥½¹…°¹•µÁÑä ¤°4(€€€€€€€€€€€€€5…Ñ•É¥…°¹1%5}=9IQ¤¤ì4(€€€ô4(€€€É•ÑÕÉ¸1¥ÍÐ¹½Áå=˜¡É•ÍÕ±Ð¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”ÍÑ…Ñ¥Œ=ÁÑ¥½¹…°ñ5…ÁA½¥¹Ðø±½…Ñ”¡5…Á•™¥¹¥Ñ¥½¸µ…À°MÑÉ¥¹œµ•ÍÍ…”¤ì4(€€€¥¹ÐÍ•Á…É…Ñ½È€ôµ•ÍÍ…”¹¥¹‘•á=˜ œèœ¤ì4(€€€¥˜€¡Í•Á…É…Ñ½È€ð€ÀñðÍ•Á…É…Ñ½È€¬€Ä€øôµ•ÍÍ…”¹±•¹Ñ  ¤¤ì4(€€€€€É•ÑÕÉ¸=ÁÑ¥½¹…°¹•µÁÑä ¤ì4(€€€ô4(€€€MÑÉ¥¹œ¥€ôµ•ÍÍ…”¹ÍÕ‰ÍÑÉ¥¹œ¡Í•Á…É…Ñ½È€¬€Ä¤¹ÍÑÉ¥À ¤ì4(€€€¥˜€¡µ…À¹‘½½ÉÌ ¤¹½¹Ñ…¥¹Í-•ä¡¥¤¤ì4(€€€€€É•ÑÕÉ¸=ÁÑ¥½¹…°¹½˜¡µ…À¹‘½½ÉÌ ¤¹•Ð¡¥¤¹Á½Í¥Ñ¥½¸ ¤¤ì4(€€€ô4(€€€¥˜€¡µ…À¹é½µ‰¥•MÁ…Ý¹Ì ¤¹½¹Ñ…¥¹Í-•ä¡¥¤¤ì4(€€€€€É•ÑÕÉ¸=ÁÑ¥½¹…°¹½˜¡µ…À¹é½µ‰¥•MÁ…Ý¹Ì ¤¹•Ð¡¥¤¹Á½Í¥Ñ¥½¸ ¤¤ì4(€€€ô4(€€€¥˜€¡µ…À¹½‰©•ÑÌ ¤¹½¹Ñ…¥¹Í-•ä¡¥¤¤ì4(€€€€€É•ÑÕÉ¸=ÁÑ¥½¹…°¹½˜¡µ…À¹½‰©•ÑÌ ¤¹•Ð¡¥¤¹Á½Í¥Ñ¥½¸ ¤¤ì4(€€€ô4(€€€¥˜€¡µ…À¹é½¹•Ì ¤¹½¹Ñ…¥¹Í-•ä¡¥¤¤ì4(€€€€€É•ÑÕÉ¸=ÁÑ¥½¹…°¹½˜¡µ…À¹é½¹•Ì ¤¹•Ð¡¥¤¹…¹¡½È ¤¤ì4(€€€ô4(€€€É•ÑÕÉ¸=ÁÑ¥½¹…°¹•µÁÑä ¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”ÍÑ…Ñ¥ŒÙ½¥Ñ•±•Á½ÉÐ¡A±…å•ÈÁ±…å•È°5…ÁA½¥¹ÐÁ½¥¹Ð¤ì4(€€€]½É±Ý½É±€ô½Éœ¹‰Õ­­¥Ð¹	Õ­­¥Ð¹•Ñ]½É±¡Á½¥¹Ð¹Ý½É± ¤¤ì4(€€€¥˜€¡Ý½É±€ôô¹Õ±°¤ì4(€€€€€Á±…å•È¹Í•¹‘5•ÍÍ…”¡5%9$¹‘•Í•É¥…±¥é” ˆñÉ•ù1”µ½¹‘”…ÍÍ½§¤¸•ÍÐÁ…Ì¡…ÉŸ¤¸ˆ¤¤ì4(€€€€€É•ÑÕÉ¸ì4(€€€ô4(€€€Á±…å•È¹Ñ•±•Á½ÉÐ 4(€€€€€€€¹•Ü½Éœ¹‰Õ­­¥Ð¹1½…Ñ¥½¸ 4(€€€€€€€€€€€Ý½É±°Á½¥¹Ð¹à ¤°Á½¥¹Ð¹ä ¤°Á½¥¹Ð¹è ¤°Á½¥¹Ð¹å…Ü ¤°Á½¥¹Ð¹Á¥Ñ  ¤¤¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”ÍÑ…Ñ¥ŒMÑÉ¥¹œÍ…™”¡Q¡É½Ý…‰±”™…¥±ÕÉ”¤ì4(€€€Q¡É½Ý…‰±”…ÕÍ”€ô4(€€€€€€€™…¥±ÕÉ”¥¹ÍÑ…¹•½˜½µÁ±•Ñ¥½¹á•ÁÑ¥½¸€˜˜™…¥±ÕÉ”¹•Ñ…ÕÍ” ¤€„ô¹Õ±°4(€€€€€€€€€€€€ü™…¥±ÕÉ”¹•Ñ…ÕÍ” ¤4(€€€€€€€€€€€€è™…¥±ÕÉ”ì4(€€€É•ÑÕÉ¸…ÕÍ”¹•Ñ5•ÍÍ…” ¤€ôô¹Õ±°€ü…ÕÍ”¹•Ñ±…ÍÌ ¤¹•ÑM¥µÁ±•9…µ” ¤€è…ÕÍ”¹•Ñ5•ÍÍ…” ¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”ÍÑ…Ñ¥Œ]½É±±½…‘‘¥Ñ¥¹]½É±¡5…Á•™¥¹¥Ñ¥½¸‘•™¥¹¥Ñ¥½¸¤ì4(€€€]½É±±½…‘•€ô½Éœ¹‰Õ­­¥Ð¹	Õ­­¥Ð¹•Ñ]½É±¡‘•™¥¹¥Ñ¥½¸¹Ý½É± ¤¤ì4(€€€¥˜€¡±½…‘•€„ô¹Õ±°¤ì4(€€€€€É•ÑÕÉ¸±½…‘•ì4(€€€ô4(€€€©…Ù„¹¹¥¼¹™¥±”¹A…Ñ É½½Ð€ô4(€€€€€€€½Éœ¹‰Õ­­¥Ð¹	Õ­­¥Ð¹•Ñ]½É±‘½¹Ñ…¥¹•È ¤¹Ñ½A…Ñ  ¤¹Ñ½‰Í½±ÕÑ•A…Ñ  ¤¹¹½Éµ…±¥é” ¤ì4(€€€©…Ù„¹¹¥¼¹™¥±”¹A…Ñ …¹‘¥‘…Ñ”€ôÉ½½Ð¹É•Í½±Ù”¡‘•™¥¹¥Ñ¥½¸¹Ý½É± ¤¤¹Ñ½‰Í½±ÕÑ•A…Ñ  ¤¹¹½Éµ…±¥é” ¤ì4(€€€¥˜€ ……¹‘¥‘…Ñ”¹ÍÑ…ÉÑÍ]¥Ñ ¡É½½Ð¤ñð€…©…Ù„¹¹¥¼¹™¥±”¹¥±•Ì¹¥Í¥É•Ñ½Éä¡…¹‘¥‘…Ñ”¤¤ì4(€€€€€É•ÑÕÉ¸¹Õ±°ì4(€€€ô4(€€€É•ÑÕÉ¸½Éœ¹‰Õ­­¥Ð¹	Õ­­¥Ð¹É•…Ñ•]½É±¡¹•Ü]½É±‘É•…Ñ½È¡‘•™¥¹¥Ñ¥½¸¹Ý½É± ¤¤¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”É•½ÉY…±¥‘…Ñ¥½¹%Ñ•´ 4(€€€€€MÑÉ¥¹œÑ¥Ñ±”°4(€€€€€MÑÉ¥¹œÑåÁ”°4(€€€€€MÑÉ¥¹œµ•ÍÍ…”°4(€€€€€MÑÉ¥¹œÍ½±ÕÑ¥½¸°4(€€€€€=ÁÑ¥½¹…°ñ5…ÁA½¥¹ÐøÁ½Í¥Ñ¥½¸°4(€€€€€5…Ñ•É¥…°µ…Ñ•É¥…°¤íô4)ô4