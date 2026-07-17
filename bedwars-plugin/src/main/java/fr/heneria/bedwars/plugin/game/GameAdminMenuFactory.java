package fr.heneria.bedwars.plugin.game;

import fr.heneria.bedwars.api.game.GameState;
import fr.heneria.bedwars.core.command.AdministrativeCommandPolicy;
import fr.heneria.bedwars.core.config.PlaceholderContext;
import fr.heneria.bedwars.core.game.GameId;
import fr.heneria.bedwars.core.game.GameInstance;
import fr.heneria.bedwars.core.game.GameInstanceManager;
import fr.heneria.bedwars.core.game.countdown.CountdownOperationResult;
import fr.heneria.bedwars.core.game.countdown.GameCountdownService;
import fr.heneria.bedwars.core.game.lobby.GameJoinResult;
import fr.heneria.bedwars.core.game.lobby.GameLobbyService;
import fr.heneria.bedwars.core.gui.ConfirmationGui;
import fr.heneria.bedwars.core.gui.Gui;
import fr.heneria.bedwars.core.gui.GuiButton;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import fr.heneria.bedwars.plugin.gui.StandardGuiButtons;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Compact, administrative-only runtime game list and instance control menus. */
public final class GameAdminMenuFactory {
  private static final List<Integer> CONTENT_SLOTS =
      List.of(
          10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37,
          38, 39, 40, 41, 42, 43);

  private final JavaPlugin plugin;
  private final GameInstanceManager games;
  private final GameCountdownService countdowns;
  private final GameLobbyService lobby;
  private final ConfigurationService configurations;
  private final StandardGuiButtons standard = new StandardGuiButtons();

  public GameAdminMenuFactory(
      JavaPlugin plugin,
      GameInstanceManager games,
      GameCountdownService countdowns,
      GameLobbyService lobby,
      ConfigurationService configurations) {
    this.plugin = plugin;
    this.games = games;
    this.countdowns = countdowns;
    this.lobby = lobby;
    this.configurations = configurations;
  }

  /** Displays live games with only the actions relevant to their current runtime state. */
  public Gui list(UUID playerId) {
    List<GameInstance> visible = games.all().stream().sorted(this::compareGames).toList();
    Gui.Builder builder =
        Gui.builder()
            .id("game.admin.list")
            .title(message("game.gui.list.title", Map.of("count", visible.size())))
            .rows(5)
            .fillEmptySlots(true)
            .button(
                4,
                GuiButton.builder()
                    .itemKey("game.admin.summary")
                    .itemPlaceholders(context -> summaryValues(visible))
                    .build())
            .button(36, standard.refresh())
            .button(44, standard.close());
    if (visible.isEmpty())
      builder.button(22, GuiButton.builder().itemKey("game.admin.empty").build());
    for (int index = 0; index < Math.min(CONTENT_SLOTS.size(), visible.size()); index++) {
      GameInstance game = visible.get(index);
      builder.button(
          CONTENT_SLOTS.get(index),
          GuiButton.builder()
              .itemKey("game.admin.entry")
              .itemPlaceholders(context -> values(game))
              .onLeftClick(context -> context.open(info(playerId, game.id())))
              .build());
    }
    return builder.build();
  }

  /** Presents a game as a guided status card with join, start, force and stop actions. */
  public Gui info(UUID playerId, GameId gameId) {
    GameInstance game = games.find(gameId).orElse(null);
    if (game == null)
      return Gui.builder()
          .id("game.admin.missing")
          .title(message("game.gui.missing.title", Map.of()))
          .rows(3)
          .fillEmptySlots(true)
          .button(13, GuiButton.builder().itemKey("game.admin.missing").build())
          .button(
              22,
              GuiButton.builder()
                  .itemKey("gui.back")
                  .onLeftClick(context -> context.open(list(playerId)))
                  .build())
          .build();
    Map<String, Object> values = values(game);
    return Gui.builder()
        .id("game.admin.info." + game.id().shortId())
        .title(message("game.gui.info.title", values))
        .rows(4)
        .fillEmptySlots(true)
        .button(
            4,
            GuiButton.builder()
                .itemKey("game.admin.info")
                .itemPlaceholders(context -> values)
                .build())
        .button(
            11,
            GuiButton.builder()
                .itemKey("game.admin.join")
                .itemPlaceholders(context -> values)
                .permission(AdministrativeCommandPolicy.GAME_JOIN)
                .onLeftClick(context -> join(playerId, game))
                .build())
        .button(
            13,
            GuiButton.builder()
                .itemKey("game.admin.start")
                .itemPlaceholders(context -> values)
                .permission(AdministrativeCommandPolicy.GAME_START)
                .onLeftClick(context -> start(playerId, game, false))
                .build())
        .button(
            15,
            GuiButton.builder()
                .itemKey("game.admin.force-start")
                .itemPlaceholders(context -> values)
                .permission(AdministrativeCommandPolicy.GAME_FORCE_START)
                .onLeftClick(context -> start(playerId, game, true))
                .build())
        .button(
            22,
            GuiButton.builder()
                .itemKey("game.admin.stop")
                .itemPlaceholders(context -> values)
                .permission(AdministrativeCommandPolicy.GAME_STOP)
                .onLeftClick(context -> context.open(stopConfirmation(playerId, game)))
                .build())
        .button(27, standard.refresh())
        .button(
            31,
            GuiButton.builder()
                .itemKey("gui.back")
                .onLeftClick(context -> context.open(list(playerId)))
                .build())
        .button(35, standard.close())
        .build();
  }

  private Gui stopConfirmation(UUID playerId, GameInstance game) {
    return ConfirmationGui.builder()
        .id("game.admin.stop." + game.id().shortId())
        .title(message("game.gui.stop.title", values(game)))
        .informationKey("game.admin.stop-confirmation")
        .informationPlaceholders(values(game))
        .confirmItemKey("gui.confirm")
        .cancelItemKey("gui.cancel")
        .permission(AdministrativeCommandPolicy.GAME_STOP)
        .onConfirm(
            context -> {
              context.close();
              lobby
                  .stopGame(game.id(), "admin-menu")
                  .whenComplete(
                      (result, failure) ->
                          main(
                              () ->
                                  send(
                                      playerId,
                                      failure == null && result != null && result.successful()
                                          ? "game.stop.success"
                                          : "game.stop.failed",
                                      Map.of(
                                          "code",
                                          failure == null && result != null
                                              ? result.code().name()
                                              : "INTERNAL_ERROR"))));
            })
        .onCancel(context -> context.open(info(playerId, game.id())))
        .build();
  }

  private void join(UUID playerId, GameInstance game) {
    lobby
        .join(game.id(), playerId)
        .whenComplete(
            (result, failure) ->
                main(
                    () -> {
                      if (failure == null && result != null && result.successful())
                        send(playerId, "game.join.success", values(game));
                      else feedback(playerId, result);
                    }));
  }

  private void start(UUID playerId, GameInstance game, boolean force) {
    CountdownOperationResult result = lobby.start(game.id(), force);
    if (result.successful()) send(playerId, "game.command.started", values(game));
    else send(playerId, "game.command.failed", Map.of("code", result.code().name()));
  }

  private int compareGames(GameInstance left, GameInstance right) {
    return left.id().shortId().compareTo(right.id().shortId());
  }

  private Map<String, Object> summaryValues(List<GameInstance> visible) {
    return Map.of(
        "game_count", visible.size(),
        "waiting_count", visible.stream().filter(game -> game.state() == GameState.WAITING).count(),
        "starting_count",
            visible.stream().filter(game -> game.state() == GameState.STARTING).count(),
        "playing_count",
            visible.stream().filter(game -> game.state() == GameState.PLAYING).count());
  }

  private Map<String, Object> values(GameInstance game) {
    var snapshot = game.snapshot(Instant.now());
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("game_id", game.id().shortId());
    values.put("arena_id", snapshot.arenaId());
    values.put("map_id", snapshot.mapTemplateId());
    values.put("state", snapshot.state());
    values.put("players", snapshot.players().size());
    values.put("minimum_players", game.arena().definition().minimumPlayers());
    values.put("maximum_players", game.arena().definition().maximumPlayers());
    values.put("world_name", snapshot.worldName().orElse("-"));
    values.put(
        "countdown",
        countdowns.snapshot(game.id()).map(countdown -> countdown.remainingSeconds()).orElse(0));
    values.put(
        "age_seconds",
        Math.max(0, Duration.between(snapshot.createdAt(), Instant.now()).toSeconds()));
    return Map.copyOf(values);
  }

  private void feedback(UUID playerId, GameJoinResult result) {
    send(
        playerId,
        "game.command.failed",
        Map.of("code", result == null ? "INTERNAL_ERROR" : result.code().name()));
  }

  private void send(UUID playerId, String key, Map<String, ?> values) {
    Player player = Bukkit.getPlayer(playerId);
    if (player == null) return;
    PlaceholderContext.Builder context = PlaceholderContext.builder();
    values.forEach(context::put);
    player.sendMessage(
        configurations
            .language()
            .message(key, configurations.snapshot().plugin().locale(), context.build()));
  }

  private String message(String key, Map<String, ?> values) {
    PlaceholderContext.Builder context = PlaceholderContext.builder();
    values.forEach(context::put);
    return configurations
        .language()
        .message(key, configurations.snapshot().plugin().locale(), context.build());
  }

  private void main(Runnable action) {
    if (Bukkit.isPrimaryThread()) action.run();
    else plugin.getServer().getScheduler().runTask(plugin, action);
  }
}
