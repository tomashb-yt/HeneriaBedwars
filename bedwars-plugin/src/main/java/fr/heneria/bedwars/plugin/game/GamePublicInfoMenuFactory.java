package fr.heneria.bedwars.plugin.game;

import fr.heneria.bedwars.core.config.PlaceholderContext;
import fr.heneria.bedwars.core.game.GameInstance;
import fr.heneria.bedwars.core.game.GameInstanceManager;
import fr.heneria.bedwars.core.game.countdown.GameCountdownService;
import fr.heneria.bedwars.core.gui.Gui;
import fr.heneria.bedwars.core.gui.GuiButton;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import fr.heneria.bedwars.plugin.gui.StandardGuiButtons;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;

/** Public three-row information menu with no route to administrative controls. */
public final class GamePublicInfoMenuFactory {
  private final GameInstanceManager games;
  private final GameCountdownService countdowns;
  private final ConfigurationService configurations;
  private final StandardGuiButtons standard = new StandardGuiButtons();

  public GamePublicInfoMenuFactory(
      GameInstanceManager games,
      GameCountdownService countdowns,
      ConfigurationService configurations) {
    this.games = games;
    this.countdowns = countdowns;
    this.configurations = configurations;
  }

  public Gui create(UUID playerId) {
    GameInstance game = games.byPlayer(playerId).orElse(null);
    if (game == null)
      return Gui.builder()
          .id("game.public.missing")
          .title(message("game.public-info.missing-title", Map.of()))
          .rows(3)
          .fillEmptySlots(true)
          .button(13, GuiButton.builder().itemKey("game.admin.missing").build())
          .button(22, standard.close())
          .build();
    Map<String, Object> values = values(game);
    return Gui.builder()
        .id("game.public.info." + game.id().shortId())
        .title(message("game.public-info.title", values))
        .rows(3)
        .fillEmptySlots(true)
        .button(
            10,
            GuiButton.builder()
                .itemKey("game.public.general")
                .itemPlaceholders(context -> values)
                .build())
        .button(
            13,
            GuiButton.builder()
                .itemKey("game.public.state")
                .itemPlaceholders(context -> values)
                .build())
        .button(
            16,
            GuiButton.builder()
                .itemKey("game.public.players")
                .itemPlaceholders(context -> values)
                .build())
        .button(22, standard.close())
        .build();
  }

  private Map<String, Object> values(GameInstance game) {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("game_id", game.id().shortId());
    values.put("arena_id", game.arena().definition().id().value());
    values.put("arena_name", game.arena().definition().displayName());
    values.put("map_id", game.arena().template().id().value());
    values.put("state", localizedState(game));
    values.put("players", game.playerIds().size());
    values.put("minimum_players", game.arena().definition().minimumPlayers());
    values.put("maximum_players", game.arena().definition().maximumPlayers());
    values.put(
        "countdown",
        countdowns.snapshot(game.id()).map(value -> value.remainingSeconds()).orElse(0));
    values.put("ready", game.playerIds().size() >= game.arena().definition().minimumPlayers());
    values.put("status_message", statusMessage(game));
    values.put(
        "player_names",
        game.playerIds().stream()
            .map(id -> Bukkit.getPlayer(id))
            .map(player -> player == null ? "?" : player.getName())
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .collect(java.util.stream.Collectors.joining(", ")));
    return Map.copyOf(values);
  }

  private String localizedState(GameInstance game) {
    return message(
        "game.state." + game.state().name().toLowerCase(java.util.Locale.ROOT), Map.of());
  }

  private String statusMessage(GameInstance game) {
    if (countdowns.snapshot(game.id()).isPresent())
      return message(
          "game.countdown.seconds",
          Map.of("countdown", countdowns.snapshot(game.id()).orElseThrow().remainingSeconds()));
    return message(
        game.playerIds().size() >= game.arena().definition().minimumPlayers()
            ? "game.status.ready"
            : "game.status.waiting",
        Map.of());
  }

  private String message(String key, Map<String, ?> values) {
    PlaceholderContext.Builder context = PlaceholderContext.builder();
    values.forEach(context::put);
    return configurations
        .language()
        .message(key, configurations.snapshot().plugin().locale(), context.build());
  }
}
