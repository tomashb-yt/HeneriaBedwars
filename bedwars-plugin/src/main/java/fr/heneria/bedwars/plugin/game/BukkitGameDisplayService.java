package fr.heneria.bedwars.plugin.game;

import fr.heneria.bedwars.core.config.GameSettings;
import fr.heneria.bedwars.core.config.PlaceholderContext;
import fr.heneria.bedwars.core.game.GameId;
import fr.heneria.bedwars.core.game.GameInstance;
import fr.heneria.bedwars.core.game.GameInstanceManager;
import fr.heneria.bedwars.core.game.countdown.GameCountdownService;
import fr.heneria.bedwars.core.game.event.GameCountdownAccelerateEvent;
import fr.heneria.bedwars.core.game.event.GameCountdownCancelEvent;
import fr.heneria.bedwars.core.game.event.GameCountdownStartEvent;
import fr.heneria.bedwars.core.game.event.GameCountdownTickEvent;
import fr.heneria.bedwars.core.game.event.GameDestroyEvent;
import fr.heneria.bedwars.core.game.event.GameEvent;
import fr.heneria.bedwars.core.game.event.GameStartEvent;
import fr.heneria.bedwars.core.game.event.PlayerGameJoinEvent;
import fr.heneria.bedwars.core.game.event.PlayerGameLeaveEvent;
import fr.heneria.bedwars.core.logging.ProjectLogger;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/** Main-thread adapter for waiting scoreboards, boss bars, chat, action bars, titles and sounds. */
public final class BukkitGameDisplayService {
  private final ConfigurationService configurations;
  private final GameInstanceManager games;
  private final GameCountdownService countdowns;
  private final BukkitRuntimePlayerGateway players;
  private final ProjectLogger logger;
  private final Map<UUID, Scoreboard> scoreboards = new LinkedHashMap<>();
  private final Map<GameId, BossBar> bossBars = new LinkedHashMap<>();

  public BukkitGameDisplayService(
      ConfigurationService configurations,
      GameInstanceManager games,
      GameCountdownService countdowns,
      BukkitRuntimePlayerGateway players,
      ProjectLogger logger) {
    this.configurations = configurations;
    this.games = games;
    this.countdowns = countdowns;
    this.players = players;
    this.logger = logger;
  }

  /** Handles one already-validated internal event on the Bukkit server thread. */
  public void handle(GameEvent event) {
    try {
      if (event instanceof PlayerGameJoinEvent joined) onJoin(joined);
      else if (event instanceof PlayerGameLeaveEvent left) onLeave(left);
      else if (event instanceof GameCountdownStartEvent started) onCountdownStart(started);
      else if (event instanceof GameCountdownTickEvent ticked) onCountdownTick(ticked);
      else if (event instanceof GameCountdownCancelEvent cancelled) onCountdownCancel(cancelled);
      else if (event instanceof GameCountdownAccelerateEvent accelerated)
        onCountdownAccelerate(accelerated);
      else if (event instanceof GameStartEvent started) onStart(started);
      else if (event instanceof GameDestroyEvent destroyed) removeBossBar(destroyed.gameId());
    } catch (RuntimeException exception) {
      logger.error("[Games] Unable to update waiting-lobby displays", exception);
    }
  }

  public void showInfo(UUID playerId) {
    GameInstance game = games.byPlayer(playerId).orElse(null);
    Player player = Bukkit.getPlayer(playerId);
    if (game == null || player == null) return;
    Map<String, Object> values = values(game);
    player.sendMessage(message("game.scoreboard.map", values));
    player.sendMessage(message("game.scoreboard.players", values));
    player.sendMessage(message("game.scoreboard.minimum", values));
    player.sendMessage(message("game.scoreboard.state", values));
    player.sendMessage(
        countdowns.snapshot(game.id()).isPresent()
            ? message("game.scoreboard.countdown", values)
            : message("game.scoreboard.waiting", values));
  }

  public void refresh(GameInstance game) {
    for (UUID playerId : game.playerIds()) {
      Player player = Bukkit.getPlayer(playerId);
      if (player != null) updateScoreboard(player, game);
    }
  }

  public void clear() {
    bossBars.values().forEach(BossBar::removeAll);
    bossBars.clear();
    scoreboards.clear();
  }

  private void onJoin(PlayerGameJoinEvent event) {
    GameInstance game = games.find(event.gameId()).orElse(null);
    Player player = Bukkit.getPlayer(event.playerId());
    if (game == null || player == null) return;
    player.sendMessage(message("game.join.success", values(game)));
    player.sendTitle(
        message("game.waiting.title", values(game)),
        message("game.waiting.subtitle", values(game)),
        10,
        40,
        10);
    broadcast(game, "game.join.broadcast", Map.of("player", player.getName()));
    BossBar bar = bossBars.get(game.id());
    if (bar != null) bar.addPlayer(player);
    refresh(game);
  }

  private void onLeave(PlayerGameLeaveEvent event) {
    Scoreboard removed = scoreboards.remove(event.playerId());
    Player player = Bukkit.getPlayer(event.playerId());
    if (player != null) bossBars.values().forEach(bar -> bar.removePlayer(player));
    GameInstance game = games.find(event.gameId()).orElse(null);
    if (game != null) refresh(game);
  }

  private void onCountdownStart(GameCountdownStartEvent event) {
    GameInstance game = games.find(event.gameId()).orElse(null);
    if (game == null) return;
    broadcast(game, event.forced() ? "game.countdown.forced" : "game.countdown.started", Map.of());
    GameSettings settings = configurations.snapshot().game();
    if (settings.bossBarEnabled()) {
      BossBar bar =
          Bukkit.createBossBar(
              message("game.countdown.seconds", values(game)),
              BarColor.valueOf(settings.bossBarColor()),
              BarStyle.valueOf(settings.bossBarStyle()));
      for (UUID playerId : game.playerIds()) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) bar.addPlayer(player);
      }
      bossBars.put(game.id(), bar);
    }
    refresh(game);
  }

  private void onCountdownTick(GameCountdownTickEvent event) {
    GameInstance game = games.find(event.gameId()).orElse(null);
    if (game == null) return;
    int remaining = event.remainingSeconds();
    Map<String, Object> extra = Map.of("countdown", remaining);
    GameSettings settings = configurations.snapshot().game();
    if (settings.chatAnnouncementSeconds().contains(remaining))
      broadcast(game, "game.countdown.seconds", extra);
    for (UUID playerId : game.playerIds()) {
      Player player = Bukkit.getPlayer(playerId);
      if (player == null) continue;
      player
          .spigot()
          .sendMessage(
              ChatMessageType.ACTION_BAR,
              TextComponent.fromLegacyText(
                  message("game.countdown.seconds", merge(values(game), extra))));
      if (settings.titleAnnouncementSeconds().contains(remaining)) {
        player.sendTitle(message("game.countdown.final", merge(values(game), extra)), "", 0, 22, 0);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1.2f);
      }
    }
    BossBar bar = bossBars.get(game.id());
    if (bar != null) {
      bar.setTitle(message("game.countdown.seconds", merge(values(game), extra)));
      bar.setProgress(
          Math.max(0, Math.min(1, remaining / (double) Math.max(1, event.initialDuration()))));
    }
    refresh(game);
  }

  private void onCountdownCancel(GameCountdownCancelEvent event) {
    GameInstance game = games.find(event.gameId()).orElse(null);
    removeBossBar(event.gameId());
    if (game != null) {
      broadcast(game, "game.countdown.cancelled", Map.of());
      refresh(game);
    }
  }

  private void onCountdownAccelerate(GameCountdownAccelerateEvent event) {
    GameInstance game = games.find(event.gameId()).orElse(null);
    if (game != null)
      broadcast(game, "game.countdown.accelerated", Map.of("countdown", event.remainingSeconds()));
  }

  private void onStart(GameStartEvent event) {
    GameInstance game = games.find(event.gameId()).orElse(null);
    removeBossBar(event.gameId());
    if (game == null) return;
    for (UUID playerId : game.playerIds()) {
      players.beginPlaying(playerId);
      Player player = Bukkit.getPlayer(playerId);
      if (player == null) continue;
      player.sendTitle(
          message("game.start.title", values(game)),
          message("game.start.subtitle", values(game)),
          5,
          50,
          10);
      player.sendMessage(message("game.start.message", values(game)));
      Scoreboard waiting = scoreboards.remove(playerId);
      if (waiting != null) player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
  }

  private void updateScoreboard(Player player, GameInstance game) {
    if (!configurations.snapshot().game().scoreboardEnabled()) return;
    Scoreboard scoreboard =
        scoreboards.computeIfAbsent(
            player.getUniqueId(), ignored -> Bukkit.getScoreboardManager().getNewScoreboard());
    Objective objective = scoreboard.getObjective("hbw");
    if (objective == null) {
      objective =
          scoreboard.registerNewObjective(
              "hbw", "dummy", message("game.scoreboard.title", values(game)));
      objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }
    for (String entry : scoreboard.getEntries()) scoreboard.resetScores(entry);
    Map<String, Object> values = values(game);
    List<String> lines =
        List.of(
            " ",
            message("game.scoreboard.map", values),
            message("game.scoreboard.players", values),
            message("game.scoreboard.minimum", values),
            message("game.scoreboard.state", values),
            countdowns.snapshot(game.id()).isPresent()
                ? message("game.scoreboard.countdown", values)
                : message("game.scoreboard.waiting", values),
            "  ",
            message("game.scoreboard.footer", values));
    int score = lines.size();
    for (int index = 0; index < lines.size(); index++)
      objective.getScore(lines.get(index) + ChatColor.values()[index]).setScore(score--);
    if (player.getScoreboard() != scoreboard) player.setScoreboard(scoreboard);
  }

  private Map<String, Object> values(GameInstance game) {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("game_id", game.id().shortId());
    values.put("arena_id", game.arena().definition().id().value());
    values.put("arena_name", game.arena().definition().displayName());
    values.put("players", game.playerIds().size());
    values.put("minimum_players", game.arena().definition().minimumPlayers());
    values.put("maximum_players", game.arena().definition().maximumPlayers());
    values.put("state", game.state().name());
    values.put(
        "countdown", countdowns.snapshot(game.id()).map(c -> c.remainingSeconds()).orElse(0));
    values.put("footer", configurations.snapshot().game().scoreboardFooter());
    return Map.copyOf(values);
  }

  private void broadcast(GameInstance game, String key, Map<String, ?> additions) {
    Map<String, Object> values = merge(values(game), additions);
    for (UUID playerId : game.playerIds()) {
      Player player = Bukkit.getPlayer(playerId);
      if (player != null) player.sendMessage(message(key, values));
    }
  }

  private String message(String key, Map<String, ?> values) {
    PlaceholderContext.Builder context = PlaceholderContext.builder();
    values.forEach(context::put);
    return configurations
        .language()
        .message(key, configurations.snapshot().plugin().locale(), context.build());
  }

  private static Map<String, Object> merge(Map<String, ?> base, Map<String, ?> additions) {
    Map<String, Object> values = new LinkedHashMap<>();
    values.putAll(base);
    values.putAll(additions);
    return Map.copyOf(values);
  }

  private void removeBossBar(GameId gameId) {
    BossBar removed = bossBars.remove(gameId);
    if (removed != null) removed.removeAll();
  }
}
