package fr.heneria.zombie.plugin.enemy;

import fr.heneria.zombie.core.editor.MapPoint;
import fr.heneria.zombie.core.enemy.ZombieAttributeCalculator;
import fr.heneria.zombie.core.enemy.ZombieDamageService;
import fr.heneria.zombie.core.enemy.ZombieDamageType;
import fr.heneria.zombie.core.enemy.ZombieDefinition;
import fr.heneria.zombie.core.enemy.ZombieInstance;
import fr.heneria.zombie.core.enemy.ZombieRemovalReason;
import fr.heneria.zombie.core.enemy.ZombieSelectionService;
import fr.heneria.zombie.core.enemy.ZombieState;
import fr.heneria.zombie.core.enemy.ZombieTargetSelector;
import fr.heneria.zombie.core.enemy.ZombieTracker;
import fr.heneria.zombie.core.game.ZombieSpawner;
import fr.heneria.zombie.plugin.player.PaperAttributeResolver;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Logger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Paper boundary for factory, targeting, native ground navigation, attacks, abilities and cleanup.
 *
 * <p>All methods are invoked by the single grouped game tick on the server thread. No task is
 * created per entity.
 */
public final class PaperZombieEngine implements ZombieSpawner {
  private static final int MAXIMUM_UPDATES_PER_TICK = 200;
  private final ZombieDefinitionLoader definitions;
  private final ZombieTracker tracker = new ZombieTracker();
  private final ZombieAttributeCalculator attributes = new ZombieAttributeCalculator();
  private final ZombieSelectionService selector = new ZombieSelectionService();
  private final ZombieDamageService damageService = new ZombieDamageService();
  private final ZombieTargetSelector targetSelector = new ZombieTargetSelector();
  private final Function<UUID, Collection<UUID>> gamePlayers;
  private final BiFunction<UUID, UUID, Boolean> targetable;
  private final PlayerDamageGateway playerDamage;
  private final RemovalGateway removals;
  private final ZombieAbilityService abilities;
  private final Logger logger;
  private final NamespacedKey markerKey;
  private final NamespacedKey internalKey;
  private final NamespacedKey typeKey;
  private final NamespacedKey gameKey;
  private final NamespacedKey roundKey;
  private int updateCursor;

  public PaperZombieEngine(
      JavaPlugin plugin,
      ZombieDefinitionLoader definitions,
      Function<UUID, Collection<UUID>> gamePlayers,
      BiFunction<UUID, UUID, Boolean> targetable,
      PlayerDamageGateway playerDamage,
      RemovalGateway removals) {
    this.definitions = definitions;
    this.gamePlayers = gamePlayers;
    this.targetable = targetable;
    this.playerDamage = playerDamage;
    this.removals = removals;
    this.abilities = new ZombieAbilityService(gamePlayers, targetable, playerDamage);
    this.logger = plugin.getLogger();
    markerKey = new NamespacedKey(plugin, "zombie_engine");
    internalKey = new NamespacedKey(plugin, "zombie_internal_id");
    typeKey = new NamespacedKey(plugin, "zombie_type");
    gameKey = new NamespacedKey(plugin, "zombie_game");
    roundKey = new NamespacedKey(plugin, "zombie_round");
  }

  @Override
  public Optional<UUID> spawn(SpawnRequest request) {
    var world = Bukkit.getWorld(request.worldName());
    int chunkX = ((int) Math.floor(request.point().x())) >> 4;
    int chunkZ = ((int) Math.floor(request.point().z())) >> 4;
    if (world == null || !world.isChunkLoaded(chunkX, chunkZ)) {
      return Optional.empty();
    }
    Set<String> allowed =
        request.allowedTypes().stream()
            .map(String::toLowerCase)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    Collection<ZombieDefinition> candidates =
        definitions.current().all().stream()
            .filter(
                value ->
                    allowed.isEmpty()
                        || allowed.contains(value.id())
                        || allowed.contains(value.category().name().toLowerCase()))
            .toList();
    Map<String, Integer> alive = new HashMap<>();
    tracker
        .game(request.gameId())
        .forEach(zombie -> alive.merge(zombie.definition().id(), 1, Math::addExact));
    Optional<ZombieDefinition> selected =
        selector.select(
            candidates,
            new ZombieSelectionService.SelectionContext(
                request.round(), request.zoneId(), "default", Set.of(), alive),
            bound -> ThreadLocalRandom.current().nextInt(bound));
    if (selected.isEmpty()) {
      return Optional.empty();
    }
    ZombieDefinition definition = selected.get();
    ZombieAttributeCalculator.CalculatedAttributes calculated =
        attributes.calculate(definition, request.round(), 1, 1);
    Location location = location(request.worldName(), request.point());
    if (!location.getBlock().isPassable()
        || !location.clone().add(0, 1, 0).getBlock().isPassable()) {
      return Optional.empty();
    }
    Entity raw = null;
    try {
      raw = world.spawnEntity(location, EntityType.valueOf(definition.entityType()));
      if (!(raw instanceof Mob mob)) {
        raw.remove();
        return Optional.empty();
      }
      UUID internalId = UUID.randomUUID();
      applyEntity(mob, definition, calculated);
      writeMetadata(mob, internalId, request, definition.id());
      ZombieInstance zombie =
          new ZombieInstance(
              internalId,
              mob.getUniqueId(),
              request.gameId(),
              request.round(),
              request.spawnId(),
              request.worldName(),
              definition,
              calculated,
              request.point(),
              Bukkit.getCurrentTick());
      zombie.state(ZombieState.SEARCHING_TARGET);
      tracker.register(zombie);
      return Optional.of(mob.getUniqueId());
    } catch (RuntimeException failure) {
      if (raw != null) {
        raw.remove();
      }
      logger.warning(
          "Zombie spawn failed [game="
              + request.gameId()
              + ", round="
              + request.round()
              + ", type="
              + definition.id()
              + "]: "
              + failure.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public void remove(UUID entityId) {
    tracker
        .findByEntity(entityId)
        .ifPresentOrElse(
            zombie -> remove(zombie, ZombieRemovalReason.GAME_ENDED, null, false),
            () -> {
              Entity entity = Bukkit.getEntity(entityId);
              if (entity != null) {
                entity.remove();
              }
            });
  }

  public boolean damage(
      UUID entityId, UUID attackerId, ZombieDamageType type, double baseDamage, boolean headshot) {
    return damageFromWeapon(entityId, attackerId, type, baseDamage, headshot, null).handled();
  }

  public WeaponDamageResult damageFromWeapon(
      UUID entityId,
      UUID attackerId,
      ZombieDamageType type,
      double baseDamage,
      boolean headshot,
      String weaponId) {
    ZombieInstance zombie = tracker.findByEntity(entityId).orElse(null);
    if (zombie == null) {
      return new WeaponDamageResult(false, 0, false, 0);
    }
    if (attackerId != null && !Boolean.TRUE.equals(targetable.apply(zombie.gameId(), attackerId))) {
      return new WeaponDamageResult(true, 0, false, 0);
    }
    ZombieDamageService.Result result =
        damageService.damage(
            zombie,
            new ZombieDamageService.Request(
                attackerId,
                type,
                baseDamage,
                headshot,
                Optional.ofNullable(weaponId),
                Optional.empty()));
    Entity entity = Bukkit.getEntity(entityId);
    if (entity instanceof LivingEntity living && result.appliedDamage() > 0) {
      living.setHealth(Math.max(0.01, Math.min(living.getHealth(), result.remainingHealth())));
      living
          .getWorld()
          .spawnParticle(Particle.DAMAGE_INDICATOR, living.getEyeLocation(), 2, 0.15, 0.15, 0.15);
    }
    if (result.lethal()) {
      ZombieRemovalReason reason =
          attackerId == null
              ? ZombieRemovalReason.KILLED_BY_ENVIRONMENT
              : ZombieRemovalReason.KILLED_BY_PLAYER;
      int reward =
          result.headshot()
              ? zombie.definition().rewards().pointsOnHeadshotKill()
              : zombie.definition().rewards().pointsOnKill();
      fr.heneria.zombie.core.economy.TransactionReason rewardReason =
          result.headshot()
              ? fr.heneria.zombie.core.economy.TransactionReason.HEADSHOT_KILL
              : type == ZombieDamageType.MELEE
                  ? fr.heneria.zombie.core.economy.TransactionReason.MELEE_KILL
                  : fr.heneria.zombie.core.economy.TransactionReason.ZOMBIE_KILL;
      remove(zombie, reason, attackerId, true, reward, rewardReason);
    }
    return new WeaponDamageResult(
        true,
        result.appliedDamage(),
        result.lethal(),
        result.appliedDamage() > 0 ? zombie.definition().rewards().pointsOnHit() : 0);
  }

  public void tick(long tick) {
    Collection<ZombieInstance> all = tracker.all();
    if (all.isEmpty()) {
      updateCursor = 0;
      return;
    }
    java.util.List<ZombieInstance> ordered = List.copyOf(all);
    int count = Math.min(MAXIMUM_UPDATES_PER_TICK, ordered.size());
    for (int offset = 0; offset < count; offset++) {
      ZombieInstance zombie = ordered.get((updateCursor + offset) % ordered.size());
      update(zombie, tick);
    }
    updateCursor = (updateCursor + count) % ordered.size();
  }

  public Optional<ZombieInstance> find(UUID entityOrInternalId) {
    return tracker.findByEntity(entityOrInternalId).or(() -> tracker.find(entityOrInternalId));
  }

  public Collection<ZombieInstance> game(UUID gameId) {
    return tracker.game(gameId);
  }

  public Collection<ZombieDefinition> types() {
    return definitions.current().all();
  }

  public boolean isValidTarget(UUID entityId, UUID playerId) {
    return tracker
        .findByEntity(entityId)
        .map(zombie -> Boolean.TRUE.equals(targetable.apply(zombie.gameId(), playerId)))
        .orElse(false);
  }

  public void removeAll(UUID gameId, ZombieRemovalReason reason) {
    for (ZombieInstance zombie : List.copyOf(tracker.game(gameId))) {
      remove(zombie, reason, null, false);
    }
  }

  public int activeCount() {
    return tracker.size();
  }

  private void update(ZombieInstance zombie, long tick) {
    Entity raw = Bukkit.getEntity(zombie.entityId());
    if (!(raw instanceof Mob mob) || !raw.isValid()) {
      remove(zombie, ZombieRemovalReason.INVALID_ENTITY, null, false);
      return;
    }
    ZombieInstance.Snapshot snapshot = zombie.snapshot();
    if (snapshot.lastTargetUpdateTick() == Long.MIN_VALUE
        || tick - snapshot.lastTargetUpdateTick()
            >= zombie.definition().behavior().targetUpdateIntervalTicks()) {
      selectTarget(zombie, mob, tick);
    }
    Player target = zombie.snapshot().targetPlayerId().map(Bukkit::getPlayer).orElse(null);
    if (target == null
        || !Boolean.TRUE.equals(targetable.apply(zombie.gameId(), target.getUniqueId()))) {
      zombie.clearTarget(tick);
      mob.setTarget(null);
      zombie.state(ZombieState.SEARCHING_TARGET);
    } else {
      mob.setTarget(target);
      zombie.state(ZombieState.MOVING);
      double range = zombie.definition().behavior().attackRange();
      if (mob.getLocation().distanceSquared(target.getLocation()) <= range * range
          && mob.hasLineOfSight(target)
          && zombie.canAttack(tick)) {
        zombie.state(ZombieState.ATTACKING);
        zombie.attackedAt(tick);
        playerDamage.damage(zombie, target, zombie.attributes().attackDamage());
        abilities.onAttack(zombie, target, tick);
      }
    }
    if (tick % Math.max(1, zombie.definition().navigation().stuckCheckIntervalTicks()) == 0) {
      Location current = mob.getLocation();
      zombie.observedPosition(
          new MapPoint(
              current.getWorld().getName(),
              current.getX(),
              current.getY(),
              current.getZ(),
              current.getYaw(),
              current.getPitch()),
          tick);
      if (zombie.stuck(tick)) {
        recover(zombie, mob);
      }
    }
    if (!zombie.definition().environment().burnInDaylight() && mob.getFireTicks() > 0) {
      mob.setFireTicks(0);
    }
  }

  private void selectTarget(ZombieInstance zombie, Mob mob, long tick) {
    java.util.ArrayList<ZombieTargetSelector.Candidate> candidates = new java.util.ArrayList<>();
    for (UUID playerId : gamePlayers.apply(zombie.gameId())) {
      Player player = Bukkit.getPlayer(playerId);
      if (player == null) {
        continue;
      }
      candidates.add(
          new ZombieTargetSelector.Candidate(
              playerId,
              zombie.gameId(),
              player.getWorld().getName(),
              !player.isDead(),
              Boolean.TRUE.equals(targetable.apply(zombie.gameId(), playerId)),
              player.getGameMode() == org.bukkit.GameMode.SPECTATOR,
              player.getHealth(),
              0,
              player.getWorld().equals(mob.getWorld())
                  ? player.getLocation().distanceSquared(mob.getLocation())
                  : Double.MAX_VALUE,
              zombie.snapshot().lastAttackerId().filter(playerId::equals).isPresent()));
    }
    Optional<ZombieTargetSelector.Candidate> result =
        targetSelector.select(
            zombie.gameId(),
            mob.getWorld().getName(),
            zombie.definition().behavior().targetStrategy(),
            candidates,
            bound -> ThreadLocalRandom.current().nextInt(bound));
    if (result.isEmpty()) {
      zombie.clearTarget(tick);
      mob.setTarget(null);
    } else {
      Player selected = Bukkit.getPlayer(result.get().playerId());
      if (selected == null) {
        zombie.clearTarget(tick);
        mob.setTarget(null);
        return;
      }
      zombie.target(selected.getUniqueId(), tick);
      mob.setTarget(selected);
    }
  }

  private void recover(ZombieInstance zombie, Mob mob) {
    int attempts = zombie.incrementRepathAttempts();
    if (attempts <= zombie.definition().navigation().maximumRepathAttempts()) {
      mob.setTarget(null);
      zombie.clearTarget(Bukkit.getCurrentTick());
      zombie.state(ZombieState.SEARCHING_TARGET);
      return;
    }
    MapPoint spawn = zombie.snapshot().spawnPoint();
    Location target = location(zombie.snapshot().worldName(), spawn);
    if (target.getWorld() != null
        && target.getWorld().isChunkLoaded(target.getBlockX() >> 4, target.getBlockZ() >> 4)) {
      mob.teleport(target);
      zombie.observedPosition(spawn, Bukkit.getCurrentTick());
    } else {
      remove(zombie, ZombieRemovalReason.DESPAWNED_STUCK, null, false);
    }
  }

  private void remove(
      ZombieInstance zombie, ZombieRemovalReason reason, UUID killerId, boolean reward) {
    remove(
        zombie,
        reason,
        killerId,
        reward,
        0,
        fr.heneria.zombie.core.economy.TransactionReason.ZOMBIE_KILL);
  }

  private void remove(
      ZombieInstance zombie,
      ZombieRemovalReason reason,
      UUID killerId,
      boolean reward,
      int points,
      fr.heneria.zombie.core.economy.TransactionReason rewardReason) {
    if (!zombie.claimDeath(reason)) {
      return;
    }
    Entity entity = Bukkit.getEntity(zombie.entityId());
    if (reward && entity != null) {
      abilities.onDeath(zombie, entity.getLocation(), Bukkit.getCurrentTick());
    }
    if (entity != null) {
      entity.remove();
    }
    tracker.unregister(zombie.id());
    zombie.completeRemoval();
    removals.removed(
        zombie,
        reason,
        killerId,
        reward ? points : 0,
        rewardReason,
        entity == null ? null : entity.getLocation());
  }

  private void applyEntity(
      Mob mob,
      ZombieDefinition definition,
      ZombieAttributeCalculator.CalculatedAttributes calculated) {
    mob.setRemoveWhenFarAway(definition.environment().removeWhenFarAway());
    mob.setCanPickupItems(false);
    mob.setPersistent(true);
    mob.setSilent(definition.appearance().silent());
    mob.setGlowing(definition.appearance().glowing());
    mob.customName(MiniMessage.miniMessage().deserialize(definition.displayName()));
    mob.setCustomNameVisible(definition.appearance().customNameVisible());
    if (mob instanceof org.bukkit.entity.Zombie zombie) {
      zombie.setShouldBurnInDay(definition.environment().burnInDaylight());
    }
    if (mob instanceof Ageable ageable) {
      if (definition.appearance().baby()) {
        ageable.setBaby();
      } else {
        ageable.setAdult();
      }
    }
    setAttribute(mob, PaperAttributeResolver.maxHealth(), calculated.maximumHealth());
    setAttribute(mob, PaperAttributeResolver.attackDamage(), calculated.attackDamage());
    setAttribute(mob, PaperAttributeResolver.movementSpeed(), calculated.movementSpeed());
    setAttribute(mob, PaperAttributeResolver.followRange(), calculated.followRange());
    setAttribute(
        mob, PaperAttributeResolver.knockbackResistance(), calculated.knockbackResistance());
    setAttribute(mob, PaperAttributeResolver.attackKnockback(), calculated.attackKnockback());
    mob.setHealth(calculated.maximumHealth());
    applyEquipment(mob.getEquipment(), definition.appearance().equipment());
  }

  private void writeMetadata(Mob mob, UUID internalId, SpawnRequest request, String typeId) {
    var data = mob.getPersistentDataContainer();
    data.set(markerKey, PersistentDataType.BYTE, (byte) 1);
    data.set(internalKey, PersistentDataType.STRING, internalId.toString());
    data.set(typeKey, PersistentDataType.STRING, typeId);
    data.set(gameKey, PersistentDataType.STRING, request.gameId().toString());
    data.set(roundKey, PersistentDataType.INTEGER, request.round());
  }

  private static void applyEquipment(EntityEquipment equipment, Map<String, String> configured) {
    if (equipment == null) {
      return;
    }
    configured.forEach(
        (slot, materialName) -> {
          Material material = Material.matchMaterial(materialName);
          if (material == null) {
            return;
          }
          ItemStack item = new ItemStack(material);
          switch (slot.toLowerCase()) {
            case "head" -> equipment.setHelmet(item, true);
            case "chest" -> equipment.setChestplate(item, true);
            case "legs" -> equipment.setLeggings(item, true);
            case "feet" -> equipment.setBoots(item, true);
            case "main-hand" -> equipment.setItemInMainHand(item, true);
            case "off-hand" -> equipment.setItemInOffHand(item, true);
            default -> {
              // Unknown slots were intentionally ignored by the presentation adapter.
            }
          }
        });
    equipment.setHelmetDropChance(0);
    equipment.setChestplateDropChance(0);
    equipment.setLeggingsDropChance(0);
    equipment.setBootsDropChance(0);
    equipment.setItemInMainHandDropChance(0);
    equipment.setItemInOffHandDropChance(0);
  }

  private static void setAttribute(Mob mob, Attribute attribute, double value) {
    var instance = mob.getAttribute(attribute);
    if (instance != null) {
      instance.setBaseValue(value);
    }
  }

  private static Location location(String worldName, MapPoint point) {
    return new Location(
        Bukkit.getWorld(worldName), point.x(), point.y(), point.z(), point.yaw(), point.pitch());
  }

  @FunctionalInterface
  public interface PlayerDamageGateway {
    void damage(ZombieInstance source, Player target, double amount);
  }

  @FunctionalInterface
  public interface RemovalGateway {
    void removed(
        ZombieInstance zombie,
        ZombieRemovalReason reason,
        UUID killerId,
        int pointsReward,
        fr.heneria.zombie.core.economy.TransactionReason rewardReason,
        Location deathLocation);
  }

  public record WeaponDamageResult(
      boolean handled, double appliedDamage, boolean lethal, int hitPointsReward) {}
}
