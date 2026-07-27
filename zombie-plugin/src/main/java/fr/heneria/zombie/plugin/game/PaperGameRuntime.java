package fr.heneria.zombie.plugin.game;

import fr.heneria.zombie.core.config.ZombieSettings.GameplayOptions;
import fr.heneria.zombie.core.economy.EconomyPolicy;
import fr.heneria.zombie.core.economy.EconomyService;
import fr.heneria.zombie.core.economy.PurchaseService;
import fr.heneria.zombie.core.economy.RewardService;
import fr.heneria.zombie.core.economy.TransactionService;
import fr.heneria.zombie.core.economy.TransactionType;
import fr.heneria.zombie.core.editor.MapDefinition;
import fr.heneria.zombie.core.editor.MapPublicationService;
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
  private final MapPublicationService publications;
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
      MapPublicationService publications,
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
    this.publications = publications;
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
        (instance.owner().isPresent()
                ? maps.find(instance.mapId())
                : publications.publishedDefinition(instance.mapId()))
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        instance.owner().isPresent()
                            ? "Map non Ã©ditÃ©e"
                            : "Map non publiÃ©e ou en maintenance"));
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
        throw new IllegalStateException("TÃ©lÃ©portation impossible vers le spawn joueur configurÃ©");
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
      end×Þt¶‰žËkºwµçM%Q%=8€˜˜Ñ¥¬€øôÉÕ¹Ñ¥µ”¹¹•áÑÑ¥½¹Ð¤ì4(€€€€€¥¹Ð¹•áÐ€ôÍ¹…ÁÍ¡½Ð¹É½Õ¹ ¤¹½É±Í•Q¡É½Ü ¤¹¹Õµ‰•È ¤€¬€Äì4(€€€€€¥¹Ð•¹•µ¥•Ì€ô4(€€€€€€€€€‘¥™™¥Õ±Ñä¹•¹•µå½Õ¹Ð¡¹•áÐ°…Ñ¥Ù•A±…å•ÉÌ¡ÉÕ¹Ñ¥µ”¤°ÉÕ¹Ñ¥µ”¹…µ”¹½¹™¥ÕÉ…Ñ¥½¸ ¤¤ì4(€€€€€ÉÕ¹Ñ¥µ”¹…µ”¹ÍÑ…ÉÑ9•áÑI½Õ¹¡•¹•µ¥•Ì¤ì4(€€€€€ÉÕ¹Ñ¥µ”¹¹•áÑÑ¥½¹Ð€ô4(€€€€€€€€€Ñ¥¬4(€€€€€€€€€€€€€€¬ÉÕ¹Ñ¥µ”¹…µ”¹½¹™¥ÕÉ…Ñ¥½¸ ¤¹™¥ÉÍÑI½Õ¹‘•±…åM•½¹‘Ì ¤€¨€ÈÁ04(€€€€€€€€€€€€€€¬ÉÕ¹Ñ¥µ”¹…µ”¹½¹™¥ÕÉ…Ñ¥½¸ ¤¹¥¹¥Ñ¥…±MÁ…Ý¹•±…åQ¥­Ì ¤ì4(€€€€€…¹¹½Õ¹”¡Í¹…ÁÍ¡½Ð¹Á±…å•ÉÌ ¤¹­•åM•Ð ¤°€‰…µ”¹É½Õ¹µÍÑ…ÉÐˆ°€‰É½Õ¹ˆ°%¹Ñ••È¹Ñ½MÑÉ¥¹œ¡¹•áÐ¤¤ì4(€€€ô4(€ô4(4(€ÁÉ¥Ù…Ñ”Ù½¥ÍÁ…Ý¸¡IÕ¹Ñ¥µ•MÑ…Ñ”ÉÕ¹Ñ¥µ”¤ì4(€€€ÉÕ¹Ñ¥µ”4(€€€€€€€€¹•¹Ñ¥Ñ¥•Ì4(€€€€€€€€¹•¹ÑÉåM•Ð ¤4(€€€€€€€€¹É•µ½Ù•%˜ 4(€€€€€€€€€€€•¹ÑÉä€´øì4(€€€€€€€€€€€€€¥˜€¡	Õ­­¥Ð¹•Ñ¹Ñ¥Ñä¡•¹ÑÉä¹•Ñ-•ä ¤¤€„ô¹Õ±°¤ì4(€€€€€€€€€€€€€€€É•ÑÕÉ¸™…±Í”ì4(€€€€€€€€€€€€€ô4(€€€€€€€€€€€€€•¹Ñ¥Ñå…µ•Ì¹É•µ½Ù”¡•¹ÑÉä¹•Ñ-•ä ¤¤ì4(€€€€€€€€€€€€€ÉÕ¹Ñ¥µ”¹…µ”¹é½µ‰¥••™•…Ñ•¡¹Õ±°¤ì4(€€€€€€€€€€€€€É•ÑÕÉ¸ÑÉÕ”ì4(€€€€€€€€€€€ô¤ì4(€€€¥˜€¡Ñ¥¬€ðÉÕ¹Ñ¥µ”¹¹•áÑÑ¥½¹Ð¤ì4(€€€€€É•ÑÕÉ¸ì4(€€€ô4(€€€¥¹Ðµ…á¥µÕµ±¥Ù”€ô4(€€€€€€€‘¥™™¥Õ±Ñä¹µ…á¥µÕµ±¥Ù”¡…Ñ¥Ù•A±…å•ÉÌ¡ÉÕ¹Ñ¥µ”¤°ÉÕ¹Ñ¥µ”¹…µ”¹½¹™¥ÕÉ…Ñ¥½¸ ¤¤ì4(€€€¥¹ÐÉ•Í•ÉÙ•€ô4(€€€€€€€ÉÕ¹Ñ¥µ”¹…µ”¹É•Í•ÉÙ•MÁ…Ý¹Ì¡ÉÕ¹Ñ¥µ”¹…µ”¹½¹™¥ÕÉ…Ñ¥½¸ ¤¹‰…Ñ¡M¥é” ¤°µ…á¥µÕµ±¥Ù”¤ì4(€€€™½È€¡¥¹Ð¥¹‘•à€ô€Àì¥¹‘•à€ðÉ•Í•ÉÙ•ì¥¹‘•à¬¬¤ì4(€€€€€5…Á•™¥¹¥Ñ¥½¸¹i½µ‰¥•MÁ…Ý¸Í½ÕÉ”€ôÍ•±•ÑMÁ…Ý¸¡ÉÕ¹Ñ¥µ”¤¹½É±Í”¡¹Õ±°¤ì4(€€€€€¥˜€¡Í½ÕÉ”€ôô¹Õ±°¤ì4(€€€€€€€ÉÕ¹Ñ¥µ”¹…µ”¹ÍÁ…Ý¹…¥±• ¤ì4(€€€€€€€½¹Ñ¥¹Õ”ì4(€€€€€ô4(€€€€€¥¹ÐÉ½Õ¹€ôÉÕ¹Ñ¥µ”¹…µ”¹Í¹…ÁÍ¡½Ð ¤¹É½Õ¹ ¤¹½É±Í•Q¡É½Ü ¤¹¹Õµ‰•È ¤ì4(€€€€€=ÁÑ¥½¹…°ñUU%øÍÁ…Ý¹•€ô4(€€€€€€€€€ÍÁ…Ý¹•È¹ÍÁ…Ý¸ 4(€€€€€€€€€€€€€¹•Üi½µ‰¥•MÁ…Ý¹•È¹MÁ…Ý¹I•ÅÕ•ÍÐ 4(€€€€€€€€€€€€€€€€€ÉÕ¹Ñ¥µ”¹…µ”¹¥ ¤°4(€€€€€€€€€€€€€€€€€É½Õ¹°4(€€€€€€€€€€€€€€€€€ÉÕ¹Ñ¥µ”¹Ý½É±‘9…µ”°4(€€€€€€€€€€€€€€€€€Í½ÕÉ”¹¥ ¤°4(€€€€€€€€€€€€€€€€€Í½ÕÉ”¹Á½Í¥Ñ¥½¸ ¤°4(€€€€€€€€€€€€€€€€€Í½ÕÉ”¹é½¹” ¤¹¥Í	±…¹¬ ¤€ü=ÁÑ¥½¹…°¹•µÁÑä ¤€è=ÁÑ¥½¹…°¹½˜¡Í½ÕÉ”¹é½¹” ¤¤°4(€€€€€€€€€€€€€€€€€Í½ÕÉ”¹…±±½Ý•‘QåÁ•Ì ¤¤¤ì4(€€€€€¥˜€¡ÍÁ…Ý¹•¹¥ÍAÉ•Í•¹Ð ¤¤ì4(€€€€€€€ÉÕ¹Ñ¥µ”¹…µ”¹ÍÁ…Ý¹• ¤ì4(€€€€€€€ÉÕ¹Ñ¥µ”¹•¹Ñ¥Ñ¥•Ì¹ÁÕÐ¡ÍÁ…Ý¹•¹•Ð ¤°¹•ÜÑ¥Ù•¹Ñ¥Ñä¡É½Õ¹°Í½ÕÉ”¹¥ ¤¤¤ì4(€€€€€€€ÉÕ¹Ñ¥µ”¹…±¥Ù•	åMÁ…Ý¸¹µ•É”¡Í½ÕÉ”¹¥ ¤°€Ä°5…Ñ èé…‘‘á…Ð¤ì4(€€€€€€€ÉÕ¹Ñ¥µ”¹±…ÍÑMÁ…Ý¹Q¥¬¹ÁÕÐ¡Í½ÕÉ”¹¥ ¤°Ñ¥¬¤ì4(€€€€€€€•¹Ñ¥Ñå…µ•Ì¹ÁÕÐ¡ÍÁ…Ý¹•¹•Ð ¤°ÉÕ¹Ñ¥µ”¹…µ”¹¥ ¤¤ì4(€€€€€ô•±Í”ì4(€€€€€€€ÉÕ¹Ñ¥µ”¹…µ”¹ÍÁ…Ý¹…¥±• ¤ì4(€€€€€ô4(€€€ô4(€€€¥¹ÐÉ½Õ¹€ôÉÕ¹Ñ¥µ”¹…µ”¹Í¹…ÁÍ¡½Ð ¤¹É½Õ¹ ¤¹½É±Í•Q¡É½Ü ¤¹¹Õµ‰•È ¤ì4(€€€ÉÕ¹Ñ¥µ”¹¹•áÑÑ¥½¹Ð€ôÑ¥¬€¬‘¥™™¥Õ±Ñä¹ÍÁ…Ý¹•±…åQ¥­Ì¡É½Õ¹°ÉÕ¹Ñ¥µ”¹…µ”¹½¹™¥ÕÉ…Ñ¥½¸ ¤¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”=ÁÑ¥½¹…°ñ5…Á•™¥¹¥Ñ¥½¸¹i½µ‰¥•MÁ…Ý¸øÍ•±•ÑMÁ…Ý¸¡IÕ¹Ñ¥µ•MÑ…Ñ”ÉÕ¹Ñ¥µ”¤ì4(€€€¥¹ÐÉ½Õ¹€ôÉÕ¹Ñ¥µ”¹…µ”¹Í¹…ÁÍ¡½Ð ¤¹É½Õ¹ ¤¹½É±Í•Q¡É½Ü ¤¹¹Õµ‰•È ¤ì4(€€€©…Ù„¹ÕÑ¥°¹1¥ÍÐñ5…Á•™¥¹¥Ñ¥½¸¹i½µ‰¥•MÁ…Ý¸ø•±¥¥‰±”€ô4(€€€€€€€ÉÕ¹Ñ¥µ”¹µ…À¹é½µ‰¥•MÁ…Ý¹Ì ¤¹Ù…±Õ•Ì ¤¹ÍÑÉ•…´ ¤4(€€€€€€€€€€€€¹™¥±Ñ•È¡ÍÁ…Ý¸€´øÍÁ…Ý¸¹µ¥¹¥µÕµI½Õ¹ ¤€ðôÉ½Õ¹€˜˜ÍÁ…Ý¸¹µ…á¥µÕµI½Õ¹ ¤€øôÉ½Õ¹¤4(€€€€€€€€€€€€¹™¥±Ñ•È¡ÍÁ…Ý¸€´øÉÕ¹Ñ¥µ”¹…±¥Ù•	åMÁ…Ý¸¹•Ñ=É•™…Õ±Ð¡ÍÁ…Ý¸¹¥ ¤°€À¤€ðÍÁ…Ý¸¹…Á…¥Ñä ¤¤4(€€€€€€€€€€€€¹™¥±Ñ•È 4(€€€€€€€€€€€€€€€ÍÁ…Ý¸€´ø4(€€€€€€€€€€€€€€€€€€€€…ÉÕ¹Ñ¥µ”¹±…ÍÑMÁ…Ý¹Q¥¬¹½¹Ñ…¥¹Í-•ä¡ÍÁ…Ý¸¹¥ ¤¤4(€€€€€€€€€€€€€€€€€€€€€€€ñðÑ¥¬€´ÉÕ¹Ñ¥µ”¹±…ÍÑMÁ…Ý¹Q¥¬¹•Ð¡ÍÁ…Ý¸¹¥ ¤¤€øôÍÁ…Ý¸¹½½±‘½Ý¹Q¥­Ì ¤¤4(€€€€€€€€€€€€¹™¥±Ñ•È¡ÍÁ…Ý¸€´øÙ…±¥‘A±…å•É¥ÍÑ…¹”¡ÉÕ¹Ñ¥µ”°ÍÁ…Ý¸¤¤4(€€€€€€€€€€€€¹Ñ½1¥ÍÐ ¤ì4(€€€‘½Õ‰±”Ñ½Ñ…°€ô•±¥¥‰±”¹ÍÑÉ•…´ ¤¹µ…ÁQ½½Õ‰±”¡5…Á•™¥¹¥Ñ¥½¸¹i½µ‰¥•MÁ…Ý¸èéÝ•¥¡Ð¤¹ÍÕ´ ¤ì4(€€€¥˜€¡Ñ½Ñ…°€ðô€À¤ì4(€€€€€É•ÑÕÉ¸=ÁÑ¥½¹…°¹•µÁÑä ¤ì4(€€€ô4(€€€‘½Õ‰±”ÕÉÍ½È€ô©…Ù„¹ÕÑ¥°¹½¹ÕÉÉ•¹Ð¹Q¡É•…‘1½…±I…¹‘½´¹ÕÉÉ•¹Ð ¤¹¹•áÑ½Õ‰±”¡Ñ½Ñ…°¤ì4(€€€™½È€¡5…Á•™¥¹¥Ñ¥½¸¹i½µ‰¥•MÁ…Ý¸ÍÁ…Ý¸€è•±¥¥‰±”¤ì4(€€€€€ÕÉÍ½È€´ôÍÁ…Ý¸¹Ý•¥¡Ð ¤ì4(€€€€€¥˜€¡ÕÉÍ½È€ð€À¤ì4(€€€€€€€É•ÑÕÉ¸=ÁÑ¥½¹…°¹½˜¡ÍÁ…Ý¸¤ì4(€€€€€ô4(€€€ô4(€€€É•ÑÕÉ¸=ÁÑ¥½¹…°¹½˜¡•±¥¥‰±”¹•Ñ1…ÍÐ ¤¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”ÍÑ…Ñ¥Œ‰½½±•…¸Ù…±¥‘A±…å•É¥ÍÑ…¹” 4(€€€€€IÕ¹Ñ¥µ•MÑ…Ñ”ÉÕ¹Ñ¥µ”°5…Á•™¥¹¥Ñ¥½¸¹i½µ‰¥•MÁ…Ý¸ÍÁ…Ý¸¤ì4(€€€]½É±Ý½É±€ô	Õ­­¥Ð¹•Ñ]½É±¡ÉÕ¹Ñ¥µ”¹Ý½É±‘9…µ”¤ì4(€€€¥˜€¡Ý½É±€ôô¹Õ±°¤ì4(€€€€€É•ÑÕÉ¸™…±Í”ì4(€€€ô4(€€€1½…Ñ¥½¸±½…Ñ¥½¸€ô4(€€€€€€€¹•Ü1½…Ñ¥½¸¡Ý½É±°ÍÁ…Ý¸¹Á½Í¥Ñ¥½¸ ¤¹à ¤°ÍÁ…Ý¸¹Á½Í¥Ñ¥½¸ ¤¹ä ¤°ÍÁ…Ý¸¹Á½Í¥Ñ¥½¸ ¤¹è ¤¤ì4(€€€‘½Õ‰±”µ¥¹¥µÕ´€ôÍÁ…Ý¸¹µ¥¹¥µÕµ¥ÍÑ…¹” ¤€¨ÍÁ…Ý¸¹µ¥¹¥µÕµ¥ÍÑ…¹” ¤ì4(€€€‘½Õ‰±”µ…á¥µÕ´€ôÍÁ…Ý¸¹µ…á¥µÕµ¥ÍÑ…¹” ¤€¨ÍÁ…Ý¸¹µ…á¥µÕµ¥ÍÑ…¹” ¤ì4(€€€É•ÑÕÉ¸ÉÕ¹Ñ¥µ”¹…µ”¹Í¹…ÁÍ¡½Ð ¤¹Á±…å•ÉÌ ¤¹•¹ÑÉåM•Ð ¤¹ÍÑÉ•…´ ¤4(€€€€€€€€¹™¥±Ñ•È¡•¹ÑÉä€´ø•¹ÑÉä¹•ÑY…±Õ” ¤¹ÍÑ…Ñ” ¤€ôô…µ•A±…å•ÉMÑ…Ñ”¹1%Y¤4(€€€€€€€€¹µ…À¡•¹ÑÉä€´ø	Õ­­¥Ð¹•ÑA±…å•È¡•¹ÑÉä¹•Ñ-•ä ¤¤¤4(€€€€€€€€¹™¥±Ñ•È¡©…Ù„¹ÕÑ¥°¹=‰©•ÑÌèé¹½¹9Õ±°¤4(€€€€€€€€¹™¥±Ñ•È¡Á±…å•È€´øÁ±…å•È¹•Ñ]½É± ¤¹•ÅÕ…±Ì¡Ý½É±¤¤4(€€€€€€€€¹µ…ÁQ½½Õ‰±”¡Á±…å•È€´øÁ±…å•È¹•Ñ1½…Ñ¥½¸ ¤¹‘¥ÍÑ…¹•MÅÕ…É•¡±½…Ñ¥½¸¤¤4(€€€€€€€€¹…¹å5…Ñ ¡‘¥ÍÑ…¹”€´ø‘¥ÍÑ…¹”€øôµ¥¹¥µÕ´€˜˜‘¥ÍÑ…¹”€ðôµ…á¥µÕ´¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”Ù½¥ÕÁ‘…Ñ•I•Ù¥Ù•Ì¡IÕ¹Ñ¥µ•MÑ…Ñ”ÉÕ¹Ñ¥µ”¤ì4(€€€ÉÕ¹Ñ¥µ”¹‰±••‘=ÕÐ¹­•åM•Ð ¤¹ÍÑÉ•…´ ¤4(€€€€€€€€¹µ…À¡	Õ­­¥Ðèé•ÑA±…å•È¤4(€€€€€€€€¹™¥±Ñ•È¡©…Ù„¹ÕÑ¥°¹=‰©•ÑÌèé¹½¹9Õ±°¤4(€€€€€€€€¹™½É… ¡Ñ¡¥Ìèé…ÁÁ±å½Ý¹•‘AÉ•Í•¹Ñ…Ñ¥½¸¤ì4(€€€ÉÕ¹Ñ¥µ”4(€€€€€€€€¹‰±••‘=ÕÐ4(€€€€€€€€¹•¹ÑÉåM•Ð ¤4(€€€€€€€€¹É•µ½Ù•%˜ 4(€€€€€€€€€€€•¹ÑÉä€´øì4(€€€€€€€€€€€€€¥˜€¡Ñ¥¬€ð•¹ÑÉä¹•ÑY…±Õ” ¤¤ì4(€€€€€€€€€€€€€€€É•ÑÕÉ¸™…±Í”ì4(€€€€€€€€€€€€€ô4(€€€€€€€€€€€€€ÉÕ¹Ñ¥µ”¹…µ”¹•±¥µ¥¹…Ñ”¡•¹ÑÉä¹•Ñ-•ä ¤¤ì4(€€€€€€€€€€€€€ÉÕ¹Ñ¥µ”¹…µ”¹ÍÁ•Ñ…Ñ”¡•¹ÑÉä¹•Ñ-•ä ¤¤ì4(€€€€€€€€€€€€€A±…å•ÈÁ±…å•È€ô	Õ­­¥Ð¹•ÑA±…å•È¡•¹ÑÉä¹•Ñ-•ä ¤¤ì4(€€€€€€€€€€€€€¥˜€¡Á±…å•È€„ô¹Õ±°¤ì4(€€€€€€€€€€€€€€€±•…É½Ý¹•‘AÉ•Í•¹Ñ…Ñ¥½¸¡Á±…å•È¤ì4(€€€€€€€€€€€€€€€Á±…å•È¹Í•Ñ…µ•5½‘”¡…µ•5½‘”¹MAQQ=H¤ì4(€€€€€€€€€€€€€ô4(€€€€€€€€€€€€€É•ÑÕÉ¸ÑÉÕ”ì4(€€€€€€€€€€€ô¤ì4(€€€ÉÕ¹Ñ¥µ”4(€€€€€€€€¹É•Ù¥Ù•Ì4(€€€€€€€€¹•¹ÑÉåM•Ð ¤4(€€€€€€€€¹É•µ½Ù•%˜ 4(€€€€€€€€€€€•¹ÑÉä€´øì4(€€€€€€€€€€€€€I•Ù¥Ù•ÑÑ•µÁÐ…ÑÑ•µÁÐ€ô•¹ÑÉä¹•ÑY…±Õ” ¤ì4(€€€€€€€€€€€€€A±…å•ÈÑ…É•Ð€ô	Õ­­¥Ð¹•ÑA±…å•È¡•¹ÑÉä¹•Ñ-•ä ¤¤ì4(€€€€€€€€€€€€€A±…å•ÈÉ•Ù¥Ù•È€ô	Õ­­¥Ð¹•ÑA±…å•È¡…ÑÑ•µÁÐ¹É•Ù¥Ù•È¤ì4(€€€€€€€€€€€€€¥˜€¡Ñ…É•Ð€ôô¹Õ±°4(€€€€€€€€€€€€€€€€€ñðÉ•Ù¥Ù•È€ôô¹Õ±°4(€€€€€€€€€€€€€€€€€ñð€…É•Ù¥Ù•È¹¥ÍM¹•…­¥¹œ ¤4(€€€€€€€€€€€€€€€€€ñðÉ•Ù¥Ù•È¹•Ñ1½…Ñ¥½¸ ¤¹‘¥ÍÑ…¹•MÅÕ…É•¡Ñ…É•Ð¹•Ñ1½…Ñ¥½¸ ¤¤€ø€ä¤ì4(€€€€€€€€€€€€€€€É•ÑÕÉ¸ÑÉÕ”ì4(€€€€€€€€€€€€€ô4(€€€€€€€€€€€€€¥˜€¡Ñ¥¬€ð…ÑÑ•µÁÐ¹½µÁ±•Ñ•Ð¤ì4(€€€€€€€€€€€€€€€É•ÑÕÉ¸™…±Í”ì4(€€€€€€€€€€€€€ô4(€€€€€€€€€€€€€¥˜€¡ÉÕ¹Ñ¥µ”¹…µ”¹É•Ù¥Ù”¡•¹ÑÉä¹•Ñ-•ä ¤°…ÑÑ•µÁÐ¹É•Ù¥Ù•È¤¤ì4(€€€€€€€€€€€€€€€ÉÕ¹Ñ¥µ”¹‰±••‘=ÕÐ¹É•µ½Ù”¡•¹ÑÉä¹•Ñ-•ä ¤¤ì4(€€€€€€€€€€€€€€€É•Ý…É‘Ì¹É•Ù¥Ù” 4(€€€€€€€€€€€€€€€€€€€ÉÕ¹Ñ¥µ”¹…µ”¹¥ ¤°4(€€€€€€€€€€€€€€€€€€€…ÑÑ•µÁÐ¹É•Ù¥Ù•È°4(€€€€€€€€€€€€€€€€€€€•¹ÑÉä¹•Ñ-•ä ¤°4(€€€€€€€€€€€€€€€€€€€€‰É•Ù¥Ù”èˆ4(€€€€€€€€€€€€€€€€€€€€€€€€¬ÉÕ¹Ñ¥µ”¹…µ”¹¥ ¤4(€€€€€€€€€€€€€€€€€€€€€€€€¬€ˆèˆ4(€€€€€€€€€€€€€€€€€€€€€€€€¬…ÑÑ•µÁÐ¹É•Ù¥Ù•È4(€€€€€€€€€€€€€€€€€€€€€€€€¬€ˆèˆ4(€€€€€€€€€€€€€€€€€€€€€€€€¬•¹ÑÉä¹•Ñ-•ä ¤4(€€€€€€€€€€€€€€€€€€€€€€€€¬€ˆèˆ4(€€€€€€€€€€€€€€€€€€€€€€€€¬Ñ¥¬¤ì4(€€€€€€€€€€€€€€€Ñ…É•Ð¹Í•Ñ…µ•5½‘”¡…µ•5½‘”¹Y9QUI¤ì4(€€€€€€€€€€€€€€€±•…É½Ý¹•‘AÉ•Í•¹Ñ…Ñ¥½¸¡Ñ…É•Ð¤ì4(€€€€€€€€€€€€€€€Ñ…É•Ð¹Í•Ñ!•…±Ñ  4(€€€€€€€€€€€€€€€€€€€5…Ñ ¹µ¥¸¡µ…á¥µÕµ!•…±Ñ ¡Ñ…É•Ð¤°ÉÕ¹Ñ¥µ”¹…µ”¹½¹™¥ÕÉ…Ñ¥½¸ ¤¹É•Ù¥Ù•!•…±Ñ  ¤¤¤ì4(€€€€€€€€€€€€€ô4(€€€€€€€€€€€€€É•ÑÕÉ¸ÑÉÕ”ì4(€€€€€€€€€€€ô¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”Ù½¥ÕÁ‘…Ñ•	½…É‘Ì¡IÕ¹Ñ¥µ•MÑ…Ñ”ÉÕ¹Ñ¥µ”¤ì4(€€€¥˜€¡Ñ¥¬€”€ÄÀ€„ô€À¤ì4(€€€€€É•ÑÕÉ¸ì4(€€€ô4(€€€i½µ‰¥•…µ”¹M¹…ÁÍ¡½ÐÍ¹…ÁÍ¡½Ð€ôÉÕ¹Ñ¥µ”¹…µ”¹Í¹…ÁÍ¡½Ð ¤ì4(€€€™½È€¡UU%Á±…å•É%€èÍ¹…ÁÍ¡½Ð¹Á±…å•ÉÌ ¤¹­•åM•Ð ¤¤ì4(€€€€€A±…å•ÈÁ±…å•È€ô	Õ­­¥Ð¹•ÑA±…å•È¡Á±…å•É%¤ì4(€€€€€¥˜€¡Á±…å•È€„ô¹Õ±°¤ì4(€€€€€€€±½¹œ‰…±…¹”€ô4(€€€€€€€€€€€•½¹½µ¥•Ì¹Ý…±±•Ð¡ÉÕ¹Ñ¥µ”¹…µ”¹¥ ¤°Á±…å•É%¤¹µ…À¡Ù…±Õ”€´øÙ…±Õ”¹‰…±…¹” ¤¤¹½É±Í” Á0¤ì4(€€€€€€€Í½É•‰½…É‘Ì¹ÕÁ‘…Ñ•…µ” 4(€€€€€€€€€€€Á±…å•È°4(€€€€€€€€€€€Í¹…ÁÍ¡½Ð°4(€€€€€€€€€€€‰…±…¹”°4(€€€€€€€€€€€Á½Ý•ÉUÁÌ¹Á½¥¹Ñ5Õ±Ñ¥Á±¥•È¡ÉÕ¹Ñ¥µ”¹…µ”¹¥ ¤¤°4(€€€€€€€€€€€Á½Ý•ÉUÁÌ¹…Ñ¥Ù”¡ÉÕ¹Ñ¥µ”¹…µ”¹¥ ¤¤¹Í¥é” ¤¤ì4(€€€€€ô4(€€€ô4(€ô4(4(€ÁÉ¥Ù…Ñ”Ù½¥‘…µ…•••‘‰…¬¡A±…å•ÈÁ±…å•È¤ì4(€€€Á±…å•È¹Á±…å!ÕÉÑ¹¥µ…Ñ¥½¸ À¤ì4(€€€Á±…å•È¹Á±…åM½Õ¹¡Á±…å•È¹•Ñ1½…Ñ¥½¸ ¤°½Éœ¹‰Õ­­¥Ð¹M½Õ¹¹9Q%Qe}A1eI}!UIP°€À¸á˜°€À¸å˜¤ì4(€€€Á±…å•È4(€€€€€€€€¹•Ñ]½É± ¤4(€€€€€€€€¹ÍÁ…Ý¹A…ÉÑ¥±” 4(€€€€€€€€€€€½Éœ¹‰Õ­­¥Ð¹A…ÉÑ¥±”¹5}%9%Q=H°4(€€€€€€€€€€€Á±…å•È¹•Ñ1½…Ñ¥½¸ ¤¹…‘ À°€Ä°€À¤°4(€€€€€€€€€€€€Ð°4(€€€€€€€€€€€€À¸ÈÔ°4(€€€€€€€€€€€€À¸ÌÔ°4(€€€€€€€€€€€€À¸ÈÔ°4(€€€€€€€€€€€€À¸ÀÔ¤ì4(€€€Á±…å•È¹Í•¹‘Ñ¥½¹	…È¡µ•ÍÍ…•Ì¹É•¹‘•È ‰…µ”¹‘…µ…”µÉ••¥Ù•ˆ¤¤ì4(€€€½Éœ¹‰Õ­­¥Ð¹ÕÑ¥°¹Y•Ñ½ÈÉ•½¥°€ôÁ±…å•È¹•Ñ1½…Ñ¥½¸ ¤¹•Ñ¥É•Ñ¥½¸ ¤¹µÕ±Ñ¥Á±ä ´À¸ÄØ¤¹Í•Ñd À¸Àà¤ì4(€€€Á±…å•È¹Í•ÑY•±½¥Ñä¡Á±…å•È¹•ÑY•±½¥Ñä ¤¹…‘¡É•½¥°¤¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”Ù½¥…ÁÁ±å½Ý¹•‘AÉ•Í•¹Ñ…Ñ¥½¸¡A±…å•ÈÁ±…å•È¤ì4(€€€Á±…å•È¹Í•ÑMÁÉ¥¹Ñ¥¹œ¡™…±Í”¤ì4(€€€Á±…å•È¹Í•ÑA½Í”¡½Éœ¹‰Õ­­¥Ð¹•¹Ñ¥Ñä¹A½Í”¹M]%55%9°ÑÉÕ”¤ì4(€€€Á±…å•È¹Í•Ñ]…±­MÁ•• À¤ì4(€€€Á±…å•È¹…‘‘A½Ñ¥½¹™™•Ð 4(€€€€€€€¹•Ü½Éœ¹‰Õ­­¥Ð¹Á½Ñ¥½¸¹A½Ñ¥½¹™™•Ð 4(€€€€€€€€€€€½Éœ¹‰Õ­­¥Ð¹Á½Ñ¥½¸¹A½Ñ¥½¹™™•ÑQåÁ”¹M1=]9ML°€ÄÈ°€ÄÀ°™…±Í”°™…±Í”°™…±Í”¤¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”Ù½¥±•…É½Ý¹•‘AÉ•Í•¹Ñ…Ñ¥½¸¡A±…å•ÈÁ±…å•È¤ì4(€€€Á±…å•È¹Í•ÑA½Í”¡½Éœ¹‰Õ­­¥Ð¹•¹Ñ¥Ñä¹A½Í”¹MQ9%9°™…±Í”¤ì4(€€€Á±…å•È¹Í•Ñ]…±­MÁ•• À¸É˜¤ì4(€€€Á±…å•È¹É•µ½Ù•A½Ñ¥½¹™™•Ð¡½Éœ¹‰Õ­­¥Ð¹Á½Ñ¥½¸¹A½Ñ¥½¹™™•ÑQåÁ”¹M1=]9ML¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”Ù½¥™¥¹¥Í ¡IÕ¹Ñ¥µ•MÑ…Ñ”ÉÕ¹Ñ¥µ”¤ì4(€€€ÉÕ¹Ñ¥µ•Ì¹É•µ½Ù”¡ÉÕ¹Ñ¥µ”¹…µ”¹¥ ¤¤ì4(€€€•¹Ñ¥Ñå…µ•Ì¹•¹ÑÉåM•Ð ¤¹É•µ½Ù•%˜¡•¹ÑÉä€´ø•¹ÑÉä¹•ÑY…±Õ” ¤¹•ÅÕ…±Ì¡ÉÕ¹Ñ¥µ”¹…µ”¹¥ ¤¤¤ì4(€€€™½È€¡UU%Á±…å•É%€èÉÕ¹Ñ¥µ”¹…µ”¹Í¹…ÁÍ¡½Ð ¤¹Á±…å•ÉÌ ¤¹­•åM•Ð ¤¤ì4(€€€€€A±…å•ÈÁ±…å•È€ô	Õ­­¥Ð¹•ÑA±…å•È¡Á±…å•É%¤ì4(€€€€€¥˜€¡Á±…å•È€„ô¹Õ±°¤ì4(€€€€€€€½½É‘¥¹…Ñ½È¹±•…Ù”¡Á±…å•È¤ì4(€€€€€ô4(€€€ô4(€€€½½É‘¥¹…Ñ½È¹ÍÑ½À¡ÉÕ¹Ñ¥µ”¹…µ”¹¥ ¤¤ì4(€€€…µ•Ì¹É•µ½Ù”¡ÉÕ¹Ñ¥µ”¹…µ”¹¥ ¤¤ì4(€€€Á…Á•ÉA½Ý•ÉUÁÌ¹±•…È¡ÉÕ¹Ñ¥µ”¹…µ”¹¥ ¤¤ì4(€€€É•Ý…É‘Ì¹±•…È¡ÉÕ¹Ñ¥µ”¹…µ”¹¥ ¤¤ì4(€€€ÁÕÉ¡…Í•Ì¹É•µ½Ù•…µ”¡ÉÕ¹Ñ¥µ”¹…µ”¹¥ ¤¤ì4(€€€•½¹½µ¥•Ì¹É•µ½Ù”¡ÉÕ¹Ñ¥µ”¹…µ”¹¥ ¤¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”5…ÀñUU%°™È¹¡•¹•É¥„¹é½µ‰¥”¹½É”¹…µ”¹…µ•I•ÍÕ±Ð¹½¹½µåA±…å•ÉI•ÍÕ±Ðø•½¹½µåI•ÍÕ±Ð 4(€€€€€UU%…µ•%¤ì4(€€€Ù…È•½¹½µä€ô•½¹½µ¥•Ì¹™¥¹¡…µ•%¤¹½É±Í”¡¹Õ±°¤ì4(€€€¥˜€¡•½¹½µä€ôô¹Õ±°¤ì4(€€€€€É•ÑÕÉ¸5…À¹½˜ ¤ì4(€€€ô4(€€€1¥¹­•‘!…Í¡5…ÀñUU%°™È¹¡•¹•É¥„¹é½µ‰¥”¹½É”¹…µ”¹…µ•I•ÍÕ±Ð¹½¹½µåA±…å•ÉI•ÍÕ±ÐøÙ…±Õ•Ì€ô4(€€€€€€€¹•Ü1¥¹­•‘!…Í¡5…Àðø ¤ì4(€€€•½¹½µä4(€€€€€€€€¹Í¹…ÁÍ¡½Ð ¤4(€€€€€€€€¹Ý…±±•ÑÌ ¤4(€€€€€€€€¹™½É…  4(€€€€€€€€€€€€¡Á±…å•É%°Ý…±±•Ð¤€´øì4(€€€€€€€€€€€€€Ù…È¡¥ÍÑ½Éä€ô•½¹½µä¹¡¥ÍÑ½Éä¡Á±…å•É%¤ì4(€€€€€€€€€€€€€±½¹œÁÕÉ¡…Í•Ì€ô4(€€€€€€€€€€€€€€€€€¡¥ÍÑ½Éä¹ÍÑÉ•…´ ¤¹™¥±Ñ•È¡Ù…±Õ”€´øÙ…±Õ”¹ÑåÁ” ¤€ôôQÉ…¹Í…Ñ¥½¹QåÁ”¹	%P¤¹½Õ¹Ð ¤ì4(€€€€€€€€€€€€€±½¹œ±…É•ÍÐ€ô4(€€€€€€€€€€€€€€€€€¡¥ÍÑ½Éä¹ÍÑÉ•…´ ¤4(€€€€€€€€€€€€€€€€€€€€€€¹™¥±Ñ•È¡Ù…±Õ”€´øÙ…±Õ”¹ÑåÁ” ¤€ôôQÉ…¹Í…Ñ¥½¹QåÁ”¹	%P¤4(€€€€€€€€€€€€€€€€€€€€€€¹µ…ÁQ½1½¹œ¡Ù…±Õ”€´øÙ…±Õ”¹…µ½Õ¹Ð ¤¤4(€€€€€€€€€€€€€€€€€€€€€€¹µ…à ¤4(€€€€€€€€€€€€€€€€€€€€€€¹½É±Í” À¤ì4(€€€€€€€€€€€€€Ù…±Õ•Ì¹ÁÕÐ 4(€€€€€€€€€€€€€€€€€Á±…å•É%°4(€€€€€€€€€€€€€€€€€¹•Ü™È¹¡•¹•É¥„¹é½µ‰¥”¹½É”¹…µ”¹…µ•I•ÍÕ±Ð¹½¹½µåA±…å•ÉI•ÍÕ±Ð 4(€€€€€€€€€€€€€€€€€€€€€Ý…±±•Ð¹Ñ½Ñ…±…É¹• ¤°4(€€€€€€€€€€€€€€€€€€€€€Ý…±±•Ð¹Ñ½Ñ…±MÁ•¹Ð ¤°4(€€€€€€€€€€€€€€€€€€€€€Ý…±±•Ð¹Ñ½Ñ…±I•™Õ¹‘• ¤°4(€€€€€€€€€€€€€€€€€€€€€Ý…±±•Ð¹‰…±…¹” ¤°4(€€€€€€€€€€€€€€€€€€€€€Ý…±±•Ð¹ÑÉ…¹Í…Ñ¥½¹½Õ¹Ð ¤°4(€€€€€€€€€€€€€€€€€€€€€ÁÕÉ¡…Í•Ì°4(€€€€€€€€€€€€€€€€€€€€€±…É•ÍÐ°4(€€€€€€€€€€€€€€€€€€€€€Á…Á•ÉA½Ý•ÉUÁÌ¹½±±•Ñ•¡…µ•%°Á±…å•É%¤¤¤ì4(€€€€€€€€€€€ô¤ì4(€€€É•ÑÕÉ¸5…À¹½Áå=˜¡Ù…±Õ•Ì¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”¥¹Ð…Ñ¥Ù•A±…å•ÉÌ¡IÕ¹Ñ¥µ•MÑ…Ñ”ÉÕ¹Ñ¥µ”¤ì4(€€€É•ÑÕÉ¸€¡¥¹Ð¤4(€€€€€€€ÉÕ¹Ñ¥µ”¹…µ”¹Í¹…ÁÍ¡½Ð ¤¹Á±…å•ÉÌ ¤¹Ù…±Õ•Ì ¤¹ÍÑÉ•…´ ¤4(€€€€€€€€€€€€¹™¥±Ñ•È 4(€€€€€€€€€€€€€€€Á±…å•È€´ø4(€€€€€€€€€€€€€€€€€€€Á±…å•È¹ÍÑ…Ñ” ¤€ôô…µ•A±…å•ÉMÑ…Ñ”¹1%Y4(€€€€€€€€€€€€€€€€€€€€€€€ñðÁ±…å•È¹ÍÑ…Ñ” ¤€ôô…µ•A±…å•ÉMÑ…Ñ”¹]%Q%94(€€€€€€€€€€€€€€€€€€€€€€€ñðÁ±…å•È¹ÍÑ…Ñ” ¤€ôô…µ•A±…å•ÉMÑ…Ñ”¹=]94(€€€€€€€€€€€€€€€€€€€€€€€ñðÁ±…å•È¹ÍÑ…Ñ” ¤€ôô…µ•A±…å•ÉMÑ…Ñ”¹%M=99Q¤4(€€€€€€€€€€€€¹½Õ¹Ð ¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”=ÁÑ¥½¹…°ñIÕ¹Ñ¥µ•MÑ…Ñ”øÉÕ¹Ñ¥µ•½È¡UU%Á±…å•É%¤ì4(€€€É•ÑÕÉ¸…µ•%‘½È¡Á±…å•É%¤¹µ…À¡ÉÕ¹Ñ¥µ•Ìèé•Ð¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”=ÁÑ¥½¹…°ñUU%ø…µ•%‘½È¡UU%Á±…å•É%¤ì4(€€€É•ÑÕÉ¸ÉÕ¹Ñ¥µ•Ì¹Ù…±Õ•Ì ¤¹ÍÑÉ•…´ ¤4(€€€€€€€€¹™¥±Ñ•È¡ÉÕ¹Ñ¥µ”€´øÉÕ¹Ñ¥µ”¹…µ”¹Í¹…ÁÍ¡½Ð ¤¹Á±…å•ÉÌ ¤¹½¹Ñ…¥¹Í-•ä¡Á±…å•É%¤¤4(€€€€€€€€¹µ…À¡ÉÕ¹Ñ¥µ”€´øÉÕ¹Ñ¥µ”¹…µ”¹¥ ¤¤4(€€€€€€€€¹™¥¹‘¥ÉÍÐ ¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”I½Õ¹‘½¹™¥ÕÉ…Ñ¥½¸½¹™¥ÕÉ…Ñ¥½¸ ¤ì4(€€€…µ•Á±…å=ÁÑ¥½¹ÌÙ…±Õ”€ô½¹™¥ÕÉ…Ñ¥½¹Ì¹ÕÉÉ•¹Ð ¤¹Í•ÑÑ¥¹Ì ¤¹…µ•Á±…ä ¤ì4(€€€É•ÑÕÉ¸¹•ÜI½Õ¹‘½¹™¥ÕÉ…Ñ¥½¸ 4(€€€€€€€Ù…±Õ”¹µ¥¹¥µÕµA±…å•ÉÌ ¤°4(€€€€€€€Ù…±Õ”¹½Õ¹Ñ‘½Ý¹M•½¹‘Ì ¤°4(€€€€€€€Ù…±Õ”¹…¹•±½Õ¹Ñ‘½Ý¹]¡•¹%¹ÍÕ™™¥¥•¹Ð ¤°4(€€€€€€€Ù…±Õ”¹©½¥¹%¹AÉ½É•ÍÌ ¤°4(€€€€€€€Ù…±Õ”¹•¹‘MÉ••¹M•½¹‘Ì ¤°4(€€€€€€€Ù…±Õ”¹ÍÑ…ÉÑ¥¹A½¥¹ÑÌ ¤°4(€€€€€€€Ù…±Õ”¹µ…á¥µÕµI½Õ¹ ¤°4(€€€€€€€Ù…±Õ”¹™¥ÉÍÑI½Õ¹‘•±…åM•½¹‘Ì ¤°4(€€€€€€€Ù…±Õ”¹ÑÉ…¹Í¥Ñ¥½¹M•½¹‘Ì ¤°4(€€€€€€€Ù…±Õ”¹•¹•µå	…Í” ¤°4(€€€€€€€Ù…±Õ”¹•¹•µ¥•ÍA•ÉI½Õ¹ ¤°4(€€€€€€€Ù…±Õ”¹Á±…å•É5Õ±Ñ¥Á±¥•È ¤°4(€€€€€€€Ù…±Õ”¹µ¥¹¥µÕµ¹•µ¥•Ì ¤°4(€€€€€€€Ù…±Õ”¹µ…á¥µÕµ¹•µ¥•Ì ¤°4(€€€€€€€Ù…±Õ”¹‰…Í•!•…±Ñ  ¤°4(€€€€€€€Ù…±Õ”¹¡•…±Ñ¡5Õ±Ñ¥Á±¥•È ¤°4(€€€€€€€Ù…±Õ”¹µ…á¥µÕµ!•…±Ñ  ¤°4(€€€€€€€Ù…±Õ”¹µ…á¥µÕµ±¥Ù•	…Í” ¤°4(€€€€€€€Ù…±Õ”¹µ…á¥µÕµ±¥Ù•A•ÉA±…å•È ¤°4(€€€€€€€Ù…±Õ”¹¥¹¥Ñ¥…±MÁ…Ý¹•±…åQ¥­Ì ¤°4(€€€€€€€Ù…±Õ”¹ÍÁ…Ý¹•±…åQ¥­Ì ¤°4(€€€€€€€Ù…±Õ”¹µ¥¹¥µÕµMÁ…Ý¹•±…åQ¥­Ì ¤°4(€€€€€€€Ù…±Õ”¹‰…Ñ¡M¥é” ¤°4(€€€€€€€Ù…±Õ”¹‘½Ý¹•‘¹…‰±• ¤°4(€€€€€€€Ù…±Õ”¹‰±••‘=ÕÑM•½¹‘Ì ¤°4(€€€€€€€Ù…±Õ”¹É•Ù¥Ù•M•½¹‘Ì ¤°4(€€€€€€€Ù…±Õ”¹É•Ù¥Ù•!•…±Ñ  ¤°4(€€€€€€€Ù…±Õ”¹Á½¥¹ÑÍA•É-¥±° ¤¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”ÍÑ…Ñ¥Œ‘½Õ‰±”µ…á¥µÕµ!•…±Ñ ¡A±…å•ÈÁ±…å•È¤ì4(€€€Ù…È…ÑÑÉ¥‰ÕÑ”€ôÁ±…å•È¹•ÑÑÑÉ¥‰ÕÑ”¡A…Á•ÉÑÑÉ¥‰ÕÑ•I•Í½±Ù•È¹µ…á!•…±Ñ  ¤¤ì4(€€€É•ÑÕÉ¸…ÑÑÉ¥‰ÕÑ”€ôô¹Õ±°€ü€ÈÀ¸À€è…ÑÑÉ¥‰ÕÑ”¹•ÑY…±Õ” ¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”ÍÑ…Ñ¥Œ‰½½±•…¸Ñ•±•Á½ÉÑQ½A±…å•ÉMÁ…Ý¸¡IÕ¹Ñ¥µ•MÑ…Ñ”ÉÕ¹Ñ¥µ”°A±…å•ÈÁ±…å•È¤ì4(€€€]½É±Ý½É±€ô	Õ­­¥Ð¹•Ñ]½É±¡ÉÕ¹Ñ¥µ”¹Ý½É±‘9…µ”¤ì4(€€€¥˜€¡Ý½É±€ôô¹Õ±°¤ì4(€€€€€É•ÑÕÉ¸™…±Í”ì4(€€€ô4(€€€Ù…ÈÁ½¥¹Ð€ôÉÕ¹Ñ¥µ”¹µ…À¹Á±…å•ÉMÁ…Ý¸ ¤¹½É±Í•Q¡É½Ü ¤ì4(€€€É•ÑÕÉ¸Á±…å•È¹Ñ•±•Á½ÉÐ 4(€€€€€€€¹•Ü1½…Ñ¥½¸¡Ý½É±°Á½¥¹Ð¹à ¤°Á½¥¹Ð¹ä ¤°Á½¥¹Ð¹è ¤°Á½¥¹Ð¹å…Ü ¤°Á½¥¹Ð¹Á¥Ñ  ¤¤¤ì4(€ô4(4(€ÁÉ¥Ù…Ñ”Ù½¥…¹¹½Õ¹”¡½±±•Ñ¥½¸ñUU%øÁ±…å•ÉÌ°MÑÉ¥¹œ­•ä°MÑÉ¥¹œ¸¸¸Á±…•¡½±‘•ÉÌ¤ì4(€€€½µÁ½¹•¹ÐÑ•áÐ€ôµ•ÍÍ…•Ì¹É•¹‘•È¡­•ä°Á±…•¡½±‘•ÉÌ¤ì4(€€€™½È€¡UU%Á±…å•É%€èÁ±…å•ÉÌ¤ì4(€€€€€A±…å•ÈÁ±…å•È€ô	Õ­­¥Ð¹•ÑA±…å•È¡Á±…å•É%¤ì4(€€€€€¥˜€¡Á±…å•È€„ô¹Õ±°¤ì4(€€€€€€€Á±…å•È¹Í¡½ÝQ¥Ñ±”¡Q¥Ñ±”¹Ñ¥Ñ±”¡Ñ•áÐ°½µÁ½¹•¹Ð¹•µÁÑä ¤¤¤ì4(€€€€€ô4(€€€ô4(€ô4(4(€ÁÉ¥Ù…Ñ”ÍÑ…Ñ¥Œ™¥¹…°±…ÍÌIÕ¹Ñ¥µ•MÑ…Ñ”ì4(€€€ÁÉ¥Ù…Ñ”™¥¹…°i½µ‰¥•…µ”…µ”ì4(€€€ÁÉ¥Ù…Ñ”™¥¹…°5…Á•™¥¹¥Ñ¥½¸µ…Àì4(€€€ÁÉ¥Ù…Ñ”™¥¹…°MÑÉ¥¹œÝ½É±‘9…µ”ì4(€€€ÁÉ¥Ù…Ñ”™¥¹…°5…ÀñUU%°Ñ¥Ù•¹Ñ¥Ñäø•¹Ñ¥Ñ¥•Ì€ô¹•Ü1¥¹­•‘!…Í¡5…Àðø ¤ì4(€€€ÁÉ¥Ù…Ñ”™¥¹…°5…ÀñMÑÉ¥¹œ°%¹Ñ••Èø…±¥Ù•	åMÁ…Ý¸€ô¹•Ü1¥¹­•‘!…Í¡5…Àðø ¤ì4(€€€ÁÉ¥Ù…Ñ”™¥¹…°5…ÀñMÑÉ¥¹œ°1½¹œø±…ÍÑMÁ…Ý¹Q¥¬€ô¹•Ü1¥¹­•‘!…Í¡5…Àðø ¤ì4(€€€ÁÉ¥Ù…Ñ”™¥¹…°5…ÀñUU%°1½¹œø‰±••‘=ÕÐ€ô¹•Ü1¥¹­•‘!…Í¡5…Àðø ¤ì4(€€€ÁÉ¥Ù…Ñ”™¥¹…°5…ÀñUU%°I•Ù¥Ù•ÑÑ•µÁÐøÉ•Ù¥Ù•Ì€ô¹•Ü1¥¹­•‘!…Í¡5…Àðø ¤ì4(€€€ÁÉ¥Ù…Ñ”±½¹œ¹•áÑÑ¥½¹Ðì4(€€€ÁÉ¥Ù…Ñ”±½¹œ•¹‘Ðì4(€€€ÁÉ¥Ù…Ñ”‰½½±•…¸•¹‘¥¹œì4(4(€€€ÁÉ¥Ù…Ñ”IÕ¹Ñ¥µ•MÑ…Ñ”¡i½µ‰¥•…µ”…µ”°5…Á•™¥¹¥Ñ¥½¸µ…À°MÑÉ¥¹œÝ½É±‘9…µ”°±½¹œ¹•áÑÑ¥½¹Ð¤ì4(€€€€€Ñ¡¥Ì¹…µ”€ô…µ”ì4(€€€€€Ñ¡¥Ì¹µ…À€ôµ…Àì4(€€€€€Ñ¡¥Ì¹Ý½É±‘9…µ”€ôÝ½É±‘9…µ”ì4(€€€€€Ñ¡¥Ì¹¹•áÑÑ¥½¹Ð€ô¹•áÑÑ¥½¹Ðì4(€€€ô4(€ô4(4(€ÁÉ¥Ù…Ñ”É•½ÉI•Ù¥Ù•ÑÑ•µÁÐ¡UU%É•Ù¥Ù•È°±½¹œ½µÁ±•Ñ•Ð¤íô4(4(€ÁÉ¥Ù…Ñ”É•½ÉÑ¥Ù•¹Ñ¥Ñä¡¥¹ÐÉ½Õ¹°MÑÉ¥¹œÍÁ…Ý¹%¤íô4)ô4