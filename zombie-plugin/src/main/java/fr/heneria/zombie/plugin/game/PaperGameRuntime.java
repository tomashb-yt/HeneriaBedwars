package fr.heneria.zombie.plugin.game;

import fr.heneria.zombie.core.config.ZombieSettings.GameplayOptions;
import fr.heneria.zombie.core.editor.MapDefinition;
import fr.heneria.zombie.core.editor.MapRegistry;
import fr.heneria.zombie.core.game.GameEndReason;
import fr.heneria.zombie.core.game.GamePlayerState;
import fr.heneria.zombie.core.game.GameResultRepository;
import fr.heneria.zombie.core.game.GameState;
import fr.heneria.zombie.core.game.RoundConfiguration;
import fr.heneria.zombie.core.game.RoundDifficultyCalculator;
import fr.heneria.zombie.core.game.ZombieGame;
import fr.heneria.zombie.core.game.ZombieGameService;
import fr.heneria.zombie.core.game.ZombieSpawner;
import fr.heneria.zombie.core.instance.GameInstanceSnapshot;
import fr.heneria.zombie.plugin.config.ConfigurationManager;
import fr.heneria.zombie.plugin.display.ContextScoreboardService;
import fr.heneria.zombie.plugin.instance.InstanceCoordinator;
import fr.heneria.zombie.plugin.message.MessageService;
import fr.heneria.zombie.plugin.player.PaperAttributeResolver;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

/** One grouped Paper tick driving every isolated game without per-zombie tasks. */
public final class PaperGameRuntime {
  private final ZombieGameService games;
  private final MapRegistry maps;
  private final ConfigurationManager configurations;
  private final InstanceCoordinator coordinator;
  private final ContextScoreboardService scoreboards;
  private final ZombieSpawner spawner;
  private final GameResultRepository results;
  private final RoundDifficultyCalculator difficulty = new RoundDifficultyCalculator();
  private final Logger logger;
  private final MessageService messages;
  private final Map<UUID, RuntimeState> runtimes = new ConcurrentHashMap<>();
  private final Map<UUID, UUID> entityGames = new ConcurrentHashMap<>();
  private long tick;

  public PaperGameRuntime(
      ZombieGameService games,
      MapRegistry maps,
      ConfigurationManager configurations,
      InstanceCoordinator coordinator,
      ContextScoreboardService scoreboards,
      ZombieSpawner spawner,
      GameResultRepository results,
      MessageService messages,
      Logger logger) {
    this.games = games;
    this.maps = maps;
    this.configurations = configurations;
    this.coordinator = coordinator;
    this.scoreboards = scoreboards;
    this.spawner = spawner;
    this.results = results;
    this.messages = messages;
    this.logger = logger;
  }

  public boolean start(UUID instanceId) {
    if (runtimes.containsKey(instanceId)) {
      return false;
    }
    GameInstanceSnapshot instance =
        coordinator.activeInstances().stream()
            .filter(value -> value.id().equals(instanceId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Instance inconnue"));
    MapDefinition map =
        maps.find(instance.mapId()).orElseThrow(() -> new IllegalStateException("Map non Ã©ditÃ©e"));
    if (map.playerSpawn().isEmpty() || map.zombieSpawns().isEmpty()) {
      throw new IllegalStateException("Spawn joueur ou zombie manquant");
    }
    ZombieGame game = games.create(instanceId, instance.mapId(), configuration());
    instance.players().forEach(game::addPlayer);
    game.prepare();
    RuntimeState runtime =
        new RuntimeState(
            game,
            map,
            instance.worldName().orElseThrow(),
            tick + game.configuration().countdownSeconds() * 20L);
    runtimes.put(instanceId, runtime);
    announce(instance.players(), "game.preparing");
    return true;
  }

  public void joined(UUID instanceId, UUID playerId) {
    games.find(instanceId).ifPresent(game -> game.addPlayer(playerId));
  }

  public void left(UUID instanceId, UUID playerId) {
    games
        .find(instanceId)
        .ifPresent(
            game -> {
              game.leave(playerId);
              if (game.defeated()) {
                end(instanceId, GameEndReason.ALL_PLAYERS_LEFT);
              }
            });
  }

  public void left(UUID playerId) {
    gameIdFor(playerId).ifPresent(gameId -> left(gameId, playerId));
  }

  public void disconnected(UUID instanceId, UUID playerId) {
    RuntimeState runtime = runtimes.get(instanceId);
    if (runtime == null) {
      return;
    }
    if (!configurations.current().settings().reconnect().enabled()) {
      runtime.game.leave(playerId);
      return;
    }
    long seconds = configurations.current().settings().reconnect().gracePeriodSeconds();
    runtime.game.disconnect(playerId, java.time.Instant.now().plusSeconds(seconds));
    runtime.bleedOut.remove(playerId);
    runtime.revives.remove(playerId);
    runtime.revives.entrySet().removeIf(entry -> entry.getValue().reviver().equals(playerId));
  }

  public void reconnected(UUID instanceId, UUID playerId) {
    RuntimeState runtime = runtimes.get(instanceId);
    if (runtime != null) {
      runtime.game.reconnect(playerId);
    }
  }

  public void tick() {
    tick++;
    for (RuntimeState runtime : new ArrayList<>(runtimes.values())) {
      try {
        drive(runtime);
      } catch (RuntimeException failure) {
        logger.severe(
            "Game "
                + runtime.game.id()
                + " failed in "
                + runtime.game.snapshot().state()
                + ": "
                + failure.getMessage());
        end(runtime.game.id(), GameEndReason.INSTANCE_ERROR);
      }
    }
  }

  public boolean zombieDefeated(UUID entityId, UUID killerId) {
    UUID gameId = entityGames.remove(entityId);
    if (gameId == null) {
      return false;
    }
    RuntimeState runtime = runtimes.get(gameId);
    if (runtime == null) {
      return false;
    }
    runtime.entities.remove(entityId);
    runtime.game.zombieDefeated(killerId);
    return true;
  }

  public boolean down(Player player) {
    Optional<RuntimeState> runtime = runtimeFor(player.getUniqueId());
    if (runtime.isEmpty() || !runtime.get().game.configuration().downedEnabled()) {
      return false;
    }
    if (!runtime.get().game.down(player.getUniqueId())) {
      return false;
    }
    runtime
        .get()
        .bleedOut
        .put(
            player.getUniqueId(),
            tick + runtime.get().game.configuration().bleedOutSeconds() * 20L);
    player.setHealth(Math.min(maximumHealth(player), 1.0));
    player.sendMessage(messages.render("game.player-downed"));
    return true;
  }

  public boolean beginRevive(Player reviver, Player target) {
    Optional<RuntimeState> runtime = runtimeFor(reviver.getUniqueId());
    if (runtime.isEmpty()
        || !runtime.get().game.id().equals(gameIdFor(target.getUniqueId()).orElse(null))
        || reviver.getLocation().distanceSquared(target.getLocation()) > 9) {
      return false;
    }
    runtime
        .get()
        .revives
        .put(
            target.getUniqueId(),
            new ReviveAttempt(
                reviver.getUniqueId(),
                tick + runtime.get().game.configuration().reviveSeconds() * 20L));
    return true;
  }

  public void end(UUID gameId, GameEndReason reason) {
    RuntimeState runtime = runtimes.get(gameId);
    if (runtime == null || runtime.ending) {
      return;
    }
    runtime.ending = true;
    runtime
        .game
        .end(reason)
        .ifPresent(
            result ->
                results
                    .save(result)
                    .exceptionally(
                        failure -> {
                          logger.severe(
                              "Could not save game result " + gameId + ": " + failure.getMessage());
                          return null;
                        }));
    runtime.endAt = tick + runtime.game.configuration().endScreenSeconds() * 20L;
    runtime.bleedOut.clear();
    runtime.revives.clear();
    announce(runtime.game.snapshot().players().keySet(), "game.ended", "reason", reason.name());
    runtime.entities.forEach((entity, ignored) -> spawner.remove(entity));
    runtime.entities.clear();
  }

  public void shutdown() {
    runtimes.keySet().forEach(id -> end(id, GameEndReason.SERVER_SHUTDOWN));
    runtimes.values().forEach(runtime -> runtime.entities.keySet().forEach(spawner::remove));
    runtimes.clear();
    entityGames.clear();
    games.clear();
  }

  public Collection<ZombieGame.Snapshot> snapshots() {
    return games.snapshots();
  }

  public Optional<ZombieGame.Snapshot> snapshot(UUID id) {
    return games.find(id).map(ZombieGame::snapshot);
  }

  public boolean forceNextRound(UUID id) {
    RuntimeState runtime = runtimes.get(id);
    if (runtime == null || runtime.game.snapshot().state() != GameState.ROUND_TRANSITION) {
      return false;
    }
    runtime.nextActionAt = tick;
    return true;
  }

  public boolean setRound(UUID id, int round) {
    RuntimeState runtime = runtimes.get(id);
    if (runtime == null
        || round <= 0
        || runtime.game.snapshot().state() != GameState.ROUND_TRANSITION) {
      return false;
    }
    int enemies =
        difficulty.enemyCount(round, activePlayers(runtime), runtime.game.configuration());
    runtime.game.startRoundAt(round, enemies);
    runtime.nextActionAt = tick + runtime.game.configuration().initialSpawnDelayTicks();
    return true;
  }

  private void drive(RuntimeState runtime) {
    runtime.game.expireDisconnectedPlayers();
    ZombieGame.Snapshot snapshot = runtime.game.snapshot();
    if (runtime.ending) {
      if (tick >= runtime.endAt) {
        finish(runtime);
      }
      return;
    }
    if (snapshot.state() == GameState.COUNTDOWN) {
      if (activePlayers(runtime) < runtime.game.configuration().minimumPlayers()) {
        if (runtime.game.configuration().cancelCountdownWhenInsufficient()) {
          runtime.game.cancelCountdown();
        }
        return;
      }
      if (tick >= runtime.nextActionAt) {
        int enemies =
            difficulty.enemyCount(1, activePlayers(runtime), runtime.game.configuration());
        runtime.game.start(enemies);
        runtime.nextActionAt = tick + runtime.game.configuration().initialSpawnDelayTicks();
        announce(snapshot.players().keySet(), "game.round-start", "round", "1");
      }
      return;
    }
    if (snapshot.state() == GameState.WAITING_FOR_PLAYERS
        && activePlayers(runtime) >= runtime.game.configuration().minimumPlayers()) {
      runtime.game.prepare();
      runtime.nextActionAt = tick + runtime.game.configuration().countdownSeconds() * 20L;
      return;
    }
    if (snapshot.state() == GameState.ROUND_ACTIVE) {
      spawn(runtime);
      updateRevives(runtime);
      if (runtime.game.defeated()) {
        end(runtime.game.id(), GameEndReason.TEAM_ELIMINATED);
      }
      if (runtime.game.snapshot().state() == GameState.ROUND_TRANSITION) {
        int completed = runtime.game.snapshot().round().orElseThrow().number();
        if (runtime.game.configuration().maximumRound() != -1
            && completed >= runtime.game.configuration().maximumRound()) {
          end(runtime.game.id(), GameEndReason.MAXIMUM_ROUND);
        } else {
          runtime.nextActionAt = tick + runtime.game.configuration().transitionSeconds() * 20L;
        }
      }
      updateBoards(runtime);
      return;
    }
    if (snapshot.state() == GameState.ROUND_TRANSITION && tick >= runtime.nextActionAt) {
      int next = snapshot.round().orElseThrow().number() + 1;
      int enemies =
          difficulty.enemyCount(next, activePlayers(runtime), runtime.game.configuration());
      runtime.game.startNextRound(enemies);
      runtime.nextActionAt =
          tick
              + runtime.game.configuration().firstRoundDelaySeconds() * 20L
              + runtime.game.configuration().initialSpawnDelayTicks();
      announce(snapshot.players().keySet(), "game.round-start", "round", Integer.toString(next));
    }
  }

  private void spawn(RuntimeState runtime) {
    runtime
        .entities
        .entrySet()
        .removeIf(
            entry -> {
              if (Bukkit.getEntity(entry.getKey()) != null) {
                return false;
              }
              entityGames.remove(entry.getKey());
              runtime.game.zombieDefeated(null);
              return true;
            });
    if (tick < runtime.nextActionÛ]u¶‰žËkºwµçY\‹›X^X[

JNÃBˆ™]\›ˆ]šX]HOH[ÈŒŒˆ]šX]K™Ù]˜[YJ
NÃBˆCBƒBˆš]˜]H›ÚY[››Ý[˜ÙJÛÛXÝ[ÛURQˆ^Y\œËÝš[™ÈÙ^KÝš[™Ë‹‹ˆXÙZÛ\œÊHÃBˆÛÛ\Û™[^HY\ÜØYÙ\Ëœ™[™\ŠÙ^KXÙZÛ\œÊNÃBˆ›Üˆ
URQ^Y\’Yˆ^Y\œÊHÃBˆ^Y\ˆ^Y\ˆHZÚÚ]™Ù]^Y\Š^Y\’Y
NÃBˆYˆ
^Y\ˆOH[
HÃBˆ^Y\‹œÚÝÕ]J]K]J^ÛÛ\Û™[™[\J
JJNÃBˆCBˆCBˆCBƒBˆš]˜]HÝ]XÈš[˜[Û\ÜÈ[[YTÝ]HÃBˆš]˜]Hš[˜[›ÛXšYQØ[YHØ[YNÃBˆš]˜]Hš[˜[X\Yš[š][ÛˆX\ÃBˆš]˜]Hš[˜[Ýš[™ÈÛÜ›˜[YNÃBˆš]˜]Hš[˜[X\URQ[YÙ\ˆ[]Y\ÈH™]È[šÙY\ÚX\Š
NÃBˆš]˜]Hš[˜[X\URQÛ™Ïˆ›YYÝ]H™]È[šÙY\ÚX\Š
NÃBˆš]˜]Hš[˜[X\URQ™]š]™P][\ˆ™]š]™\ÈH™]È[šÙY\ÚX\Š
NÃBˆš]˜]HÛ™È™^XÝ[Û]ÃBˆš]˜]HÛ™È[™]ÃBˆš]˜]H›ÛÛX[ˆ[™[™ÎÃBƒBˆš]˜]H[[YTÝ]J›ÛXšYQØ[YHØ[YKX\Yš[š][ÛˆX\Ýš[™ÈÛÜ›˜[YKÛ™È™^XÝ[Û]
HÃBˆ\Ë™Ø[YHHØ[YNÃBˆ\Ë›X\HX\ÃBˆ\ËÛÜ›˜[YHHÛÜ›˜[YNÃBˆ\Ë›™^XÝ[Û]H™^XÝ[Û]ÃBˆCBˆCBƒBˆš]˜]H™XÛÜ™™]š]™P][\
URQ™]š]™\‹Û™ÈÛÛ\]P]
HßCBŸCB