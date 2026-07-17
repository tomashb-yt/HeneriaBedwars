package fr.heneria.bedwars.plugin.game;

import fr.heneria.bedwars.core.config.PlaceholderContext;
import fr.heneria.bedwars.core.game.GameInstance;
import fr.heneria.bedwars.core.game.GameInstanceManager;
import fr.heneria.bedwars.core.game.countdown.GameCountdownService;
import fr.heneria.bedwars.core.game.lobby.GameLobbyService;
import fr.heneria.bedwars.core.gui.Gui;
import fr.heneria.bedwars.core.gui.GuiButton;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import fr.heneria.bedwars.plugin.gui.StandardGuiButtons;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Public three-row information menu with no route to administrative controls. */
public final class GamePublicInfoMenuFactory {
  private final JavaPlugin plugin;
  private final GameInstanceManager games;
  private final GameCountdownService countdowns;
  private final GameLobbyService lobby;
  private final ConfigurationService configurations;
  private final StandardGuiButtons standard = new StandardGuiButtons();

  public GamePublicInfoMenuFactory(
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
            11,
            GuiButton.builder()
                .itemKey("game.public.info")
                .itemPlaceholders(context -> values)
                .build())
        .button(
            15,
            GuiButton.builder()
                .itemKey("game.public.leave")
                .onLeftClick(
                    context -> {
                      context.close();
                      lobby
                          .leave(playerId)
                          .whenComplete(
                              (result, failure) ->
                                  main(
                                      () ->
                                          feedback(
                                              playerId,
                                              result != null
                                                  && result.successful()
                                                  && failure == null)));
                    })
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
    return Map.copyOf(values);
  }

  private String localizedState(GameInstance game) {
    return message(
        "game.state." + game.state().name().toLowerCase(java.util.Locale.ROOT), Map.of());
  }

  private void feedback(UUID playerId, boolean successful) {
    Player player = Bukkit.getPlayer(playerId);
    if (player != null)
      player.sendMessage(
          message(successful ? "game.leave.success" : "game.leave.not-in-game", Map.of()));
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
