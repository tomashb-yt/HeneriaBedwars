package fr.heneria.bedwars.plugin.game;

import fr.heneria.bedwars.api.game.GameState;
import fr.heneria.bedwars.core.config.PlaceholderContext;
import fr.heneria.bedwars.core.game.BedDestroyCode;
import fr.heneria.bedwars.core.game.BedDestroyResult;
import fr.heneria.bedwars.core.game.CombatTracker;
import fr.heneria.bedwars.core.game.DeathDecision;
import fr.heneria.bedwars.core.game.GameBedService;
import fr.heneria.bedwars.core.game.GameDeathService;
import fr.heneria.bedwars.core.game.GameInstance;
import fr.heneria.bedwars.core.game.GameInstanceManager;
import fr.heneria.bedwars.core.game.GameLocationMapper;
import fr.heneria.bedwars.core.game.RuntimeBlockPosition;
import fr.heneria.bedwars.core.game.RuntimePlayer;
import fr.heneria.bedwars.core.game.item.RuntimeItemAction;
import fr.heneria.bedwars.core.game.item.RuntimeItemActionRegistry;
import fr.heneria.bedwars.core.game.item.RuntimeItemDecision;
import fr.heneria.bedwars.core.game.lobby.GameLobbyService;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;

/** Bukkit boundary for PLAYING-only bed, death and spectator mechanics. */
public final class BukkitGamePlayListener implements Listener {
  private static final Duration KILL_CREDIT = Duration.ofSeconds(10);
  private final JavaPlugin plugin;
  private final ConfigurationService configurations;
  private final GameInstanceManager games;
  private final GameBedService beds;
  private final GameDeathService deaths;
  private final BukkitRuntimePlayerGateway players;
  private final GameLobbyService lobby;
  private final CombatTracker combat;
  private final GameLocationMapper locations = new GameLocationMapper();
  private final RuntimeItemPdc runtimeItems;
  private final RuntimeItemActionRegistry itemActions = new RuntimeItemActionRegistry();

  public BukkitGamePlayListener(
      JavaPlugin plugin,
      ConfigurationService configurations,
      GameInstanceManager games,
      GameBedService beds,
      GameDeathService deaths,
      BukkitRuntimePlayerGateway players,
      GameLobbyService lobby,
      CombatTracker combat) {
    this.plugin = plugin;
    this.configurations = configurations;
    this.games = games;
    this.beds = beds;
    this.deaths = deaths;
    this.players = players;
    this.lobby = lobby;
    this.combat = combat;
    this.runtimeItems = new RuntimeItemPdc(plugin);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onBreak(BlockBreakEvent event) {
    GameInstance game = playing(event.getPlayer()).orElse(null);
    if (game == null || !runtimeWorld(game, event.getBlock().getWorld().getName())) return;
    event.setCancelled(true);
    BedDestroyResult result =
        beds.destroy(event.getPlayer().getUniqueId(), position(event.getBlock()));
    if (result.code() == BedDestroyCode.OWN_BED) {
      event.getPlayer().sendMessage(message("game.bed.own", PlaceholderContext.EMPTY));
      return;
    }
    if (!result.successful()) return;
    var bed = result.bed().orElseThrow();
    event
        .getBlock()
        .getWorld()
        .getBlockAt(bed.foot().x(), bed.foot().y(), bed.foot().z())
        .setType(Material.AIR, false);
    event
        .getBlock()
        .getWorld()
        .getBlockAt(bed.head().x(), bed.head().y(), bed.head().z())
        .setType(Material.AIR, false);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onPlace(BlockPlaceEvent event) {
    if (playing(event.getPlayer()).isPresent()) event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onExplosion(EntityExplodeEvent event) {
    protectBeds(event.getLocation().getWorld().getName(), event.blockList());
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onBlockExplosion(BlockExplodeEvent event) {
    protectBeds(event.getBlock().getWorld().getName(), event.blockList());
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onPistonExtend(BlockPistonExtendEvent event) {
    if (event.getBlocks().stream()
        .anyMatch(
            block -> runtimeBed(block) || runtimeBed(block.getRelative(event.getDirection()))))
      event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onPistonRetract(BlockPistonRetractEvent event) {
    if (event.getBlocks().stream().anyMatch(this::runtimeBed)) event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onFluid(BlockFromToEvent event) {
    if (runtimeBed(event.getToBlock())) event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onBurn(BlockBurnEvent event) {
    if (runtimeBed(event.getBlock())) event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onPhysics(BlockPhysicsEvent event) {
    if (runtimeBed(event.getBlock())) event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    RuntimePlayer runtime = runtime(player).orElse(null);
    if (runtime == null) return;
    if (runtime.spectator() || runtime.protectedAt(Instant.now())) event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onCombat(EntityDamageByEntityEvent event) {
    if (!(event.getEntity() instanceof Player victim)) return;
    Player attacker = attacker(event.getDamager());
    if (attacker == null) return;
    GameInstance game = playing(victim).orElse(null);
    if (game == null || games.byPlayer(attacker.getUniqueId()).filter(game::equals).isEmpty())
      return;
    combat.record(victim.getUniqueId(), attacker.getUniqueId(), Instant.now());
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onSpectatorAttack(EntityDamageByEntityEvent event) {
    Player attacker = attacker(event.getDamager());
    if (attacker == null) return;
    RuntimePlayer attacking = runtime(attacker).orElse(null);
    if (attacking == null) return;
    if (attacking.spectator()) {
      event.setCancelled(true);
      return;
    }
    if (event.getEntity() instanceof Player victim
        && !configurations.snapshot().gameplay().friendlyFire()) {
      RuntimePlayer target = runtime(victim).orElse(null);
      if (target != null
          && attacking.teamId().isPresent()
          && attacking.teamId().equals(target.teamId())) event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onDeath(PlayerDeathEvent event) {
    Player victim = event.getEntity();
    if (playing(victim).isEmpty()) return;
    UUID killer =
        Optional.ofNullable(victim.getKiller())
            .map(Player::getUniqueId)
            .or(() -> combat.attacker(victim.getUniqueId(), Instant.now(), KILL_CREDIT))
            .orElse(null);
    var result = deaths.handle(victim.getUniqueId(), killer);
    if (result.decision() == DeathDecision.IGNORE) return;
    event.setKeepInventory(true);
    event.getDrops().clear();
    event.setDroppedExp(0);
    event.setKeepLevel(true);
    combat.forget(victim.getUniqueId());
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onRespawn(PlayerRespawnEvent event) {
    GameInstance game = games.byPlayer(event.getPlayer().getUniqueId()).orElse(null);
    RuntimePlayer runtime = runtime(event.getPlayer()).orElse(null);
    if (game == null || runtime == null || !runtime.spectator()) return;
    game.world()
        .map(handle -> Bukkit.getWorld(handle.worldName()))
        .ifPresent(
            world -> {
              var target = locations.spectator(game);
              event.setRespawnLocation(
                  new org.bukkit.Location(
                      world, target.x(), target.y(), target.z(), target.yaw(), target.pitch()));
            });
    plugin
        .getServer()
        .getScheduler()
        .runTask(
            plugin,
            () -> {
              if (games
                  .byPlayer(event.getPlayer().getUniqueId())
                  .filter(game::equals)
                  .isPresent()) {
                event.getPlayer().setGameMode(GameMode.SPECTATOR);
                event.getPlayer().getInventory().clear();
                if (runtime.finalDeath())
                  players.prepareSpectatorItems(event.getPlayer(), game.id().toString());
              }
            });
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onInteract(PlayerInteractEvent event) {
    RuntimePlayer runtime = runtime(event.getPlayer()).orElse(null);
    if (runtime == null || !runtime.spectator()) return;
    event.setCancelled(true);
    GameInstance game = games.byPlayer(event.getPlayer().getUniqueId()).orElse(null);
    RuntimeItemPdc.Token token = runtimeItems.read(event.getItem()).orElse(null);
    if (game == null || token == null) return;
    RuntimeItemDecision decision =
        itemActions.evaluate(
            event.getPlayer().getUniqueId(),
            token.actionKey(),
            token.gameId(),
            game.id().toString(),
            game.state(),
            event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND,
            System.currentTimeMillis(),
            configurations.snapshot().game().itemInteractionCooldownMillis());
    if (decision != RuntimeItemDecision.EXECUTE) return;
    RuntimeItemAction action = RuntimeItemAction.find(token.actionKey()).orElseThrow();
    if (action == RuntimeItemAction.SPECTATOR_LEAVE) {
      lobby.leave(event.getPlayer().getUniqueId());
      return;
    }
    if (action == RuntimeItemAction.SPECTATOR_TELEPORTER) {
      Player target =
          game.runtimePlayers().stream()
              .filter(player -> !player.spectator())
              .map(player -> Bukkit.getPlayer(player.playerId()))
              .filter(java.util.Objects::nonNull)
              .findFirst()
              .orElse(null);
      if (target == null) {
        event
            .getPlayer()
            .sendMessage(message("game.spectator.no-targets", PlaceholderContext.EMPTY));
      } else {
        event.getPlayer().teleport(target.getLocation());
        event
            .getPlayer()
            .sendMessage(
                message(
                    "game.spectator.teleported",
                    PlaceholderContext.builder().put("player", target.getName()).build()));
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onPickup(EntityPickupItemEvent event) {
    if (event.getEntity() instanceof Player player
        && runtime(player).filter(RuntimePlayer::spectator).isPresent()) event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMove(PlayerMoveEvent event) {
    if (event.getTo() == null
        || event.getTo().getY() >= configurations.snapshot().gameplay().voidMinimumY()) return;
    RuntimePlayer runtime = runtime(event.getPlayer()).orElse(null);
    if (runtime == null || runtime.spectator() || event.getPlayer().isDead()) return;
    event.getPlayer().setHealth(0);
  }

  public void forget(UUID playerId) {
    combat.forget(playerId);
    itemActions.forget(playerId);
  }

  private void protectBeds(String worldName, java.util.List<org.bukkit.block.Block> blocks) {
    GameInstance game =
        games.all().stream()
            .filter(candidate -> candidate.state() == GameState.PLAYING)
            .filter(candidate -> runtimeWorld(candidate, worldName))
            .findFirst()
            .orElse(null);
    if (game == null) return;
    blocks.removeIf(block -> game.bedAt(position(block)).isPresent());
  }

  private boolean runtimeBed(org.bukkit.block.Block block) {
    return games.all().stream()
        .filter(game -> game.state() == GameState.PLAYING)
        .filter(game -> runtimeWorld(game, block.getWorld().getName()))
        .anyMatch(game -> game.bedAt(position(block)).isPresent());
  }

  private Optional<GameInstance> playing(Player player) {
    return games.byPlayer(player.getUniqueId()).filter(game -> game.state() == GameState.PLAYING);
  }

  private Optional<RuntimePlayer> runtime(Player player) {
    return games.byPlayer(player.getUniqueId()).flatMap(game -> game.player(player.getUniqueId()));
  }

  private static boolean runtimeWorld(GameInstance game, String worldName) {
    return game.world().map(handle -> handle.worldName().equals(worldName)).orElse(false);
  }

  private static RuntimeBlockPosition position(org.bukkit.block.Block block) {
    return new RuntimeBlockPosition(block.getX(), block.getY(), block.getZ());
  }

  private static Player attacker(org.bukkit.entity.Entity damager) {
    if (damager instanceof Player player) return player;
    if (damager instanceof Projectile projectile) {
      ProjectileSource shooter = projectile.getShooter();
      if (shooter instanceof Player player) return player;
    }
    return null;
  }

  private String message(String key, PlaceholderContext context) {
    return configurations
        .language()
        .message(key, configurations.snapshot().plugin().locale(), context);
  }
}
