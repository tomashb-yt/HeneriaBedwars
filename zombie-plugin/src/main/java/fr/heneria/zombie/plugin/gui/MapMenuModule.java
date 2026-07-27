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
              "<yellow>Cliquez pour restã­;¶‰ËkºwµçHRS’K™\Ù\šX[^™JBˆ™YÛÛ™šYİ\˜][Û‹Ü]ÛœËØš™]Ë™\œÚ[ÛœÈ][Û™\ÈÜÜğêY0ê\ÈÙ\›Ûİ\š[pê\ËˆŠKBˆÛØÚËš[œİ[

Kœ\ÔÙXÛÛ™ÊJKBˆÛÛ^OˆÃBˆİš[™Èİ\œ™[›ØÚÙ\ˆH[][Û›ØÚÙ\ŠX\Y
NÃBˆYˆ
İ\œ™[›ØÚÙ\ˆOH[
HÃBˆÛÛ^œÙ\ÜÚ[ÛŠ
K˜ÛÛ™š\›X][ÛŠ[
NÃBˆ^Y\‹œÙ[™Y\ÜØYÙJBˆRS’K™\Ù\šX[^™J™Y”İ\™\ÜÚ[Ûˆ™Y\ğêYHˆˆ
Èİ\œ™[›ØÚÙ\ŠJNÃBˆİZ\Ë˜˜XÚÊ^Y\ŠNÃBˆ™]\›ÃBˆCBˆÛÜ›X›XØ][ÛœÃBˆœ™\\™Q[][ÛŠYš[š][ÛŠCBˆ[ÛÛ\ÜÙJYÛ›Ü™YOˆX›XØ][ÛœË™[]JX\Y

HOˆY]ÜœË™[]JX\Y
JJCBˆ[”[ŠBˆ

HOˆÃBˆ[\]\Ëœ™Yœ™\ÚÛİ[

NÃBˆJCBˆÚ[ÛÛ\]P\Ş[˜ÊBˆ
[]Y˜Z[\™JHOˆÃBˆÛÛ^œÙ\ÜÚ[ÛŠ
K˜ÛÛ™š\›X][ÛŠ[
NÃBˆYˆ
˜Z[\™HOH[
HÃBˆ^Y\‹œÙ[™Y\ÜØYÙJBˆRS’K™\Ù\šX[^™JBˆÜ™Y[“X\İ\š[pêYH]™XÈØHÛÛ™šYİ\˜][Û‹Ù\È™\œÚ[ÛœÈ]Ù\È[Û™\ËˆŠJNÃBˆİZ\Ë›Ü[Š^Y\‹™]ÈİZRY
˜YZ[‹[X\ÈŠJNÃBˆH[ÙHÃBˆ^Y\‹œÙ[™Y\ÜØYÙJBˆRS’K™\Ù\šX[^™J™Y”İ\™\ÜÚ[Ûˆ[\ÜÜÚX›Hˆˆ
ÈØY™J˜Z[\™JJJNÃBˆİZ\Ë˜˜XÚÊ^Y\ŠNÃBˆCBˆKBˆXZ[•™XY
NÃBˆJJNÃBˆCBƒBˆš]˜]Hİš[™È[][Û›ØÚÙ\Šİš[™ÈX\Y
HÃBˆYˆ
[œİ[˜Ù\Ë˜Xİ]™R[œİ[˜Ù\Ê
Kœİ™X[J
K˜[SX]Ú
˜[YHOˆ˜[YK›X\Y

K™\]X[ÊX\Y
JJHÃBˆ™]\›ˆ[™H[œİ[˜ÙH][\ÙH[˜ÛÜ™HÙ]HX\ˆÃBˆCBˆYˆ
Y]ÜœË™Y]Ü“ÙŠX\Y
Kš\Ô™\Ù[

JHÃBˆ™]\›ˆ›HX\\İ[˜ÛÜ™Hİ]™\H[œÈ	ğêY]]\‹ˆÃBˆCBˆ™]\›ˆ[ÃBˆCBƒBˆš]˜]H›ÚY\İ
^Y\ˆ^Y\‹İš[™ÈX\Y
HÃBˆX\Yš[š][ÛˆX\HY]ÜœËœ™YÚ\İJ
K™š[™
X\Y
K›Ü‘[ÙJ[
NÃBˆYˆ
X\OH[]˜[Y]Ü‹˜[Y]JX\
K˜[Y

JHÃBˆ^Y\‹œÙ[™Y\ÜØYÙJRS’K™\Ù\šX[^™J™Y“HX\Ú]0ê™H˜[YH]˜[H\İˆŠJNÃBˆ™]\›ÃBˆCBˆYˆ
ØYY][™ÕÛÜ›
X\
HOH[
HÃBˆ^Y\‹œÙ[™Y\ÜØYÙJRS’K™\Ù\šX[^™J™Y“H[Û™H	ğêY][Ûˆ™H]]\È0ê™HÚ\™ğêKˆŠJNÃBˆ™]\›ÃBˆCBˆÜ[Û˜[URQˆY]ÜˆHY]ÜœË™Y]Ü“ÙŠX\Y
NÃBˆYˆ
Y]Ü‹š\Ô™\Ù[

H	‰ˆYY]Ü‹›Ü‘[ÙU›İÊ
K™\]X[Ê^Y\‹™Ù][š\]YRY

JJHÃBˆ^Y\‹œÙ[™Y\ÜØYÙJRS’K™\Ù\šX[^™J™Y“HX\\İ[˜ÛÜ™H[ÙYšpêYH\ˆ[ˆ]]™HYZ[‹ˆŠJNÃBˆ™]\›ÃBˆCBˆ^Y\‹˜ÛÜÙR[™[ÜJ
NÃBˆÛÛ\]X›Q]\™O›ÚYˆÛÜÙYY]ÜÃBˆYˆ
Y]ÜœÃBˆœÙ\ÜÚ[ÛŠ^Y\‹™Ù][š\]YRY

JCBˆ™š[\ŠÙ\ÜÚ[ÛˆOˆÙ\ÜÚ[Û‹™Yš[š][ÛŠ
KšY

K™\]X[ÊX\Y
JCBˆš\Ô™\Ù[

JHÃBˆš\İX[^˜][ÛœË˜ÛX\‘Y]ÜŠ^Y\‹™Ù][š\]YRY

JNÃBˆY]Ü’][\Ëœ™[[İ™J^Y\ŠNÃBˆÛÜÙYY]ÜˆHY]ÜœË›X]™J^Y\‹™Ù][š\]YRY

JK[\JYÛ›Ü™YOˆ[
NÃBˆ[œİ[˜Ù\Ë›X]™J^Y\ŠNÃBˆH[ÙHÃBˆÛÜÙYY]ÜˆHÛÛ\]X›Q]\™K˜ÛÛ\]Y]\™J[
NÃBˆCBˆÛÜÙYY]ÜƒBˆ[ÛÛ\ÜÙP\Ş[˜ÊYÛ›Ü™YOˆÛÜ›X›XØ][ÛœË\]U[\]JX\
KXZ[•™XY
CBˆ[ÛÛ\ÜÙJBˆYÛ›Ü™YOƒBˆ[œİ[˜Ù\Ë˜Ü™X]JX\YÜ[Û˜[›ÙŠ^Y\‹™Ù][š\]YRY

JKX\›X^[][T^Y\œÊ
JJCBˆ[ÛÛ\ÜÙJBˆÜ™X]YOƒBˆ[œİ[˜Ù\ÃBˆš›Ú[Š^Y\‹Ü™X]YšY

JCBˆ[\JBˆ™\İ[OˆÃBˆYˆ
™\İ[OH^Y\’[œİ[˜ÙT™\İ[”ÕPĞÑTÔÊHÃBˆ›İÈ™]È[YØ[İ]Q^Ù\[ÛŠ‘[°êYH™Y\ğêYHˆˆ
È™\İ[
NÃBˆCBˆØ[Y\Ëœİ\
Ü™X]YšY

JNÃBˆ™]\›ˆÜ™X]YÃBˆJJCBˆÚ[ÛÛ\]P\Ş[˜ÊBˆ
Ü™X]Y˜Z[\™JHOƒBˆ^Y\‹œÙ[™Y\ÜØYÙJBˆ˜Z[\™HOH[BˆÈRS’K™\Ù\šX[^™JÜ™Y[”\YHH\İ0ê[X\œ°êYKˆŠCBˆˆRS’K™\Ù\šX[^™J™Y•\İ[\ÜÜÚX›Hˆˆ
ÈØY™J˜Z[\™JJJKBˆXZ[•™XY
NÃBˆCBƒBˆš]˜]H›ÚY›Ú[”X›\ÚY
^Y\ˆ^Y\‹İš[™ÈX\Y
HÃBˆYˆ
X›XØ][ÛœËœX›\ÚYYš[š][ÛŠX\Y
Kš\Ñ[\J
JHÃBˆ^Y\‹œÙ[™Y\ÜØYÙJRS’K™\Ù\šX[^™J™YÙ]HX\‰Ù\İ\ÈX›pêYKˆŠJNÃBˆ™]\›ÃBˆCBˆ^Y\‹˜ÛÜÙR[™[ÜJ
NÃBˆÜ[Û˜[Ø[YR[œİ[˜ÙTÛ˜\Úİˆ]˜Z[X›HCBˆX›XÒ[œİ[˜Ù\ÊX\Y
Kœİ™X[J
CBˆ™š[\ŠBˆ˜[YHOƒBˆ˜[YKœ^Y\œÊ
KœÚ^™J
H˜[YK›X^[][T^Y\œÊ
CBˆ	‰ˆ
˜[YKœİ]J
HOHØ[YR[œİ[˜ÙTİ]K•ĞRUS‘ÃBˆ˜[YKœİ]J
HOHØ[YR[œİ[˜ÙTİ]K”ÕT•S‘ÃBˆ˜[YKœİ]J
HOHØ[YR[œİ[˜ÙTİ]K”•S“’S‘ÊJCBˆ™š[™š\œİ

NÃBˆYˆ
]˜Z[X›Kš\Ô™\Ù[

JHÃBˆ›Ú[‘^\İ[™Ê^Y\‹]˜Z[X›K›Ü‘[ÙU›İÊ
JNÃBˆ™]\›ÃBˆCBˆ^Y\‹œÙ[™Y\ÜØYÙJRS’K™\Ù\šX[^™JY[İÏÜ°êX][Ûˆ]]ÛX]\]YH	İ[™H\YK‹‹ˆŠJNÃBˆ[œİ[˜Ù\ÃBˆ˜Ü™X]JBˆX\YBˆÜ[Û˜[™[\J
KBˆX›XØ][ÛœËœX›\ÚYYš[š][ÛŠX\Y
K›Ü‘[ÙU›İÊ
K›X^[][T^Y\œÊ
JCBˆ[ÛÛ\ÜÙJBˆÜ™X]YOƒBˆ[œİ[˜Ù\ÃBˆš›Ú[Š^Y\‹Ü™X]YšY

JCBˆ[\JBˆ™\İ[OˆÃBˆYˆ
™\İ[OH^Y\’[œİ[˜ÙT™\İ[”ÕPĞÑTÔÊHÃBˆ›İÈ™]È[YØ[İ]Q^Ù\[ÛŠ‘[°êYH™Y\ğêYHˆˆ
È™\İ[
NÃBˆCBˆX\Yš[š][ÛˆX›\ÚYCBˆX›XØ][ÛœËœX›\ÚYYš[š][ÛŠX\Y
K›Ü‘[ÙU›İÊ
NÃBˆ[^Y\œÈCBˆ[œİ[˜Ù\Ë˜Xİ]™R[œİ[˜Ù\Ê
Kœİ™X[J
CBˆ™š[\Š˜[YHOˆ˜[YKšY

K™\]X[ÊÜ™X]YšY

JJCBˆ›X\Ò[
˜[YHOˆ˜[YKœ^Y\œÊ
KœÚ^™J
JCBˆ™š[™š\œİ

CBˆ›Ü‘[ÙJ
NÃBˆYˆ
^Y\œÈHX›\ÚY›Z[š[][T^Y\œÊ
JHÃBˆØ[Y\Ëœİ\
Ü™X]YšY

JNÃBˆCBˆ™]\›ˆÜ™X]YÃBˆJJCBˆÚ[ÛÛ\]P\Ş[˜ÊBˆ
Ü™X]Y˜Z[\™JHOƒBˆ^Y\‹œÙ[™Y\ÜØYÙJBˆ˜Z[\™HOH[BˆÈRS’K™\Ù\šX[^™JÜ™Y[”\YHÜ°êpêYH]™Z›Ú[KˆŠCBˆˆRS’K™\Ù\šX[^™J™Y’[\ÜÜÚX›HH™Z›Ú[™™Hˆˆ
ÈØY™J˜Z[\™JJJKBˆXZ[•™XY
NÃBˆCBƒBˆš]˜]H›ÚY›Ú[‘^\İ[™Ê^Y\ˆ^Y\‹Ø[YR[œİ[˜ÙTÛ˜\Úİ[œİ[˜ÙJHÃBˆ[œİ[˜Ù\ÃBˆš›Ú[Š^Y\‹[œİ[˜ÙKšY

JCBˆÚ[ÛÛ\]P\Ş[˜ÊBˆ
™\İ[˜Z[\™JHOˆÃBˆYˆ
˜Z[\™HOH[™\İ[OH^Y\’[œİ[˜ÙT™\İ[”ÕPĞÑTÔÊHÃBˆ^Y\‹œÙ[™Y\ÜØYÙJBˆRS’K™\Ù\šX[^™JBˆ™YÛÛ›™^[Ûˆ™Y\ğêYHˆˆ
È
˜Z[\™HOH[È™\İ[ˆØY™J˜Z[\™JJJJNÃBˆH[ÙHÃBˆYˆ
Ø[Y\ËœÛ˜\Úİ
[œİ[˜ÙKšY

JKš\Ô™\Ù[

JHÃBˆØ[Y\Ëš›Ú[™Y
[œİ[˜ÙKšY

K^Y\‹™Ù][š\]YRY

JNÃBˆ™]\›ÃBˆCBˆ[^Y\œÈCBˆ[œİ[˜Ù\Ë˜Xİ]™R[œİ[˜Ù\Ê
Kœİ™X[J
CBˆ™š[\Š˜[YHOˆ˜[YKšY

K™\]X[Ê[œİ[˜ÙKšY

JJCBˆ›X\Ò[
˜[YHOˆ˜[YKœ^Y\œÊ
KœÚ^™J
JCBˆ™š[™š\œİ

CBˆ›Ü‘[ÙJ
NÃBˆ[Z[š[][HCBˆX›XØ][ÛœÃBˆœX›\ÚYYš[š][ÛŠ[œİ[˜ÙK›X\Y

JCBˆ›X\
X\Yš[š][Û›Z[š[][T^Y\œÊCBˆ›Ü‘[ÙJJNÃBˆYˆ
^Y\œÈHZ[š[][JHÃBˆØ[Y\Ëœİ\
[œİ[˜ÙKšY

JNÃBˆH[ÙHÃBˆ^Y\‹œÙ[™Y\ÜØYÙJBˆRS’K™\Ù\šX[^™JBˆY[İÏ‘[ˆ][HH›İY]\œÈˆÚ]Oˆˆ
È^Y\œÈ
È‹Èˆ
ÈZ[š[][JJNÃBˆCBˆCBˆKBˆXZ[•™XY
NÃBˆCBƒBˆš]˜]H\İØ[YR[œİ[˜ÙTÛ˜\ÚİˆX›XÒ[œİ[˜Ù\Êİš[™ÈX\Y
HÃBˆ™]\›ˆ[œİ[˜Ù\Ë˜Xİ]™R[œİ[˜Ù\Ê
Kœİ™X[J
CBˆ™š[\Š˜[YHOˆ˜[YK›X\Y

K™\]X[ÊX\Y
H	‰ˆ˜[YK›İÛ™\Š
Kš\Ñ[\J
JCBˆÓ\İ

NÃBˆCBƒBˆš]˜]H›ÚY˜]šYØ][ÛŠİZUšY]ÈšY]ËİZTYÚ[˜][Û‹”YÙOÏˆYÙK[İ[İš[™ÈX™[
HÃBˆšY]Ë˜ÛÛ™šYİ\™Y
˜˜XÚÈŠNÃBˆšY]Ë˜ÛÛ™šYİ\™Y
šÛYHŠNÃBˆšY]Ë˜ÛÛ™šYİ\™Y
œÙX\˜ÚŠNÃBˆYˆ
YÙKš[™^

Hˆ
HÃBˆšY]Ë˜ÛÛ™šYİ\™Y
œ™]š[İ\ÈŠNÃBˆCBˆYˆ
YÙKš[™^

H
ÈHYÙKœYÙPÛİ[

JHÃBˆšY]Ë˜ÛÛ™šYİ\™Y
›™^ŠNÃBˆCBˆšY]Ëš[™›Ü›X][ÛŠBˆKBˆX]\šX[”TT‹BˆÚ]O”YÙHˆ
È
YÙKš[™^

H
ÈJH
È‹Èˆ
ÈYÙKœYÙPÛİ[

KBˆ\İ›ÙŠÜ˜^Oˆˆ
Èİ[
Èˆˆ
ÈX™[
JNÃBˆCBƒBˆš]˜]Hİ]XÈİš[™ÈÙ[XİY
İZPÛXÚĞÛÛ^ÛÛ^
HÃBˆ™]\›ˆÛÛ^œÙ\ÜÚ[ÛŠ
K˜İ\œ™[ÛÛ^

K˜[YJ›X\‹İš[™Ë˜Û\ÜÊK›Ü‘[ÙJˆŠNÃBˆCBƒBˆš]˜]Hİ]XÈX]\šX[X]\šX[
İš[™È˜[YJHÃBˆX]\šX[X]\šX[HX]\šX[›X]ÚX]\šX[
˜[YJNÃBˆ™]\›ˆX]\šX[OH[[X]\šX[š\Ò][J
HÈX]\šX[‘’SQÓPTˆX]\šX[ÃBˆCBƒBˆš]˜]Hİ]XÈX]\šX[İ]\ÓX]\šX[
X\İ]\Èİ]\ÊHÃBˆ™]\›ˆİÚ]Ú
İ]\ÊHÃBˆØ\ÙHP“TÒQOˆX]\šX[“SQWĞÓÓÔ‘UNÃBˆØ\ÙHS•SQOˆX]\šX[”‘QĞÓÓÔ‘UNÃBˆØ\ÙHTÕS‘ÈOˆX]\šX[‘S‘T—ÑVQNÃBˆØ\ÙHPRS•SSÑHOˆX]\šX[“ÔS‘ÑWĞÓÓÔ‘UNÃBˆØ\ÙHTÒU‘QOˆX]\šX[‘ÔVWĞÓÓÔ‘UNÃBˆY˜][OˆX]\šX[‘’SQÓPTÃBˆNÃBˆCBƒBˆš]˜]Hİ]XÈİš[™È[\Jİš[™È˜[YKİš[™È˜[˜XÚÊHÃBˆ™]\›ˆ˜[YKš\Ğ›[šÊ
HÈ˜[˜XÚÈˆ˜[YNÃBˆCBƒBˆš]˜]Hİš[™È^Y\”İ]\Ê\İØ[YR[œİ[˜ÙTÛ˜\Úİˆ[œİ[˜Ù\ÊHÃBˆYˆ
[œİ[˜Ù\Ëš\Ñ[\J
JHÃBˆ™]\›ˆ‘\ÜÛšX›HÃBˆCBˆYˆ
[œİ[˜Ù\Ëœİ™X[J
K˜[X]Ú
˜[YHOˆ˜[YKœ^Y\œÊ
KœÚ^™J
HH˜[YK›X^[][T^Y\œÊ
JJHÃBˆ™]\›ˆÛÛ\0êHÃBˆCBˆYˆ
[œİ[˜Ù\Ëœİ™X[J
CBˆ˜[SX]Ú
Bˆ˜[YHOƒBˆ˜[YKœİ]J
HOHØ[YR[œİ[˜ÙTİ]K”•S“’S‘ÃBˆØ[Y\ËœÛ˜\Úİ
˜[YKšY

JKš\Ô™\Ù[

JJHÃBˆ™]\›ˆ”\YH[ˆÛİ\œÈÃBˆCBˆYˆ
[œİ[˜Ù\Ëœİ™X[J
K˜[SX]Ú
˜[YHOˆ]˜[YKœ^Y\œÊ
Kš\Ñ[\J
JJHÃBˆ™]\›ˆ‘[ˆ][HH›İY]\œÈÃBˆCBˆ™]\›ˆ‘\ÜÛšX›HÃBˆCBƒBˆš]˜]H\İ˜[Y][Û’][Oˆ˜[Y][Û’][\ÊX\Yš[š][ÛˆX\
HÃBˆ˜\ˆ™\ÜH˜[Y]Ü‹˜[Y]JX\
NÃBˆ˜]˜K][\œ˜^S\İ˜[Y][Û’][Oˆ™\İ[H™]È˜]˜K][\œ˜^S\İŠ
NÃBˆ™\ÜBˆ™\œ›ÜœÊ
CBˆ™›Ü‘XXÚ
BˆY\ÜØYÙHOƒBˆ™\İ[˜Y
Bˆ™]È˜[Y][Û’][JBˆ™Y‘\œ™]\ˆˆ‹Bˆ“ÔUPS•H‹BˆY\ÜØYÙKBˆÛÜœšYÙ\ˆÙ]0ê[0ê[Y[Z\È™[[˜Ù\ˆH˜[Y][Û‹ˆ‹BˆØØ]JX\Y\ÜØYÙJKBˆX]\šX[”‘QĞÓÓÔ‘UJJJNÃBˆ™\ÜBˆØ\›š[™ÜÊ
CBˆ™›Ü‘XXÚ
BˆY\ÜØYÙHOƒBˆ™\İ[˜Y
Bˆ™]È˜[Y][Û’][JBˆÛÛ]™\\ÜÙ[Y[ˆ‹BˆU‘T•TÔÑSQS•‹BˆY\ÜØYÙKBˆ•°ê\šYšY\ˆÙ]0ê[0ê[Y[]˜[HX›XØ][Û‹ˆ‹BˆØØ]JX\Y\ÜØYÙJKBˆX]\šX[“ÔS‘ÑWĞÓÓÔ‘UJJJNÃBˆ™\ÜBˆ˜YšXÙJ
CBˆ™›Ü‘XXÚ
BˆY\ÜØYÙHOƒBˆ™\İ[˜Y
Bˆ™]È˜[Y][Û’][JBˆ\]XO’[™›Ü›X][Ûˆˆ‹Bˆ’S‘“Ô“PUSÓˆ‹BˆY\ÜØYÙKBˆÛÛ\0ê]\ˆHÛÛ™šYİ\˜][Ûˆ™XÛÛ[X[™0êYKˆ‹BˆØØ]JX\Y\ÜØYÙJKBˆX]\šX[“QÒĞ“QWĞÓÓÔ‘UJJJNÃBˆYˆ
™\İ[š\Ñ[\J
JHÃBˆ™\İ[˜Y
Bˆ™]È˜[Y][Û’][JBˆÜ™Y[•˜[Y][Ûˆˆ‹Bˆ”ÕPĞğâÈ‹Bˆ]Xİ[ˆ›Ø›0êYH0ê]Xİ0êH‹Bˆ“HX\]]0ê™H\İ0êYHİHX›pêYKˆ‹BˆÜ[Û˜[™[\J
KBˆX]\šX[“SQWĞÓÓÔ‘UJJNÃBˆCBˆ™]\›ˆ\İ˜ÛÜSÙŠ™\İ[
NÃBˆCBƒBˆš]˜]Hİ]XÈÜ[Û˜[X\Ú[ˆØØ]JX\Yš[š][ÛˆX\İš[™ÈY\ÜØYÙJHÃBˆ[Ù\\˜]ÜˆHY\ÜØYÙKš[™^ÙŠ	Î‰ÊNÃBˆYˆ
Ù\\˜]ÜˆÙ\\˜]Üˆ
ÈHHY\ÜØYÙK›[™İ

JHÃBˆ™]\›ˆÜ[Û˜[™[\J
NÃBˆCBˆİš[™ÈYHY\ÜØYÙKœİXœİš[™ÊÙ\\˜]Üˆ
ÈJKœİš\

NÃBˆYˆ
X\™ÛÜœÊ
K˜ÛÛZ[œÒÙ^JY
JHÃBˆ™]\›ˆÜ[Û˜[›ÙŠX\™ÛÜœÊ
K™Ù]
Y
KœÜÚ][ÛŠ
JNÃBˆCBˆYˆ
X\›ÛXšYTÜ]ÛœÊ
K˜ÛÛZ[œÒÙ^JY
JHÃBˆ™]\›ˆÜ[Û˜[›ÙŠX\›ÛXšYTÜ]ÛœÊ
K™Ù]
Y
KœÜÚ][ÛŠ
JNÃBˆCBˆYˆ
X\›Øš™XİÊ
K˜ÛÛZ[œÒÙ^JY
JHÃBˆ™]\›ˆÜ[Û˜[›ÙŠX\›Øš™XİÊ
K™Ù]
Y
KœÜÚ][ÛŠ
JNÃBˆCBˆYˆ
X\›Û™\Ê
K˜ÛÛZ[œÒÙ^JY
JHÃBˆ™]\›ˆÜ[Û˜[›ÙŠX\›Û™\Ê
K™Ù]
Y
K˜[˜ÚÜŠ
JNÃBˆCBˆ™]\›ˆÜ[Û˜[™[\J
NÃBˆCBƒBˆš]˜]Hİ]XÈ›ÚY[\Ü
^Y\ˆ^Y\‹X\Ú[Ú[
HÃBˆÛÜ›ÛÜ›HÜ™Ë˜ZÚÚ]ZÚÚ]™Ù]ÛÜ›
Ú[ÛÜ›

JNÃBˆYˆ
ÛÜ›OH[
HÃBˆ^Y\‹œÙ[™Y\ÜØYÙJRS’K™\Ù\šX[^™J™Y“H[Û™H\ÜÛØÚpêH‰Ù\İ\ÈÚ\™ğêKˆŠJNÃBˆ™]\›ÃBˆCBˆ^Y\‹[\Ü
Bˆ™]ÈÜ™Ë˜ZÚÚ]“ØØ][ÛŠBˆÛÜ›Ú[

KÚ[J
KÚ[Š
KÚ[X]Ê
KÚ[œ]Ú

JJNÃBˆCBƒBˆš]˜]Hİ]XÈİš[™ÈØY™J›İØX›H˜Z[\™JHÃBˆ›İØX›HØ]\ÙHCBˆ˜Z[\™H[œİ[˜Ù[ÙˆÛÛ\][Û‘^Ù\[Ûˆ	‰ˆ˜Z[\™K™Ù]Ø]\ÙJ
HOH[BˆÈ˜Z[\™K™Ù]Ø]\ÙJ
CBˆˆ˜Z[\™NÃBˆ™]\›ˆØ]\ÙK™Ù]Y\ÜØYÙJ
HOH[ÈØ]\ÙK™Ù]Û\ÜÊ
K™Ù]Ú[\S˜[YJ
HˆØ]\ÙK™Ù]Y\ÜØYÙJ
NÃBˆCBƒBˆš]˜]Hİ]XÈÛÜ›ØYY][™ÕÛÜ›
X\Yš[š][ÛˆYš[š][ÛŠHÃBˆÛÜ›ØYYHÜ™Ë˜ZÚÚ]ZÚÚ]™Ù]ÛÜ›
Yš[š][Û‹ÛÜ›

JNÃBˆYˆ
ØYYOH[
HÃBˆ™]\›ˆØYYÃBˆCBˆ˜]˜K›š[Ë™š[K”]›ÛİCBˆÜ™Ë˜ZÚÚ]ZÚÚ]™Ù]ÛÜ›ÛÛZ[™\Š
KÔ]

KĞXœÛÛ]T]

K››Ü›X[^™J
NÃBˆ˜]˜K›š[Ë™š[K”]Ø[™Y]HH›Ûİœ™\ÛÛ™JYš[š][Û‹ÛÜ›

JKĞXœÛÛ]T]

K››Ü›X[^™J
NÃBˆYˆ
XØ[™Y]Kœİ\ÕÚ]
›Ûİ
HZ˜]˜K›š[Ë™š[K‘š[\Ëš\Ñ\™XİÜJØ[™Y]JJHÃBˆ™]\›ˆ[ÃBˆCBˆ™]\›ˆÜ™Ë˜ZÚÚ]ZÚÚ]˜Ü™X]UÛÜ›
™]ÈÛÜ›Ü™X]ÜŠYš[š][Û‹ÛÜ›

JJNÃBˆCBƒBˆš]˜]H™XÛÜ™˜[Y][Û’][JBˆİš[™È]KBˆİš[™È\KBˆİš[™ÈY\ÜØYÙKBˆİš[™ÈÛÛ][Û‹BˆÜ[Û˜[X\Ú[ˆÜÚ][Û‹BˆX]\šX[X]\šX[
HßCBŸCB