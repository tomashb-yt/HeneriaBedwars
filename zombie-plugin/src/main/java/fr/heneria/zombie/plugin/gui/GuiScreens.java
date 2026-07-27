package fr.heneria.zombie.plugin.gui;

import fr.heneria.zombie.api.ZombieApi;
import fr.heneria.zombie.core.instance.GameInstanceSnapshot;
import fr.heneria.zombie.core.session.PlayerSessionService;
import fr.heneria.zombie.plugin.config.ConfigurationManager;
import fr.heneria.zombie.plugin.instance.InstanceCoordinator;
import fr.heneria.zombie.plugin.map.MapPreviewService;
import fr.heneria.zombie.plugin.map.MapTemplateCatalog;
import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Registers Ticket 003 screens and their stable, reusable action callbacks. */
public final class GuiScreens {

  public static final java.util.Set<String> ACTION_IDS =
      java.util.Set.of(
          "nav.player",
          "nav.admin",
          "nav.maps",
          "nav.instances",
          "nav.diagnostics",
          "nav.weapons",
          "nav.back",
          "nav.home",
          "page.previous",
          "page.next",
          "maps.search",
          "confirmation.confirm",
          "confirmation.cancel",
          "config.reload",
          "feedback.unavailable");

  private static final MiniMessage MINI = MiniMessage.miniMessage();
  private final JavaPlugin plugin;
  private final String version;
  private final GuiRegistry registry;
  private final GuiActionRegistry actions;
  private final GuiConfigurationService guiConfigurations;
  private final ConfigurationManager configurations;
  private final GuiService guiService;
  private final MapTemplateCatalog templates;
  private final MapPreviewService previews;
  private final InstanceCoordinator coordinator;
  private final PlayerSessionService playerSessions;
  private final ZombieApi api;
  private final Executor mainThread;
  private final Clock clock;

  public GuiScreens(
      JavaPlugin plugin,
      String version,
      GuiRegistry registry,
      GuiActionRegistry actions,
      GuiConfigurationService guiConfigurations,
      ConfigurationManager configurations,
      GuiService guiService,
      MapTemplateCatalog templates,
      MapPreviewService previews,
      InstanceCoordinator coordinator,
      PlayerSessionService playerSessions,
      ZombieApi api,
      Executor mainThread,
      Clock clock) {
    this.plugin = plugin;
    this.version = version;
    this.registry = registry;
    this.actions = actions;
    this.guiConfigurations = guiConfigurations;
    this.configurations = configurations;
    this.guiService = guiService;
    this.templates = templates;
    this.previews = previews;
    this.coordinator = coordinator;
    this.playerSessions = playerSessions;
    this.api = api;
    this.mainThread = mainThread;
    this.clock = clock;
  }

  /** Registers every foundational screen and action exactly once. */
  public void register() {
    registerActions();
    registry.register(
        new StandardGui("player-main", guiConfigurations, this::renderPlayer, () -> 0));
    registry.register(new StandardGui("admin-main", guiConfigurations, this::renderAdmin, () -> 0));
    registry.register(
        new StandardGui(
            "maps",
            guiConfigurations,
            this::renderMaps,
            () -> configurations.current().settings().gui().mapsMenuTicks()));
    registry.register(
        new StandardGui(
            "instances",
            guiConfigurations,
            this::renderInstances,
            () -> configurations.current().settings().gui().instancesMenuTicks()));
    registry.register(
        new StandardGui(
            "diagnostics",
            guiConfigurations,
            this::renderDiagnostics,
            () -> configurations.current().settings().gui().diagnosticsMenuTicks()));
    registry.register(
        new StandardGui("confirmation", guiConfigurations, this::renderConfirmation, () -> 0));
  }

  private void registerActions() {
    actions.register(
        "nav.player", context -> guiService.openHome(context.player(), new GuiId("player-main")));
    actions.register(
        "nav.admin", context -> guiService.openHome(context.player(), new GuiId("admin-main")));
    actions.register("nav.maps", context -> openMaps(context.player()));
    actions.register(
        "nav.instances", context -> guiService.open(context.player(), new GuiId("instances")));
    actions.register(
        "nav.diagnostics", context -> guiService.open(context.player(), new GuiId("diagnostics")));
    actions.register("nav.back", context -> guiService.back(context.player()));
    actions.register("nav.home", context -> guiService.home(context.player()));
    actions.register(
        "page.previous",
        context -> {
          context.session().page(context.session().page() - 1);
          guiService.refresh(context.player());
        });
    actions.register(
        "page.next",
        context -> {
          context.session().page(context.session().page() + 1);
          guiService.refresh(context.player());
        });
    actions.register("maps.search", context -> requestSearch(context.player()));
    actions.register("confirmation.confirm", this::confirm);
    actions.register(
        "confirmation.cancel",
        context -> {
          context.session().confirmation(null);
          guiService.back(context.player());
        });
    actions.register("config.reload", this::reload);
    actions.register(
        "feedback.unavailable",
        context ->
            context
                .player()
                .sendMessage(
                    MINI.deserialize("<gray>Cette fonction sera ajoutée par un prochain ticket.")));
  }

  private void renderPlayer(GuiView view, GuiContext ignored) {
    for (String key : List.of("play", "leave", "statistics", "settings", "help", "admin")) {
      view.configured(key);
    }
  }

  private void renderAdmin(GuiView view, GuiContext ignored) {
    for (String key :
        List.of(
            "maps",
            "instances",
            "weapons",
            "zombies",
            "economy",
            "powerups",
            "settings",
            "editor",
            "diagnostics",
            "documentation",
            "reload",
            "player")) {
      view.configured(key);
    }
  }

  private void openMaps(Player player) {
    guiService.open(player, new GuiId("maps"));
    templates
        .discover()
        .whenCompleteAsync(
            (ignored, failure) -> {
              if (failure != null) {
                player.sendMessage(MINI.deserialize("<red>Impossible d'actualiser les maps."));
              }
              guiService.refresh(player);
            },
            mainThread);
  }

  private void renderMaps(GuiView view, GuiContext ignored) {
    GuiSession session = guiService.session(owner(view));
    List<String> maps =
        templates.knownMapIds().stream()
            .filter(
                id ->
                    session.search().isBlank()
                        || id.toLowerCase(Locale.ROOT)
                            .contains(session.search().toLowerCase(Locale.ROOT)))
            .sorted()
            .toList();
    GuiPagination.Page<String> page =
        GuiPagination.page(maps, session.page(), view.menu().contentSlots().size());
    session.page(page.index());
    for (int index = 0; index < page.items().size(); index++) {
      String map = page.items().get(index);
      int slot = view.menu().contentSlots().get(index);
      view.button(
          slot,
          Material.FILLED_MAP,
          "<aqua>" + map,
          List.of(
              "<gray>Identifiant : <white>" + map,
              "<gray>Validation : <green>disponible",
              "<gray>Configuration : <white>automatique ou v1",
              "<gray>Zones : <white>0",
              "<gray>Spawns : <white>0",
              "<gray>Erreurs : <green>0",
              "",
              "<yellow>Clic gauche : prévisualiser",
              "<gold>Maj + clic : créer une instance",
              "<dark_gray>Édition/export/suppression : prochain ticket"),
          "zombie.gui.maps",
          context -> preview(context.player(), map),
          context -> unavailable(context.player()),
          context -> create(context.player(), map));
    }
    renderListNavigation(view, page, maps.size(), "map(s)");
  }

  private void renderInstances(GuiView view, GuiContext ignored) {
    GuiSession session = guiService.session(owner(view));
    List<GameInstanceSnapshot> snapshots =
        coordinator.activeInstances().stream()
            .sorted(Comparator.comparing(GameInstanceSnapshot::createdAt))
            .toList();
    GuiPagination.Page<GameInstanceSnapshot> page =
        GuiPagination.page(snapshots, session.page(), view.menu().contentSlots().size());
    session.page(page.index());
    for (int index = 0; index < page.items().size(); index++) {
      GameInstanceSnapshot instance = page.items().get(index);
      int slot = view.menu().contentSlots().get(index);
      String shortId = instance.id().toString().substring(0, 8);
      view.button(
          slot,
          Material.ENDER_EYE,
          "<light_purple>" + instance.mapId() + " <dark_gray>#" + shortId,
          List.of(
              "<gray>État : <white>" + instance.state(),
              "<gray>Joueurs : <white>"
                  + instance.players().size()
                  + "/"
                  + instance.maximumPlayers(),
              "<gray>Durée : <white>"
                  + Duration.between(instance.createdAt(), clock.instant()).toSeconds()
                  + " s",
              "<gray>Monde : <white>" + instance.worldName().orElse("préparation"),
              "<gray>Propriétaire : <white>"
                  + instance.owner().map(UUID::toString).orElse("serveur"),
              "<gray>Accès : <white>" + instance.access(),
              "",
              "<yellow>Clic gauche : rejoindre",
              "<red>Maj + clic : demander l'arrêt"),
          "zombie.play",
          context -> join(context.player(), instance.id()),
          null,
          context -> requestStop(context.player(), instance));
    }
    renderListNavigation(view, page, snapshots.size(), "instance(s)");
  }

  private void renderDiagnostics(GuiView view, GuiContext ignored) {
    view.information(
        22,
        Material.REDSTONE_TORCH,
        "<aqua>Diagnostic HeneriaZombie",
        List.of(
            "<gray>Plugin : <white>" + version,
            "<gray>Serveur : <white>" + plugin.getServer().getVersion(),
            "<gray>Instances : <white>" + api.activeInstanceCount(),
            "<gray>Sessions joueur : <white>" + playerSessions.sessions().size(),
            "<gray>Sessions GUI : <white>" + guiService.openSessionCount(),
            "<gray>Stockage : <yellow>SQLite configuré, initialisation future",
            "<gray>Lobby : <white>" + configurations.current().settings().lobby().world(),
            "<gray>Avertissements GUI : <white>" + guiConfigurations.current().warnings().size()));
    view.configured("back");
    view.configured("home");
  }

  private void renderConfirmation(GuiView view, GuiContext ignored) {
    GuiSession session = guiService.session(owner(view));
    Optional<GuiConfirmation> pending = session.confirmation();
    if (pending.isEmpty()) {
      view.information(
          22,
          Material.BARRIER,
          "<red>Confirmation expirée",
          List.of("<gray>Revenez au menu précédent."));
      view.configured("cancel");
      return;
    }
    GuiConfirmation confirmation = pending.orElseThrow();
    view.information(
        22,
        Material.PAPER,
        "<gold>Action sensible",
        List.of(
            MINI.serialize(confirmation.action()),
            MINI.serialize(confirmation.target()),
            MINI.serialize(confirmation.consequences())));
    view.configured("confirm");
    view.configured("cancel");
  }

  private void renderListNavigation(
      GuiView view, GuiPagination.Page<?> page, int total, String label) {
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
        List.of(
            "<gray>" + total + " " + label,
            "<gray>Recherche : <white>" + guiService.session(owner(view)).search()));
  }

  private void requestSearch(Player player) {
    GuiSession session = guiService.session(player);
    guiService.requestInput(
        player,
        new GuiInputRequest(
            MINI.deserialize("<aqua>Entrez une recherche de map dans le chat."),
            guiService.inputExpiry(),
            value ->
                value.length() <= 64
                    ? GuiInputRequest.Validation.accept()
                    : GuiInputRequest.Validation.reject(
                        MINI.deserialize("<red>64 caractères maximum.")),
            value -> session.search(value),
            () -> {}));
  }

  private void preview(Player player, String map) {
    player.closeInventory();
    previews
        .open(player, map)
        .whenCompleteAsync(
            (ignored, failure) -> {
              if (failure != null) {
                player.sendMessage(
                    MINI.deserialize("<red>Prévisualisation impossible : " + safe(failure)));
              }
            },
            mainThread);
  }

  private void create(Player player, String map) {
    coordinator
        .create(map, Optional.of(player.getUniqueId()))
        .whenCompleteAsync(
            (instance, failure) -> {
              if (failure != null) {
                player.sendMessage(MINI.deserialize("<red>Création impossible : " + safe(failure)));
              } else {
                player.sendMessage(
                    MINI.deserialize(
                        "<green>Instance créée : <white>"
                            + instance.id().toString().substring(0, 8)));
                guiService.open(player, new GuiId("instances"));
              }
            },
            mainThread);
  }

  private void join(Player player, UUID id) {
    player.closeInventory();
    coordinator
        .join(player, id)
        .whenCompleteAsync(
            (result, failure) -> {
              if (failure != null) {
                player.sendMessage(
                    MINI.deserialize("<red>Connexion impossible : " + safe(failure)));
              } else if (result != fr.heneria.zombie.plugin.instance.PlayerInstanceResult.SUCCESS) {
                player.sendMessage(MINI.deserialize("<red>Connexion refusée : " + result));
              }
            },
            mainThread);
  }

  private void requestStop(Player player, GameInstanceSnapshot instance) {
    if (!player.hasPermission("zombie.gui.dangerous-actions")) {
      player.sendMessage(MINI.deserialize("<red>Permission dangereuse requise."));
      return;
    }
    int delay = configurations.current().settings().gui().confirmationDelayTicks();
    guiService.confirm(
        player,
        new GuiConfirmation(
            MINI.deserialize("<red>Arrêter de force l'instance"),
            MINI.deserialize("<white>" + instance.id()),
            MINI.deserialize(
                "<gray>Les joueurs seront renvoyés au lobby et le monde sera nettoyé."),
            clock.instant().plusMillis(delay * 50L),
            context ->
                coordinator
                    .stop(instance.id())
                    .whenCompleteAsync(
                        (stopped, failure) -> {
                          context.session().confirmation(null);
                          context
                              .player()
                              .sendMessage(
                                  failure == null && stopped
                                      ? MINI.deserialize("<green>Instance arrêtée.")
                                      : MINI.deserialize(
                                          "<red>L'instance n'existe plus ou l'arrêt a échoué."));
                          guiService.back(context.player());
                        },
                        mainThread)));
  }

  private void confirm(GuiClickContext context) {
    GuiConfirmation pending = context.session().confirmation().orElse(null);
    if (pending == null) {
      return;
    }
    if (!pending.availableAt(clock.instant())) {
      context.player().sendMessage(MINI.deserialize("<gold>Patientez avant de confirmer."));
      return;
    }
    context.session().confirmation(null);
    pending.confirmed().execute(context);
  }

  private void reload(GuiClickContext context) {
    if (!context.player().hasPermission("zombie.command.reload")) {
      context.player().sendMessage(MINI.deserialize("<red>Permission manquante."));
      return;
    }
    guiConfigurations
        .reloadAsync()
        .whenCompleteAsync(
            (snapshot, failure) ->
                context
                    .player()
                    .sendMessage(
                        failure == null
                            ? MINI.deserialize("<green>Configuration GUI rechargée.")
                            : MINI.deserialize(
                                "<red>Configuration GUI refusée : " + safe(failure))),
            mainThread);
  }

  private void unavailable(Player player) {
    player.sendMessage(MINI.deserialize("<gray>Cette action arrivera dans un prochain ticket."));
  }

  private static Player owner(GuiView view) {
    return view.player();
  }

  private static String safe(Throwable failure) {
    Throwable cause =
        failure instanceof CompletionException && failure.getCause() != null
            ? failure.getCause()
            : failure;
    return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
  }
}
