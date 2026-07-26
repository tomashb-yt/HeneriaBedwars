package fr.heneria.zombie.plugin.editor;

import fr.heneria.zombie.core.editor.MapDefinition;
import fr.heneria.zombie.core.editor.MapEditorService;
import fr.heneria.zombie.core.editor.MapValidator;
import fr.heneria.zombie.plugin.gui.GuiId;
import fr.heneria.zombie.plugin.gui.GuiService;
import fr.heneria.zombie.plugin.instance.InstanceCoordinator;
import fr.heneria.zombie.plugin.instance.PlayerInstanceResult;
import fr.heneria.zombie.plugin.map.MapPreviewService;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Administrative command surface for the universal map editor. */
public final class ZMapCommand implements CommandExecutor, TabCompleter {
  private static final MiniMessage MINI = MiniMessage.miniMessage();
  private final MapEditorService editors;
  private final MapValidator validator;
  private final EditorItemService items;
  private final GuiService guis;
  private final InstanceCoordinator coordinator;
  private final MapPreviewService previews;
  private final Executor mainThread;

  public ZMapCommand(
      MapEditorService editors,
      MapValidator validator,
      EditorItemService items,
      GuiService guis,
      InstanceCoordinator coordinator,
      MapPreviewService previews,
      Executor mainThread) {
    this.editors = editors;
    this.validator = validator;
    this.items = items;
    this.guis = guis;
    this.coordinator = coordinator;
    this.previews = previews;
    this.mainThread = mainThread;
  }

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] arguments) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("Cette commande doit être utilisée en jeu.");
      return true;
    }
    if (!player.hasPermission("zombie.editor")) {
      player.sendMessage(MINI.deserialize("<red>Permission zombie.editor requise."));
      return true;
    }
    if (arguments.length == 0) {
      usage(player);
      return true;
    }
    switch (arguments[0].toLowerCase(Locale.ROOT)) {
      case "create" -> {
        if (arguments.length != 2 || !MapDefinition.safeId(arguments[1].toLowerCase(Locale.ROOT))) {
          player.sendMessage(MINI.deserialize("<red>Usage : /zmap create <identifiant>"));
        } else {
          create(player, arguments[1].toLowerCase(Locale.ROOT));
        }
      }
      case "edit" -> {
        if (arguments.length != 2) {
          player.sendMessage(MINI.deserialize("<red>Usage : /zmap edit <map>"));
        } else {
          edit(player, arguments[1].toLowerCase(Locale.ROOT));
        }
      }
      case "leave" -> leave(player);
      case "validate" -> validate(player);
      case "test" -> test(player);
      case "save" -> complete(player, editors.save(player.getUniqueId()), "Map sauvegardée.");
      case "undo" -> history(player, true);
      case "redo" -> history(player, false);
      default -> usage(player);
    }
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String alias,
      @NotNull String[] arguments) {
    if (arguments.length == 1) {
      return filter(
          List.of("create", "edit", "leave", "validate", "test", "save", "undo", "redo"),
          arguments[0]);
    }
    if (arguments.length == 2 && arguments[0].equalsIgnoreCase("edit")) {
      return filter(
          editors.registry().all().stream().map(MapDefinition::id).toList(), arguments[1]);
    }
    return List.of();
  }

  private void create(Player player, String id) {
    editors
        .create(id, id, player.getUniqueId(), player.getWorld().getName())
        .whenCompleteAsync(
            (definition, failure) -> {
              if (failure != null) {
                player.sendMessage(MINI.deserialize("<red>Création refusée : " + safe(failure)));
              } else {
                player.sendMessage(
                    MINI.deserialize(
                        "<green>Map créée dans le registre : <white>" + definition.id()));
                edit(player, definition.id());
              }
            },
            mainThread);
  }

  private void edit(Player player, String id) {
    editors
        .open(player.getUniqueId(), id)
        .ifPresentOrElse(
            session -> {
              items.give(player, session);
              guis.openHome(player, new GuiId("editor-main"));
              player.sendMessage(MINI.deserialize("<green>Session d'édition ouverte."));
            },
            () ->
                player.sendMessage(MINI.deserialize("<red>Map inconnue ou session déjà ouverte.")));
  }

  private void leave(Player player) {
    player.closeInventory();
    editors
        .leave(player.getUniqueId())
        .whenCompleteAsync(
            (left, failure) -> {
              items.remove(player);
              if (failure != null) {
                player.sendMessage(MINI.deserialize("<red>La sauvegarde de sortie a échoué."));
              } else {
                player.sendMessage(
                    MINI.deserialize(
                        left ? "<green>Éditeur quitté proprement." : "<yellow>Aucune session."));
              }
            },
            mainThread);
  }

  private void validate(Player player) {
    var session = editors.session(player.getUniqueId()).orElse(null);
    if (session == null) {
      player.sendMessage(MINI.deserialize("<red>Aucune session d'édition."));
      return;
    }
    var report = validator.validate(session.definition());
    player.sendMessage(
        MINI.deserialize(
            report.valid()
                ? "<green>Validation réussie."
                : "<red>Validation échouée : " + report.errors().size() + " erreur(s)."));
    report.errors().forEach(value -> player.sendMessage(MINI.deserialize("<red>✘ " + value)));
    report.warnings().forEach(value -> player.sendMessage(MINI.deserialize("<gold>⚠ " + value)));
    report.advice().forEach(value -> player.sendMessage(MINI.deserialize("<aqua>ℹ " + value)));
  }

  private void test(Player player) {
    var session = editors.session(player.getUniqueId()).orElse(null);
    if (session == null) {
      player.sendMessage(MINI.deserialize("<red>Ouvrez d'abord la map avec /zmap edit <map>."));
      return;
    }
    MapDefinition definition = session.definition();
    var report = validator.validate(definition);
    if (!report.valid()) {
      player.sendMessage(
          MINI.deserialize(
              "<red>Test refusé : " + report.errors().size() + " erreur(s) bloquante(s)."));
      report.errors().forEach(value -> player.sendMessage(MINI.deserialize("<red>✘ " + value)));
      return;
    }
    player.closeInventory();
    player.sendMessage(MINI.deserialize("<yellow>Préparation de l'instance de test..."));
    editors
        .leave(player.getUniqueId())
        .thenComposeAsync(
            ignored -> {
              items.remove(player);
              return previews.leave(player);
            },
            mainThread)
        .thenComposeAsync(
            leftPreview -> {
              if (!leftPreview) {
                coordinator.leave(player);
              }
              return coordinator.create(definition.id(), Optional.of(player.getUniqueId()));
            },
            mainThread)
        .thenCompose(
            created ->
                coordinator
                    .join(player, created.id())
                    .thenCompose(
                        result -> {
                          if (result == PlayerInstanceResult.SUCCESS) {
                            return java.util.concurrent.CompletableFuture.completedFuture(created);
                          }
                          return coordinator
                              .stop(created.id())
                              .thenCompose(
                                  ignored ->
                                      java.util.concurrent.CompletableFuture.failedFuture(
                                          new IllegalStateException(
                                              "Entrée refusée : " + result.name())));
                        }))
        .whenCompleteAsync(
            (created, failure) ->
                player.sendMessage(
                    failure == null
                        ? MINI.deserialize(
                            "<green>Instance de test rejointe : <white>"
                                + created.id().toString().substring(0, 8))
                        : MINI.deserialize("<red>Test impossible : " + safe(failure))),
            mainThread);
  }

  private void history(Player player, boolean undo) {
    var future = undo ? editors.undo(player.getUniqueId()) : editors.redo(player.getUniqueId());
    future.whenCompleteAsync(
        (changed, failure) ->
            player.sendMessage(
                failure == null && changed
                    ? MINI.deserialize("<green>Historique appliqué et sauvegardé.")
                    : MINI.deserialize("<yellow>Aucune version disponible.")),
        mainThread);
  }

  private void complete(
      Player player, java.util.concurrent.CompletableFuture<?> future, String text) {
    future.whenCompleteAsync(
        (ignored, failure) ->
            player.sendMessage(
                failure == null
                    ? MINI.deserialize("<green>" + text)
                    : MINI.deserialize("<red>Opération échouée : " + safe(failure))),
        mainThread);
  }

  private static void usage(Player player) {
    player.sendMessage(
        MINI.deserialize(
            "<yellow>/zmap create <nom>, edit <map>, leave, validate, test, save, undo, redo"));
  }

  private static List<String> filter(List<String> values, String prefix) {
    String normalized = prefix.toLowerCase(Locale.ROOT);
    return values.stream().filter(value -> value.startsWith(normalized)).toList();
  }

  private static String safe(Throwable failure) {
    Throwable cause =
        failure instanceof CompletionException && failure.getCause() != null
            ? failure.getCause()
            : failure;
    return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
  }
}
