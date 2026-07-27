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
    actions.register("maps.admin", context -> openAdminMaps(context.player()));
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
              "<gray>Difficulté : <white>" + map.difficulty(),
              "<gray>Mode : <white>" + map.gameMode(),
              "<gray>Joueurs : <white>" + map.minimumPlayers() + "-" + map.maximumPlayers(),
              "<gray>État : <white>" + playerStatus(active),
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
    navigation(view, page, maps.size(), "map(s) publiée(s)");
  }

  private void renderAdminMaps(GuiView view, GuiContext ignored) {
    GuiSession session = guis.session(view.player());
    java.util.Map<String, MapDefinition> configured =
        editors.registry().all().stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    MapDefinition::id, map -> map, (first, ignoredDuplicate) -> first));
    java.util.Set<String> identifiers = new java.util.HashSet<>(templates.knownMapIds());
    identifiers.addAll(configured.keySet());
    List<AdminMapEntry> maps =
        identifiers.stream()
            .map(id -> new AdminMapEntry(id, Optional.ofNullable(configured.get(id))))
            .filter(
                entry ->
                    session.search().isBlank()
                        || entry
                            .id()
                            .toLowerCase(Locale.ROOT)
                            .contains(session.search().toLowerCase(Locale.ROOT))
                        || entry
                            .definition()
                            .map(MapDefinition::displayName)
                            .orElse(entry.id())
                            .toLowerCase(Locale.ROOT)
                            .contains(session.search().toLowerCase(Locale.ROOT)))
            .sorted(Comparator.comparing(AdminMapEntry::id))
            .toList();
    GuiPagination.Page<AdminMapEntry> page =
        GuiPagination.page(maps, session.page(), view.menu().contentSlots().size());
    session.page(page.index());
    for (int index = 0; index < page.items().size(); index++) {
      AdminMapEntry entry = page.items().get(index);
      if (entry.definition().isEmpty()) {
        view.button(
            view.menu().contentSlots().get(index),
            Material.ENDER_PEARL,
            "<aqua>" + entry.id(),
            List.of(
                "<gray>État : <yellow>template importé",
                "<gray>Configuration : <red>non créée",
                "<gray>Dossier : <white>zombie_templates/" + entry.id(),
                "",
                "<green>La visite est déjà disponible.",
                "<yellow>Cliquez pour ouvrir la fiche"),
            "zombies.admin.maps.view",
            context ->
                guis.open(
                    context.player(),
                    new GuiId("admin-map-detail"),
                    GuiContext.of("map", entry.id())),
            null,
            null);
        continue;
      }
      MapDefinition map = entry.definition().orElseThrow();
      MapPublication publication = publications.publication(map.id());
      var report = validator.validate(map);
      view.button(
          view.menu().contentSlots().get(index),
          statusMaterial(publication.status()),
          "<aqua>" + map.displayName(),
          List.of(
              "<gray>ID : <white>" + map.id(),
              "<gray>État : <white>" + publication.status(),
              "<gray>Version active : <white>"
                  + publication.activeVersion().map(value -> "v" + value).orElse("aucune"),
              "<gray>Erreurs : "
                  + (report.errors().isEmpty() ? "<green>0" : "<red>" + report.errors().size()),
              "<gray>Avertissements : <gold>" + report.warnings().size(),
              "<gray>Éditeur : <white>"
                  + editors.editorOf(map.id()).map(UUID::toString).orElse("libre"),
              "",
              "<yellow>Cliquez pour gérer"),
          "zombies.admin.maps.view",
          context ->
              guis.open(
                  context.player(), new GuiId("admin-map-detail"), GuiContext.of("map", map.id())),
          null,
          null);
    }
    navigation(view, page, maps.size(), "template(s) et map(s)");
    view.configured("create");
  }

  private void renderMapDetail(GuiView view, GuiContext context) {
    String id = context.value("map", String.class).orElse("");
    MapDefinition map = editors.registry().find(id).orElse(null);
    if (map == null) {
      if (templates.knownMapIds().contains(id)) {
        view.information(
            4,
            Material.FILLED_MAP,
            "<aqua>" + id,
            List.of(
                "<gray>État : <yellow>template importé",
                "<gray>Configuration : <red>non créée",
                "<gray>Source : <white>zombie_templates/" + id,
                "",
                "<green>Vous pouvez déjà visiter cette map.",
                "<yellow>Aucune partie ne sera créée."));
        view.information(
            19,
            Material.WRITABLE_BOOK,
            "<gold>Configuration à faire plus tard",
            List.of(
                "<gray>La visite ne crée aucun map.yml.",
                "<gray>Spawns, zones et machines restent",
                "<gray>à configurer avant un test."));
        view.configured("visit");
        view.configured("back");
        view.configured("home");
        return;
      }
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
            "<gray>État : <white>" + publication.status(),
            "<gray>Monde de travail : <white>" + map.world(),
            "<gray>Template visitable : "
                + (templates.knownMapIds().contains(id) ? "<green>disponible" : "<red>absent"),
            "<gray>Version publiée : <white>"
                + publication.activeVersion().map(value -> "v" + value).orElse("aucune"),
            "<gray>Validation : "
                + (report.valid()
                    ? "<green>valide"
                    : "<red>" + report.errors().size() + " erreur(s)"),
            "",
            "<gray>Zones : <white>"
                + map.zones().size()
                + " <dark_gray>• <gray>Spawns : <white>"
                + map.zombieSpawns().size()
                + " <dark_gray>• <gray>Objets : <white>"
                + map.objects().size()));
    view.information(
        1,
        Material.COMPASS,
        "<aqua>Étape 1 — Explorer et préparer",
        List.of(
            "<gray>Visitez d'abord le template.",
            "<gray>Modifiez ensuite la copie de travail.",
            "<gray>Terminez par la vérification."));
    view.information(
        19,
        Material.CLOCK,
        "<light_purple>Étape 2 — Tester et publier",
        List.of(
            "<gray>Le test lance une partie privée.",
            "<gray>La publication rend un snapshot",
            "<gray>accessible depuis /zombies."));
    view.information(
        37,
        Material.CHEST,
        "<gold>Étape 3 — Cycle de vie",
        List.of(
            "<gray>Archiver conserve les fichiers.",
            "<red>Supprimer efface définitivement",
            "<red>tout le contenu possédé."));
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
              "<yellow>Cliquez pour restaurer cette version"),
          "zombies.admin.maps.rollback",
          click -> requestRollback(click.player(), id, version.version()),
          null,
          null);
    }
    navigation(view, page, versions.size(), "version(s)");
  }

  private void renderValidation(GuiView view, GuiContext context) {
    String id = context.value("map", String.class).orElse("");
    MapDefinition map = editors.registry().find(id).orElse(null);
    if (map == null) {
      view.information(22, Material.BARRIER, "<red>Map introuvable", List.of());
      view.configured("back");
      return;
    }
    List<ValidationItem> issues = validationItems(map);
    GuiSession session = guis.session(view.player());
    GuiPagination.Page<ValidationItem> page =
        GuiPagination.page(issues, session.page(), view.menu().contentSlots().size());
    session.page(page.index());
    for (int index = 0; index < page.items().size(); index++) {
      ValidationItem issue = page.items().get(index);
      view.button(
          view.menu().contentSlots().get(index),
          issue.material(),
          issue.title() + issue.message(),
          List.of(
              "<gray>Type : <white>" + issue.type(),
              "<gray>Solution : <white>" + issue.solution(),
              issue.position().isPresent()
                  ? "<yellow>Cliquez pour vous téléporter"
                  : "<dark_gray>Aucune position associée"),
          "zombies.admin.maps.validate",
          click -> issue.position().ifPresent(point -> teleport(click.player(), point)),
          null,
          null);
    }
    navigation(view, page, issues.size(), "problème(s)");
  }

  private void requestCreate(Player player) {
    guis.requestInput(
        player,
        new GuiInputRequest(
            MINI.deserialize("<aqua>Entrez l'identifiant de la nouvelle map."),
            guis.inputExpiry(),
            value ->
                MapDefinition.safeId(value.toLowerCase(Locale.ROOT))
                    ? GuiInputRequest.Validation.accept()
                    : GuiInputRequest.Validation.reject(
                        MINI.deserialize("<red>Utilisez a-z, 0-9, _ ou - (64 caractères).")),
            value ->
                editors
                    .create(
                        value.toLowerCase(Locale.ROOT),
                        value.toLowerCase(Locale.ROOT),
                        player.getUniqueId(),
                        player.getWorld().getName())
                    .whenCompleteAsync(
                        (map, failure) -> {
                          if (failure != null) {
                            player.sendMessage(
                                MINI.deserialize("<red>Création refusée : " + safe(failure)));
                          } else {
                            edit(player, map.id());
                          }
                        },
                        mainThread),
            () -> {}));
  }

  private void requestDuplicate(Player player, String sourceId) {
    MapDefinition source = editors.registry().find(sourceId).orElse(null);
    if (source == null || loadEditingWorld(source) == null) {
      player.sendMessage(MINI.deserialize("<red>Le monde source ne peut pas être chargé."));
      return;
    }
    guis.requestInput(
        player,
        new GuiInputRequest(
            MINI.deserialize("<aqua>Entrez l'identifiant de la copie."),
            guis.inputExpiry(),
            value ->
                MapDefinition.safeId(value.toLowerCase(Locale.ROOT))
                        && editors.registry().find(value.toLowerCase(Locale.ROOT)).isEmpty()
                    ? GuiInputRequest.Validation.accept()
                    : GuiInputRequest.Validation.reject(
                        MINI.deserialize("<red>Identifiant invalide ou déjà utilisé.")),
            value -> {
              String newId = value.toLowerCase(Locale.ROOT);
              worldPublications
                  .duplicateEditingWorld(source, newId)
                  .thenCompose(
                      world -> editors.duplicate(sourceId, newId, player.getUniqueId(), world))
                  .whenCompleteAsync(
                      (copy, failure) -> {
                        if (failure != null) {
                          player.sendMessage(
                              MINI.deserialize("<red>Duplication refusée : " + safe(failure)));
                        } else {
                          edit(player, copy.id());
                        }
                      },
                      mainThread);
            },
            () -> {}));
  }

  private void edit(Player player, String mapId) {
    player.closeInventory();
    previews
        .leave(player)
        .whenCompleteAsync(
            (leftPreview, failure) -> {
              if (failure != null) {
                player.sendMessage(
                    MINI.deserialize(
                        "<red>Impossible de fermer la visite avant l'édition : " + safe(failure)));
                return;
              }
              openEditor(player, mapId);
            },
            mainThread);
  }

  private void openEditor(Player player, String mapId) {
    editors
        .open(player.getUniqueId(), mapId)
        .ifPresentOrElse(
            session -> {
              World editingWorld = loadEditingWorld(session.definition());
              if (editingWorld == null) {
                editors.leave(player.getUniqueId());
                player.sendMessage(
                    MINI.deserialize("<red>Impossible de charger le monde d'édition."));
                return;
              }
              var spawn = session.definition().playerSpawn();
              org.bukkit.Location destination =
                  spawn
                      .map(
                          point ->
                              new org.bukkit.Location(
                                  editingWorld,
                                  point.x(),
                                  point.y(),
                                  point.z(),
                                  point.yaw(),
                                  point.pitch()))
                      .orElseGet(editingWorld::getSpawnLocation);
              if (!player.teleport(destination)) {
                editors.leave(player.getUniqueId());
                player.sendMessage(
                    MINI.deserialize("<red>Impossible d'entrer dans le monde d'édition."));
                return;
              }
              editorItems.give(player, session);
              visualizations.refreshEditor(
                  player.getUniqueId(), editingWorld, session.definition());
              guis.openHome(player, new GuiId("editor-main"));
              player.sendMessage(MINI.deserialize("<green>Copie de travail ouverte."));
            },
            () ->
                player.sendMessage(
                    MINI.deserialize("<red>Map inconnue, déjà ouverte ou verrouillée.")));
  }

  private void visit(Player player, String mapId) {
    if (editors.session(player.getUniqueId()).isPresent()) {
      player.sendMessage(
          MINI.deserialize(
              "<red>Fermez d'abord votre session d'édition avec <white>/zmap leave<red>."));
      return;
    }
    player.closeInventory();
    games.left(player.getUniqueId());
    instances.leave(player);
    player.sendMessage(MINI.deserialize("<yellow>Préparation d'une copie privée de la map..."));
    previews
        .open(player, mapId)
        .whenCompleteAsync(
            (opened, failure) ->
                player.sendMessage(
                    failure == null
                        ? MINI.deserialize(
                            "<green>Map chargée. <gray>Retour : <white>/zombie map leave")
                        : MINI.deserialize(
                            "<red>Visite impossible : "
                                + safe(failure)
                                + ". Vérifiez zombie_templates/"
                                + mapId
                                + ".")),
            mainThread);
  }

  private void openAdminMaps(Player player) {
    guis.open(player, new GuiId("admin-maps"));
    templates
        .discover()
        .whenCompleteAsync(
            (ignored, failure) -> {
              if (failure != null) {
                player.sendMessage(
                    MINI.deserialize("<red>Impossible d'actualiser les templates importés."));
              }
              guis.refresh(player);
            },
            mainThread);
  }

  private void publish(Player player, String mapId) {
    MapDefinition definition = editors.registry().find(mapId).orElse(null);
    if (definition == null) {
      return;
    }
    Optional<UUID> editor = editors.editorOf(mapId);
    if (editor.isPresent() && !editor.orElseThrow().equals(player.getUniqueId())) {
      player.sendMessage(
          MINI.deserialize("<red>Publication refusée : cette map est encore modifiée ailleurs."));
      return;
    }
    player.sendMessage(MINI.deserialize("<yellow>Sauvegarde et copie du monde d'édition..."));
    CompletableFuture<Void> closedEditor;
    if (editors
        .session(player.getUniqueId())
        .filter(session -> session.definition().id().equals(mapId))
        .isPresent()) {
      visualizations.clearEditor(player.getUniqueId());
      editorItems.remove(player);
      closedEditor = editors.leave(player.getUniqueId()).thenApply(ignored -> null);
      instances.leave(player);
    } else {
      closedEditor = CompletableFuture.completedFuture(null);
    }
    closedEditor
        .thenComposeAsync(ignored -> worldPublications.updateTemplate(definition), mainThread)
        .thenCompose(ignored -> templates.find(mapId))
        .thenCompose(
            template ->
                template.isPresent()
                    ? publications.publish(mapId, player.getUniqueId())
                    : java.util.concurrent.CompletableFuture.failedFuture(
                        new IllegalStateException(
                            "Le dossier zombie_templates/" + mapId + " est absent ou invalide")))
        .whenCompleteAsync(
            (publication, failure) -> {
              player.sendMessage(
                  failure == null
                      ? MINI.deserialize(
                          "<green>Map publiée en version <white>"
                              + publication.activeVersion().orElseThrow())
                      : MINI.deserialize("<red>Publication refusée : " + safe(failure)));
              guis.refresh(player);
            },
            mainThread);
  }

  private void unpublish(Player player, String mapId) {
    publications
        .unpublish(mapId)
        .whenCompleteAsync(
            (publication, failure) -> {
              player.sendMessage(
                  failure == null
                      ? MINI.deserialize("<yellow>Map retirée du catalogue joueur.")
                      : MINI.deserialize("<red>Opération refusée : " + safe(failure)));
              guis.refresh(player);
            },
            mainThread);
  }

  private void requestRollback(Player player, String mapId, int version) {
    guis.confirm(
        player,
        new GuiConfirmation(
            MINI.deserialize("<gold>Restaurer une version publiée"),
            MINI.deserialize("<white>" + mapId + " v" + version),
            MINI.deserialize(
                "<gray>Un nouvel instantané sera publié; aucun historique ne sera effacé."),
            clock.instant().plusSeconds(1),
            context ->
                publications
                    .rollback(mapId, version, player.getUniqueId())
                    .whenCompleteAsync(
                        (publication, failure) -> {
                          context.session().confirmation(null);
                          player.sendMessage(
                              failure == null
                                  ? MINI.deserialize(
                                      "<green>Version restaurée et publiée en v"
                                          + publication.activeVersion().orElseThrow()
                                          + ".")
                                  : MINI.deserialize(
                                      "<red>Restauration refusée : " + safe(failure)));
                          guis.back(player);
                        },
                        mainThread)));
  }

  private void requestArchive(Player player, String mapId) {
    boolean active =
        instances.activeInstances().stream().anyMatch(value -> value.mapId().equals(mapId));
    if (active) {
      player.sendMessage(
          MINI.deserialize("<red>Archivage refusé : une instance utilise encore cette map."));
      return;
    }
    guis.confirm(
        player,
        new GuiConfirmation(
            MINI.deserialize("<red>Archiver la map"),
            MINI.deserialize("<white>" + mapId),
            MINI.deserialize(
                "<gray>La map disparaîtra du catalogue; ses fichiers et versions seront conservés."),
            clock.instant().plusSeconds(1),
            context ->
                publications
                    .changeStatus(mapId, MapStatus.ARCHIVED)
                    .whenCompleteAsync(
                        (publication, failure) -> {
                          context.session().confirmation(null);
                          player.sendMessage(
                              failure == null
                                  ? MINI.deserialize("<green>Map archivée sans perte de données.")
                                  : MINI.deserialize("<red>Archivage refusé : " + safe(failure)));
                          guis.back(player);
                        },
                        mainThread)));
  }

  private void requestDeletion(Player player, String mapId) {
    MapDefinition definition = editors.registry().find(mapId).orElse(null);
    if (definition == null) {
      player.sendMessage(MINI.deserialize("<red>Suppression refusée : map introuvable."));
      return;
    }
    String blocker = deletionBlocker(mapId);
    if (blocker != null) {
      player.sendMessage(MINI.deserialize("<red>Suppression refusée : " + blocker));
      return;
    }
    guis.confirm(
        player,
        new GuiConfirmation(
            MINI.deserialize("<dark_red>Supprimer définitivement la map"),
            MINI.deserialize("<white>" + mapId),
            MINI.deserialize(
                "<red>Configuration, spawns, objets, versions et mondes possédés seront supprimés."),
            clock.instant().plusSeconds(1),
            context -> {
              String currentBlocker = deletionBlocker(mapId);
              if (currentBlocker != null) {
                context.session().confirmation(null);
                player.sendMessage(
                    MINI.deserialize("<red>Suppression refusée : " + currentBlocker));
                guis.back(player);
                return;
              }
              worldPublications
                  .prepareDeletion(definition)
                  .thenCompose(ignored -> publications.delete(mapId, () -> editors.delete(mapId)))
                  .thenRun(
                      () -> {
                        templates.refreshCount();
                      })
                  .whenCompleteAsync(
                      (deleted, failure) -> {
                        context.session().confirmation(null);
                        if (failure == null) {
                          player.sendMessage(
                              MINI.deserialize(
                                  "<green>Map supprimée avec sa configuration, ses versions et ses mondes."));
                          guis.open(player, new GuiId("admin-maps"));
                        } else {
                          player.sendMessage(
                              MINI.deserialize("<red>Suppression impossible : " + safe(failure)));
                          guis.back(player);
                        }
                      },
                      mainThread);
            }));
  }

  private String deletionBlocker(String mapId) {
    if (instances.activeInstances().stream().anyMatch(value -> value.mapId().equals(mapId))) {
      return "une instance utilise encore cette map.";
    }
    if (editors.editorOf(mapId).isPresent()) {
      return "la map est encore ouverte dans l'éditeur.";
    }
    return null;
  }

  private void test(Player player, String mapId) {
    MapDefinition map = editors.registry().find(mapId).orElse(null);
    if (map == null || !validator.validate(map).valid()) {
      player.sendMessage(MINI.deserialize("<red>La map doit être valide avant le test."));
      return;
    }
    if (loadEditingWorld(map) == null) {
      player.sendMessage(MINI.deserialize("<red>Le monde d'édition ne peut pas être chargé."));
      return;
    }
    Optional<UUID> editor = editors.editorOf(mapId);
    if (editor.isPresent() && !editor.orElseThrow().equals(player.getUniqueId())) {
      player.sendMessage(MINI.deserialize("<red>La map est encore modifiée par un autre admin."));
      return;
    }
    player.closeInventory();
    CompletableFuture<Void> closedEditor;
    if (editors
        .session(player.getUniqueId())
        .filter(session -> session.definition().id().equals(mapId))
        .isPresent()) {
      visualizations.clearEditor(player.getUniqueId());
      editorItems.remove(player);
      closedEditor = editors.leave(player.getUniqueId()).thenApply(ignored -> null);
      instances.leave(player);
    } else {
      closedEditor = CompletableFuture.completedFuture(null);
    }
    closedEditor
        .thenComposeAsync(ignored -> previews.leave(player), mainThread)
        .thenComposeAsync(
            ignored -> {
              games.left(player.getUniqueId());
              instances.leave(player);
              return worldPublications.updateTemplate(map);
            },
            mainThread)
        .thenCompose(
            ignored ->
                instances.create(mapId, Optional.of(player.getUniqueId()), map.maximumPlayers()))
        .thenCompose(
            created ->
                instances
                    .join(player, created.id())
                    .thenApply(
                        result -> {
                          if (result != PlayerInstanceResult.SUCCESS) {
                            throw new IllegalStateException("Entrée refusée : " + result);
                          }
                          games.start(created.id());
                          return created;
                        }))
        .whenCompleteAsync(
            (created, failure) ->
                player.sendMessage(
                    failure == null
                        ? MINI.deserialize("<green>Partie de test démarrée.")
                        : MINI.deserialize("<red>Test impossible : " + safe(failure))),
            mainThread);
  }

  private void joinPublished(Player player, String mapId) {
    if (publications.publishedDefinition(mapId).isEmpty()) {
      player.sendMessage(MINI.deserialize("<red>Cette map n'est plus publiée."));
      return;
    }
    player.closeInventory();
    Optional<GameInstanceSnapshot> available =
        publicInstances(mapId).stream()
            .filter(
                value ->
                    value.players().size() < value.maximumPlayers()
                        && (value.state() == GameInstanceState.WAITING
                            || value.state() == GameInstanceState.STARTING
                            || value.state() == GameInstanceState.RUNNING))
            .findFirst();
    if (available.isPresent()) {
      joinExisting(player, available.orElseThrow());
      return;
    }
    player.sendMessage(MINI.deserialize("<yellow>Création automatique d'une partie..."));
    instances
        .create(
            mapId,
            Optional.empty(),
            publications.publishedDefinition(mapId).orElseThrow().maximumPlayers())
        .thenCompose(
            created ->
                instances
                    .join(player, created.id())
                    .thenApply(
                        result -> {
                          if (result != PlayerInstanceResult.SUCCESS) {
                            throw new IllegalStateException("Entrée refusée : " + result);
                          }
                          MapDefinition published =
                              publications.publishedDefinition(mapId).orElseThrow();
                          int players =
                              instances.activeInstances().stream()
                                  .filter(value -> value.id().equals(created.id()))
                                  .mapToInt(value -> value.players().size())
                                  .findFirst()
                                  .orElse(0);
                          if (players >= published.minimumPlayers()) {
                            games.start(created.id());
                          }
                          return created;
                        }))
        .whenCompleteAsync(
            (created, failure) ->
                player.sendMessage(
                    failure == null
                        ? MINI.deserialize("<green>Partie créée et rejointe.")
                        : MINI.deserialize("<red>Impossible de rejoindre : " + safe(failure))),
            mainThread);
  }

  private void joinExisting(Player player, GameInstanceSnapshot instance) {
    instances
        .join(player, instance.id())
        .whenCompleteAsync(
            (result, failure) -> {
              if (failure != null || result != PlayerInstanceResult.SUCCESS) {
                player.sendMessage(
                    MINI.deserialize(
                        "<red>Connexion refusée : " + (failure == null ? result : safe(failure))));
              } else {
                if (games.snapshot(instance.id()).isPresent()) {
                  games.joined(instance.id(), player.getUniqueId());
                  return;
                }
                int players =
                    instances.activeInstances().stream()
                        .filter(value -> value.id().equals(instance.id()))
                        .mapToInt(value -> value.players().size())
                        .findFirst()
                        .orElse(0);
                int minimum =
                    publications
                        .publishedDefinition(instance.mapId())
                        .map(MapDefinition::minimumPlayers)
                        .orElse(1);
                if (players >= minimum) {
                  games.start(instance.id());
                } else {
                  player.sendMessage(
                      MINI.deserialize(
                          "<yellow>En attente de joueurs : <white>" + players + "/" + minimum));
                }
              }
            },
            mainThread);
  }

  private List<GameInstanceSnapshot> publicInstances(String mapId) {
    return instances.activeInstances().stream()
        .filter(value -> value.mapId().equals(mapId) && value.owner().isEmpty())
        .toList();
  }

  private void navigation(GuiView view, GuiPagination.Page<?> page, int total, String label) {
    view.configured("back");
    view.configured("home");
    view.configured("search");
    if (page.index() > 0) {
      view.configured("previous");
    }
    if (page.index() + 1 < page.pageCount()) {
      view.configured("next");
    }
    view.information(
        49,
        Material.PAPER,
        "<white>Page " + (page.index() + 1) + "/" + page.pageCount(),
        List.of("<gray>" + total + " " + label));
  }

  private static String selected(GuiClickContext context) {
    return context.session().currentContext().value("map", String.class).orElse("");
  }

  private static Material material(String name) {
    Material material = Material.matchMaterial(name);
    return material == null || !material.isItem() ? Material.FILLED_MAP : material;
  }

  private static Material statusMaterial(MapStatus status) {
    return switch (status) {
      case PUBLISHED -> Material.LIME_CONCRETE;
      case INVALID -> Material.RED_CONCRETE;
      case TESTING -> Material.ENDER_EYE;
      case MAINTENANCE -> Material.ORANGE_CONCRETE;
      case ARCHIVED -> Material.GRAY_CONCRETE;
      default -> Material.FILLED_MAP;
    };
  }

  private static String empty(String value, String fallback) {
    return value.isBlank() ? fallback : value;
  }

  private String playerStatus(List<GameInstanceSnapshot> instances) {
    if (instances.isEmpty()) {
      return "Disponible";
    }
    if (instances.stream().allMatch(value -> value.players().size() >= value.maximumPlayers())) {
      return "Complète";
    }
    if (instances.stream()
        .anyMatch(
            value ->
                value.state() == GameInstanceState.RUNNING
                    || games.snapshot(value.id()).isPresent())) {
      return "Partie en cours";
    }
    if (instances.stream().anyMatch(value -> !value.players().isEmpty())) {
      return "En attente de joueurs";
    }
    return "Disponible";
  }

  private List<ValidationItem> validationItems(MapDefinition map) {
    var report = validator.validate(map);
    java.util.ArrayList<ValidationItem> result = new java.util.ArrayList<>();
    report
        .errors()
        .forEach(
            message ->
                result.add(
                    new ValidationItem(
                        "<red>Erreur : ",
                        "BLOQUANTE",
                        message,
                        "Corriger cet élément puis relancer la validation.",
                        locate(map, message),
                        Material.RED_CONCRETE)));
    report
        .warnings()
        .forEach(
            message ->
                result.add(
                    new ValidationItem(
                        "<gold>Avertissement : ",
                        "AVERTISSEMENT",
                        message,
                        "Vérifier cet élément avant la publication.",
                        locate(map, message),
                        Material.ORANGE_CONCRETE)));
    report
        .advice()
        .forEach(
            message ->
                result.add(
                    new ValidationItem(
                        "<aqua>Information : ",
                        "INFORMATION",
                        message,
                        "Compléter la configuration recommandée.",
                        locate(map, message),
                        Material.LIGHT_BLUE_CONCRETE)));
    if (result.isEmpty()) {
      result.add(
          new ValidationItem(
              "<green>Validation : ",
              "SUCCÈS",
              "Aucun problème détecté",
              "La map peut être testée ou publiée.",
              Optional.empty(),
              Material.LIME_CONCRETE));
    }
    return List.copyOf(result);
  }

  private static Optional<MapPoint> locate(MapDefinition map, String message) {
    int separator = message.indexOf(':');
    if (separator < 0 || separator + 1 >= message.length()) {
      return Optional.empty();
    }
    String id = message.substring(separator + 1).strip();
    if (map.doors().containsKey(id)) {
      return Optional.of(map.doors().get(id).position());
    }
    if (map.zombieSpawns().containsKey(id)) {
      return Optional.of(map.zombieSpawns().get(id).position());
    }
    if (map.objects().containsKey(id)) {
      return Optional.of(map.objects().get(id).position());
    }
    if (map.zones().containsKey(id)) {
      return Optional.of(map.zones().get(id).anchor());
    }
    return Optional.empty();
  }

  private static void teleport(Player player, MapPoint point) {
    World world = org.bukkit.Bukkit.getWorld(point.world());
    if (world == null) {
      player.sendMessage(MINI.deserialize("<red>Le monde associé n'est pas chargé."));
      return;
    }
    player.teleport(
        new org.bukkit.Location(
            world, point.x(), point.y(), point.z(), point.yaw(), point.pitch()));
  }

  private static String safe(Throwable failure) {
    Throwable cause =
        failure instanceof CompletionException && failure.getCause() != null
            ? failure.getCause()
            : failure;
    return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
  }

  private static World loadEditingWorld(MapDefinition definition) {
    World loaded = org.bukkit.Bukkit.getWorld(definition.world());
    if (loaded != null) {
      return loaded;
    }
    java.nio.file.Path root =
        org.bukkit.Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
    java.nio.file.Path candidate = root.resolve(definition.world()).toAbsolutePath().normalize();
    if (!candidate.startsWith(root) || !java.nio.file.Files.isDirectory(candidate)) {
      return null;
    }
    return org.bukkit.Bukkit.createWorld(new WorldCreator(definition.world()));
  }

  private record ValidationItem(
      String title,
      String type,
      String message,
      String solution,
      Optional<MapPoint> position,
      Material material) {}

  private record AdminMapEntry(String id, Optional<MapDefinition> definition) {}
}
