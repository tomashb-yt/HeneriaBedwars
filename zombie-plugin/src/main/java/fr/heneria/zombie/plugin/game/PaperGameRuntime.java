package fr.heneria.zombie.plugin.game;

import fr.heneria.zombie.core.config.ZombieSettings.GameplayOptions;
import fr.heneria.zombie.core.economy.EconomyPolicy;
import fr.heneria.zombie.core.economy.EconomyService;
import fr.heneria.zombie.core.economy.PurchaseService;
import fr.heneria.zombie.core.economy.RewardService;
import fr.heneria.zombie.core.economy.TransactionService;
import fr.heneria.zombie.core.economy.TransactionType;
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
import fr.heneria.zombie.plugin.economy.PaperPowerUpService;
import fr.heneria.zombie.plugin.economy.PointDisplayService;
import fr.heneria.zombie.plugin.editor.MapVisualizationService;
import fr.heneria.zombie.plugin.enemy.PaperZombieEngine;
import fr.heneria.zombie.plugin.instance.InstanceCoordinator;
import fr.heneria.zombie.plugin.message.MessageService;
import fr.heneria.zombie.plugin.player.PaperAttributeResolver;
import fr.heneria.zombie.plugin.weapon.PaperWeaponService;
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
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/** One grouped Paper tick driving every isolated game without per-zombie tasks. */
public final class PaperGameRuntime {
  private final ZombieGameService games;
  private final MapRegistry maps;
  private final ConfigurationManager configurations;
  private final InstanceCoordinator coordinator;
  private final ContextScoreboardService scoreboards;
  private final PaperZombieEngine spawner;
  private final PaperWeaponService weapons;
  private final EconomyService economies;
  private final TransactionService transactions;
  private final PurchaseService purchases;
  private final RewardService rewards;
  private final fr.heneria.zombie.core.powerup.PowerUpService powerUps;
  private final PaperPowerUpService paperPowerUps;
  private final PointDisplayService pointDisplay;
  private final MapVisualizationService visualizations;
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
      PaperZombieEngine spawner,
      PaperWeaponService weapons,
      EconomyService economies,
      TransactionService transactions,
      PurchaseService purchases,
      RewardService rewards,
      fr.heneria.zombie.core.powerup.PowerUpService powerUps,
      PaperPowerUpService paperPowerUps,
      PointDisplayService pointDisplay,
      MapVisualizationService visualizations,
      GameResultRepository results,
      MessageService messages,
      Logger logger) {
    this.games = games;
    this.maps = maps;
    this.configurations = configurations;
    this.coordinator = coordinator;
    this.scoreboards = scoreboards;
    this.spawner = spawner;
    this.weapons = weapons;
    this.economies = economies;
    this.transactions = transactions;
    this.purchases = purchases;
    this.rewards = rewards;
    this.powerUps = powerUps;
    this.paperPowerUps = paperPowerUps;
    this.pointDisplay = pointDisplay;
    this.visualizations = visualizations;
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
        maps.find(instance.mapId()).orElseThrow(() -> new IllegalStateException("Map non éditée"));
    if (map.playerSpawn().isEmpty() || map.zombieSpawns().isEmpty()) {
      throw new IllegalStateException("Spawn joueur ou zombie manquant");
    }
    ZombieGame game = games.create(instanceId, instance.mapId(), configuration());
    var economy = configurations.current().settings().economy();
    economies.create(
        instanceId,
        new EconomyPolicy(
            economy.maximumBalance(),
            fr.heneria.zombie.core.economy.OverflowPolicy.valueOf(
                economy.overflowPolicy().toUpperCase(java.util.Locale.ROOT)),
            economy.allowNegativeBalance(),
            economy.maximumHistoryEntries()));
    instance
        .players()
        .forEach(
            playerId -> {
              game.addPlayer(playerId);
              transactions.openWallet(
                  instanceId,
                  playerId,
                  economy.startingPoints(),
                  "starting-points:" + instanceId + ":" + playerId);
            });
    try {
      game.prepare();
    } catch (RuntimeException failure) {
      economies.remove(instanceId);
      games.remove(instanceId);
      throw failure;
    }
    RuntimeState runtime =
        new RuntimeState(
            game,
            map,
            instance.worldName().orElseThrow(),
            tick + game.configuration().countdownSeconds() * 20L);
    runtimes.put(instanceId, runtime);
    World runtimeWorld = Bukkit.getWorld(runtime.worldName);
    if (runtimeWorld == null) {
      runtimes.remove(instanceId);
      games.remove(instanceId);
      economies.remove(instanceId);
      throw new IllegalStateException("Monde de partie introuvable");
    }
    visualizations.materializeRuntime(runtimeWorld, map);
    for (UUID playerId : instance.players()) {
      Player player = Bukkit.getPlayer(playerId);
      if (player != null && !teleportToPlayerSpawn(runtime, player)) {
        runtimes.remove(instanceId);
        games.remove(instanceId);
        economies.remove(instanceId);
        throw new IllegalStateException("Téléportation impossible vers le spawn joueur configuré");
      }
      if (player != null) {
        weapons.equipStarter(instanceId, player, tick);
      }
    }
    announce(instance.players(), "game.preparing");
    return true;
  }

  public void joined(UUID instanceId, UUID playerId) {
    RuntimeState runtime = runtimes.get(instanceId);
    if (runtime == null || !runtime.game.addPlayer(playerId)) {
      return;
    }
    transactions.openWallet(
        instanceId,
        playerId,
        configurations.current().settings().economy().startingPoints(),
        "join-points:" + instanceId + ":" + playerId);
    Player player = Bukkit.getPlayer(playerId);
    if (player != null && !teleportToPlayerSpawn(runtime, player)) {
      runtime.game.leave(playerId);
      logger.warning("Could not teleport joining player " + playerId + " to configured spawn");
    }
    if (player != null) {
      weapons.equipStarter(instanceId, player, tick);
    }
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
    if (runtime == null || !runtime.game.reconnect(playerId)) {
      return;
    }
    Player player = Bukkit.getPlayer(playerId);
    if (player != null && !teleportToPlayerSpawn(runtime, player)) {
      logger.warning("Could not teleport reconnecting player " + playerId + " to configured spawn");
    }
    if (player != null) {
      weapons.synchronizeInventory(player);
    }
  }

  public void tick() {
    tick++;
    spawner.tick(tick);
    weapons.tick(tick);
    paperPowerUps.tick();
    pointDisplay.tick();
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
    return zombieRemoved(
        entityId, killerId, 0, fr.heneria.zombie.core.economy.TransactionReason.ZOMBIE_KILL, null);
  }

  public boolean zombieRemoved(
      UUID entityId,
      UUID killerId,
      int pointsReward,
      fr.heneria.zombie.core.economy.TransactionReason rewardReason,
      Location deathLocation) {
    UUID gameId = entityGames.remove(entityId);
    if (gameId == null) {
      return false;
    }
    RuntimeState runtime = runtimes.get(gameId);
    if (runtime == null) {
      return false;
    }
    ActiveEntity removed = runtime.entities.remove(entityId);
    if (removed != null) {
      runtime.aliveBySpawn.computeIfPresent(
          removed.spawnId(), (ignored, count) -> count <= 1 ? null : count - 1);
    }
    int round = runtime.game.snapshot().round().map(value -> value.number()).orElse(0);
    rewards.kill(
        gameId,
        killerId,
        entityId,
        pointsReward,
        rewardReason,
        "zombie-kill:" + gameId + ":" + entityId);
    runtime.game.zombieDefeated(killerId);
    if (killerId != null && pointsReward > 0) {
      paperPowerUps.zombieDefeated(gameId, round, deathLocation);
    }
    return true;
  }

  public Collection<UUID> playerIds(UUID gameId) {
    RuntimeState runtime = runtimes.get(gameId);
    return runtime == null
        ? java.util.List.of()
        : java.util.List.copyOf(runtime.game.snapshot().players().keySet());
  }

  public Optional<UUID> gameFor(UUID playerId) {
    return gameIdFor(playerId);
  }

  public Optional<MapDefinition> mapFor(UUID gameId) {
    RuntimeState runtime = runtimes.get(gameId);
    return runtime == null ? Optional.empty() : Optional.of(runtime.map);
  }

  public void weaponHit(UUID gameId, UUID playerId, double appliedDamage, boolean headshot) {
    RuntimeState runtime = runtimes.get(gameId);
    if (runtime != null) {
      runtime.game.weaponHit(playerId, appliedDamage, headshot);
    }
  }

  public boolean isTargetable(UUID gameId, UUID playerId) {
    RuntimeState runtime = runtimes.get(gameId);
    if (runtime == null) {
      return false;
    }
    var player = runtime.game.snapshot().players().get(playerId);
    Player online = Bukkit.getPlayer(playerId);
    return player != null
        && online != null
        && online.isOnline()
        && !online.isDead()
        && player.state() == GamePlayerState.ALIVE;
  }

  public boolean canAct(UUID playerId) {
    return runtimeFor(playerId)
        .map(runtime -> runtime.game.snapshot().players().get(playerId))
        .map(player -> player.state() == GamePlayerState.ALIVE)
        .orElse(false);
  }

  public boolean isDowned(UUID playerId) {
    return runtimeFor(playerId)
        .map(runtime -> runtime.game.snapshot().players().get(playerId))
        .map(player -> player.state() == GamePlayerState.DOWNED)
        .orElse(false);
  }

  public void damagePlayer(UUID gameId, Player target, double amount) {
    if (!isTargetable(gameId, target.getUniqueId()) || amount <= 0) {
      return;
    }
    if (amount >= target.getHealth() && down(target)) {
      return;
    }
    target.setHealth(Math.max(0, target.getHealth() - amount));
    damageFeedback(target);
  }

  public boolean down(Player player) {
    Optional<RuntimeState> runtime = runtimeFor(player.getUniqueId());
    if (runtime.isEmpty() || !runtime.get().game.configuration().downedEnabled()) {
      return false;
    }
    if (!runtime.get().game.hasLivingTeammate(player.getUniqueId())) {
      runtime.get().game.eliminate(player.getUniqueId());
      runtime.get().game.spectate(player.getUniqueId());
      player.setGameMode(GameMode.SPECTATOR);
      player.sendMessage(messages.render("game.player-eliminated-alone"));
      end(runtime.get().game.id(), GameEndReason.TEAM_ELIMINATED);
      return true;
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
    applyDownedPresentation(player);
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
                    .save(result.withEconomy(economyResult(gameId)))
                    .exceptionally(
                        failure -> {
                          logger.severe(
                              "Could not save game result " + gameId + ": " + failure.getMessage());
                          return null;
                        }));
    runtime.endAt = tick + runtime.game.configuration().endScreenSeconds() * 20L;
    runtime.bleedOut.keySet().stream()
        .map(Bukkit::getPlayer)
        .filter(java.util.Objects::nonNull)
        .forEach(this::clearDownedPresentation);
    runtime.bleedOut.clear();
    runtime.revives.clear();
    announce(runtime.game.snapshot().players().keySet(), "game.ended", "reason", reason.name());
    spawner.removeAll(gameId, fr.heneria.zombie.core.enemy.ZombieRemovalReason.GAME_ENDED);
    weapons.removeGame(gameId);
    runtime.entities.clear();
  }

  public void shutdown() {
    runtimes.keySet().forEach(id -> end(id, GameEndReason.SERVER_SHUTDOWN));
    runtimes.values().forEach(runtime -> runtime.entities.keySet().forEach(spawner::remove));
    runtimes.clear();
    entityGames.clear();
    games.clear();
    economies.clear();
    pointDisplay.clear();
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
    if (tick < runtime.nextActionAt) {
      return;
    }
    int maximumAlive =
        difficulty.maximumAlive(activePlayers(runtime), runtime.game.configuration());
    int reserved =
        runtime.game.reserveSpawns(runtime.game.configuration().batchSize(), maximumAlive);
    for (int index = 0; index < reserved; index++) {
      MapDefinition.ZombieSpawn source = selectSpawn(runtime).orElse(null);
      if (source == null) {
        runtime.game.spawnFailed();
        continue;
      }
      int round = runtime.game.snapshot().round().orElseThrow().number();
      Optional<UUID> spawned =
          spawner.spawn(
              new ZombieSpawner.SpawnRequest(
                  runtime.game.id(),
                  round,
                  runtime.worldName,
                  source.id(),
                  source.position(),
                  source.zone().isBlank() ? Optional.empty() : Optional.of(source.zone()),
                  source.allowedTypes()));
      if (spawned.isPresent()) {
        runtime.game.spawned();
        runtime.entities.put(spawned.get(), new ActiveEntity(round, source.id()));
        runtime.aliveBySpawn.merge(source.id(), 1, Math::addExact);
        runtime.lastSpawnTick.put(source.id(), tick);
        entityGames.put(spawned.get(), runtime.game.id());
      } else {
        runtime.game.spawnFailed();
      }
    }
    int round = runtime.game.snapshot().round().orElseThrow().number();
    runtime.nextActionAt = tick + difficulty.spawnDelayTicks(round, runtime.game.configuration());
  }

  private Optional<MapDefinition.ZombieSpawn> selectSpawn(RuntimeState runtime) {
    int round = runtime.game.snapshot().round().orElseThrow().number();
    java.util.List<MapDefinition.ZombieSpawn> eligible =
        runtime.map.zombieSpawns().values().stream()
            .filter(spawn -> spawn.minimumRound() <= round && spawn.maximumRound() >= round)
            .filter(spawn -> runtime.aliveBySpawn.getOrDefault(spawn.id(), 0) < spawn.capacity())
            .filter(
                spawn ->
                    !runtime.lastSpawnTick.containsKey(spawn.id())
                        || tick - runtime.lastSpawnTick.get(spawn.id()) >= spawn.cooldownTicks())
            .filter(spawn -> validPlayerDistance(runtime, spawn))
            .toList();
    double total = eligible.stream().mapToDouble(MapDefinition.ZombieSpawn::weight).sum();
    if (total <= 0) {
      return Optional.empty();
    }
    double cursor = java.util.concurrent.ThreadLocalRandom.current().nextDouble(total);
    for (MapDefinition.ZombieSpawn spawn : eligible) {
      cursor -= spawn.weight();
      if (cursor < 0) {
        return Optional.of(spawn);
      }
    }
    return Optional.of(eligible.getLast());
  }

  private static boolean validPlayerDistance(
      RuntimeState runtime, MapDefinition.ZombieSpawn spawn) {
    World world = Bukkit.getWorld(runtime.worldName);
    if (world == null) {
      return false;
    }
    Location location =
        new Location(world, spawn.position().x(), spawn.position().y(), spawn.position().z());
    double minimum = spawn.minimumDistance() * spawn.minimumDistance();
    double maximum = spawn.maximumDistance() * spawn.maximumDistance();
    return runtime.game.snapshot().players().entrySet().stream()
        .filter(entry -> entry.getValue().state() == GamePlayerState.ALIVE)
        .map(entry -> Bukkit.getPlayer(entry.getKey()))
        .filter(java.util.Objects::nonNull)
        .filter(player -> player.getWorld().equals(world))
        .mapToDouble(player -> player.getLocation().distanceSquared(location))
        .anyMatch(distance -> distance >= minimum && distance <= maximum);
  }

  private void updateRevives(RuntimeState runtime) {
    runtime.bleedOut.keySet().stream()
        .map(Bukkit::getPlayer)
        .filter(java.util.Objects::nonNull)
        .forEach(this::applyDownedPresentation);
    runtime
        .bleedOut
        .entrySet()
        .removeIf(
            entry -> {
              if (tick < entry.getValue()) {
                return false;
              }
              runtime.game.eliminate(entry.getKey());
              runtime.game.spectate(entry.getKey());
              Player player = Bukkit.getPlayer(entry.getKey());
              if (player != null) {
                clearDownedPresentation(player);
                player.setGameMode(GameMode.SPECTATOR);
              }
              return true;
            });
    runtime
        .revives
        .entrySet()
        .removeIf(
            entry -> {
              ReviveAttempt attempt = entry.getValue();
              Player target = Bukkit.getPlayer(entry.getKey());
              Player reviver = Bukkit.getPlayer(attempt.reviver);
              if (target == null
                  || reviver == null
                  || !reviver.isSneaking()
                  || reviver.getLocation().distanceSquared(target.getLocation()) > 9) {
                return true;
              }
              if (tick < attempt.completeAt) {
                return false;
              }
              if (runtime.game.revive(entry.getKey(), attempt.reviver)) {
                runtime.bleedOut.remove(entry.getKey());
                rewards.revive(
                    runtime.game.id(),
                    attempt.reviver,
                    entry.getKey(),
                    "revive:"
                        + runtime.game.id()
                        + ":"
                        + attempt.reviver
                        + ":"
                        + entry.getKey()
                        + ":"
                        + tick);
                target.setGameMode(GameMode.ADVENTURE);
                clearDownedPresentation(target);
                target.setHealth(
                    Math.min(maximumHealth(target), runtime.game.configuration().reviveHealth()));
              }
              return true;
            });
  }

  private void updateBoards(RuntimeState runtime) {
    if (tick % 10 != 0) {
      return;
    }
    ZombieGame.Snapshot snapshot = runtime.game.snapshot();
    for (UUID playerId : snapshot.players().keySet()) {
      Player player = Bukkit.getPlayer(playerId);
      if (player != null) {
        long balance =
            economies.wallet(runtime.game.id(), playerId).map(value -> value.balance()).orElse(0L);
        scoreboards.updateGame(
            player,
            snapshot,
            balance,
            powerUps.pointMultiplier(runtime.game.id()),
            powerUps.active(runtime.game.id()).size());
      }
    }
  }

  private void damageFeedback(Player player) {
    player.playHurtAnimation(0);
    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_HURT, 0.8f, 0.9f);
    player
        .getWorld()
        .spawnParticle(
            org.bukkit.Particle.DAMAGE_INDICATOR,
            player.getLocation().add(0, 1, 0),
            4,
            0.25,
            0.35,
            0.25,
            0.05);
    player.sendActionBar(messages.render("game.damage-received"));
    org.bukkit.util.Vector recoil = player.getLocation().getDirection().multiply(-0.16).setY(0.08);
    player.setVelocity(player.getVelocity().add(recoil));
  }

  private void applyDownedPresentation(Player player) {
    player.setSprinting(false);
    player.setPose(org.bukkit.entity.Pose.SWIMMING, true);
    player.setWalkSpeed(0);
    player.addPotionEffect(
        new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.SLOWNESS, 12, 10, false, false, false));
  }

  private void clearDownedPresentation(Player player) {
    player.setPose(org.bukkit.entity.Pose.STANDING, false);
    player.setWalkSpeed(0.2f);
    player.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS);
  }

  private void finish(RuntimeState runtime) {
    runtimes.remove(runtime.game.id());
    entityGames.entrySet().removeIf(entry -> entry.getValue().equals(runtime.game.id()));
    for (UUID playerId : runtime.game.snapshot().players().keySet()) {
      Player player = Bukkit.getPlayer(playerId);
      if (player != null) {
        coordinator.leave(player);
      }
    }
    coordinator.stop(runtime.game.id());
    games.remove(runtime.game.id());
    paperPowerUps.clear(runtime.game.id());
    rewards.clear(runtime.game.id());
    purchases.removeGame(runtime.game.id());
    economies.remove(runtime.game.id());
  }

  private Map<UUID, fr.heneria.zombie.core.game.GameResult.EconomyPlayerResult> economyResult(
      UUID gameId) {
    var economy = economies.find(gameId).orElse(null);
    if (economy == null) {
      return Map.of();
    }
    LinkedHashMap<UUID, fr.heneria.zombie.core.game.GameResult.EconomyPlayerResult> values =
        new LinkedHashMap<>();
    economy
        .snapshot()
        .wallets()
        .forEach(
            (playerId, wallet) -> {
              var history = economy.history(playerId);
              long purchases =
                  history.stream().filter(value -> value.type() == TransactionType.DEBIT).count();
              long largest =
                  history.stream()
                      .filter(value -> value.type() == TransactionType.DEBIT)
                      .mapToLong(value -> value.amount())
                      .max()
                      .orElse(0);
              values.put(
                  playerId,
                  new fr.heneria.zombie.core.game.GameResult.EconomyPlayerResult(
                      wallet.totalEarned(),
                      wallet.totalSpent(),
                      wallet.totalRefunded(),
                      wallet.balance(),
                      wallet.transactionCount(),
                      purchases,
                      largest,
                      paperPowerUps.collected(gameId, playerId)));
            });
    return Map.copyOf(values);
  }

  private int activePlayers(RuntimeState runtime) {
    return (int)
        runtime.game.snapshot().players().values().stream()
            .filter(
                player ->
                    player.state() == GamePlayerState.ALIVE
                        || player.state() == GamePlayerState.WAITING
                        || player.state() == GamePlayerState.DOWNED
                        || player.state() == GamePlayerState.DISCONNECTED)
            .count();
  }

  private Optional<RuntimeState> runtimeFor(UUID playerId) {
    return gameIdFor(playerId).map(runtimes::get);
  }

  private Optional<UUID> gameIdFor(UUID playerId) {
    return runtimes.values().stream()
        .filter(runtime -> runtime.game.snapshot().players().containsKey(playerId))
        .map(runtime -> runtime.game.id())
        .findFirst();
  }

  private RoundConfiguration configuration() {
    GameplayOptions value = configurations.current().settings().gameplay();
    return new RoundConfiguration(
        value.minimumPlayers(),
        value.countdownSeconds(),
        value.cancelCountdownWhenInsufficient(),
        value.joinInProgress(),
        value.endScreenSeconds(),
        value.startingPoints(),
        value.maximumRound(),
        value.firstRoundDelaySeconds(),
        value.transitionSeconds(),
        value.enemyBase(),
        value.enemiesPerRound(),
        value.playerMultiplier(),
        value.minimumEnemies(),
        value.maximumEnemies(),
        value.baseHealth(),
        value.healthMultiplier(),
        value.maximumHealth(),
        value.maximumAliveBase(),
        value.maximumAlivePerPlayer(),
        value.initialSpawnDelayTicks(),
        value.spawnDelayTicks(),
        value.minimumSpawnDelayTicks(),
        value.batchSize(),
        value.downedEnabled(),
        value.bleedOutSeconds(),
        value.reviveSeconds(),
        value.reviveHealth(),
        value.pointsPerKill());
  }

  private static double maximumHealth(Player player) {
    var attribute = player.getAttribute(PaperAttributeResolver.maxHealth());
    return attribute == null ? 20.0 : attribute.getValue();
  }

  private static boolean teleportToPlayerSpawn(RuntimeState runtime, Player player) {
    World world = Bukkit.getWorld(runtime.worldName);
    if (world == null) {
      return false;
    }
    var point = runtime.map.playerSpawn().orElseThrow();
    return player.teleport(
        new Location(world, point.x(), point.y(), point.z(), point.yaw(), point.pitch()));
  }

  private void announce(Collection<UUID> players, String key, String... placeholders) {
    Component text = messages.render(key, placeholders);
    for (UUID playerId : players) {
      Player player = Bukkit.getPlayer(playerId);
      if (player != null) {
        player.showTitle(Title.title(text, Component.empty()));
      }
    }
  }

  private static final class RuntimeState {
    private final ZombieGame game;
    private final MapDefinition map;
    private final String worldName;
    private final Map<UUID, ActiveEntity> entities = new LinkedHashMap<>();
    private final Map<String, Integer> aliveBySpawn = new LinkedHashMap<>();
    private final Map<String, Long> lastSpawnTick = new LinkedHashMap<>();
    private final Map<UUID, Long> bleedOut = new LinkedHashMap<>();
    private final Map<UUID, ReviveAttempt> revives = new LinkedHashMap<>();
    private long nextActionAt;
    private long endAt;
    private boolean ending;

    private RuntimeState(ZombieGame game, MapDefinition map, String worldName, long nextActionAt) {
      this.game = game;
      this.map = map;
      this.worldName = worldName;
      this.nextActionAt = nextActionAt;
    }
  }

  private record ReviveAttempt(UUID reviver, long completeAt) {}

  private record ActiveEntity(int round, String spawnId) {}
}
