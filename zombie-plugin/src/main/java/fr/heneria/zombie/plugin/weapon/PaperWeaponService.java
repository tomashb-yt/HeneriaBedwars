package fr.heneria.zombie.plugin.weapon;

import fr.heneria.zombie.core.economy.PriceResolver;
import fr.heneria.zombie.core.economy.PurchaseFundingMode;
import fr.heneria.zombie.core.economy.PurchaseRequest;
import fr.heneria.zombie.core.economy.PurchaseResult;
import fr.heneria.zombie.core.economy.PurchaseService;
import fr.heneria.zombie.core.economy.PurchaseType;
import fr.heneria.zombie.core.economy.RewardService;
import fr.heneria.zombie.core.economy.TransactionReason;
import fr.heneria.zombie.core.editor.MapDefinition;
import fr.heneria.zombie.core.editor.MapObjectType;
import fr.heneria.zombie.core.enemy.ZombieInstance;
import fr.heneria.zombie.core.weapon.MysteryBoxSelector;
import fr.heneria.zombie.core.weapon.WeaponDamageCalculator;
import fr.heneria.zombie.core.weapon.WeaponDefinition;
import fr.heneria.zombie.core.weapon.WeaponEvent;
import fr.heneria.zombie.core.weapon.WeaponEventDispatcher;
import fr.heneria.zombie.core.weapon.WeaponInstance;
import fr.heneria.zombie.core.weapon.WeaponService;
import fr.heneria.zombie.core.weapon.WeaponSpreadCalculator;
import fr.heneria.zombie.plugin.enemy.PaperZombieEngine;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Predicate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

/**
 * Paper adapter for weapon items, hitscan fire, reloads and map purchase interactions.
 *
 * <p>It owns no scheduler. Delayed burst, charge and reload operations are processed by the
 * existing grouped game tick.
 */
public final class PaperWeaponService {
  private static final MiniMessage MINI = MiniMessage.miniMessage();
  private final WeaponDefinitionLoader definitions;
  private final PaperZombieEngine zombies;
  private final WeaponService weapons = new WeaponService();
  private final WeaponDamageCalculator damage = new WeaponDamageCalculator();
  private final WeaponSpreadCalculator spread = new WeaponSpreadCalculator();
  private final MysteryBoxSelector mysteryBox = new MysteryBoxSelector();
  private final Function<UUID, Optional<UUID>> playerGame;
  private final Function<UUID, Optional<MapDefinition>> gameMap;
  private final PurchaseService purchases;
  private final RewardService rewards;
  private final Predicate<UUID> instaKill;
  private final Predicate<UUID> canAct;
  private final HitObserver hitObserver;
  private final WeaponEventDispatcher events;
  private final NamespacedKey markerKey;
  private final NamespacedKey instanceKey;
  private final NamespacedKey definitionKey;
  private final List<PendingShot> pendingShots = new ArrayList<>();
  private final Map<UUID, StationAnimation> stationAnimations = new LinkedHashMap<>();

  public PaperWeaponService(
      JavaPlugin plugin,
      WeaponDefinitionLoader definitions,
      PaperZombieEngine zombies,
      Function<UUID, Optional<UUID>> playerGame,
      Function<UUID, Optional<MapDefinition>> gameMap,
      PurchaseService purchases,
      RewardService rewards,
      Predicate<UUID> instaKill,
      Predicate<UUID> canAct,
      HitObserver hitObserver) {
    this.definitions = java.util.Objects.requireNonNull(definitions, "definitions");
    this.zombies = java.util.Objects.requireNonNull(zombies, "zombies");
    this.playerGame = java.util.Objects.requireNonNull(playerGame, "playerGame");
    this.gameMap = java.util.Objects.requireNonNull(gameMap, "gameMap");
    this.purchases = java.util.Objects.requireNonNull(purchases, "purchases");
    this.rewards = java.util.Objects.requireNonNull(rewards, "rewards");
    this.instaKill = java.util.Objects.requireNonNull(instaKill, "instaKill");
    this.canAct = java.util.Objects.requireNonNull(canAct, "canAct");
    this.hitObserver = java.util.Objects.requireNonNull(hitObserver, "hitObserver");
    this.events =
        new WeaponEventDispatcher(
            failure -> plugin.getLogger().warning("Weapon event listener failed: " + failure));
    markerKey = new NamespacedKey(plugin, "weapon_engine");
    instanceKey = new NamespacedKey(plugin, "weapon_instance");
    definitionKey = new NamespacedKey(plugin, "weapon_type");
  }

  public void equipStarter(UUID gameId, Player player, long tick) {
    if (!weapons.player(player.getUniqueId()).isEmpty()) {
      synchronizeInventory(player);
      return;
    }
    definitions
        .current()
        .find("starter_pistol")
        .ifPresent(definition -> give(gameId, player, definition, tick));
  }

  public Optional<WeaponInstance> give(
      UUID gameId, Player player, WeaponDefinition definition, long tick) {
    if (!playerGame.apply(player.getUniqueId()).filter(gameId::equals).isPresent()) {
      return Optional.empty();
    }
    WeaponInstance instance = weapons.create(gameId, player.getUniqueId(), definition, tick);
    ItemStack item = item(instance);
    int slot = firstWeaponSlot(player);
    ItemStack replaced = player.getInventory().getItem(slot);
    find(replaced).ifPresent(value -> weapons.remove(value.id()));
    player.getInventory().setItem(slot, item);
    player.getInventory().setHeldItemSlot(slot);
    play(player, definition.sounds().get("switch"));
    return Optional.of(instance);
  }

  public boolean fire(Player player, long tick) {
    if (!canAct.test(player.getUniqueId())) {
      return false;
    }
    WeaponInstance weapon = current(player).orElse(null);
    if (weapon == null
        || !playerGame.apply(player.getUniqueId()).filter(weapon.gameId()::equals).isPresent()) {
      return false;
    }
    if (!publish(WeaponEvent.Type.PRE_FIRE, weapon, java.util.Map.of())) {
      return true;
    }
    WeaponInstance.FireDecision decision = weapon.tryFire(tick, false);
    if (decision == WeaponInstance.FireDecision.EMPTY) {
      play(player, weapon.definition().sounds().get("empty"));
      beginReload(player, tick);
      return true;
    }
    if (decision != WeaponInstance.FireDecision.FIRED) {
      return true;
    }
    switch (weapon.definition().fire().mode()) {
      case BURST -> {
        fireProjectiles(player, weapon);
        for (int index = 1; index < weapon.definition().fire().burstSize(); index++) {
          pendingShots.add(
              new PendingShot(
                  player.getUniqueId(),
                  weapon.id(),
                  tick + (long) index * weapon.definition().fire().burstIntervalTicks(),
                  true));
        }
      }
      case CHARGE ->
          pendingShots.add(
              new PendingShot(
                  player.getUniqueId(),
                  weapon.id(),
                  tick + weapon.definition().fire().chargeTicks(),
                  false));
      case SEMI_AUTOMATIC, AUTOMATIC, MELEE -> fireProjectiles(player, weapon);
    }
    refreshItem(player, weapon);
    publish(WeaponEvent.Type.FIRED, weapon, java.util.Map.of());
    return true;
  }

  public boolean beginReload(Player player, long tick) {
    if (!canAct.test(player.getUniqueId())) {
      return false;
    }
    WeaponInstance weapon = current(player).orElse(null);
    if (weapon == null
        || !publish(WeaponEvent.Type.PRE_RELOAD, weapon, java.util.Map.of())
        || !weapon.beginReload(tick)) {
      return false;
    }
    play(player, weapon.definition().sounds().get("reload"));
    refreshItem(player, weapon);
    publish(WeaponEvent.Type.RELOAD_STARTED, weapon, java.util.Map.of());
    return true;
  }

  public void tick(long tick) {
    tickStationAnimations(tick);
    pendingShots.removeIf(
        shot -> {
          if (shot.fireAtTick() > tick) {
            return false;
          }
          Player player = Bukkit.getPlayer(shot.playerId());
          WeaponInstance weapon = weapons.find(shot.weaponInstanceId()).orElse(null);
          if (player != null
              && weapon != null
              && (!shot.consumeAmmo()
                  || weapon.tryFollowUpShot(false) == WeaponInstance.FireDecision.FIRED)) {
            fireProjectiles(player, weapon);
            refreshItem(player, weapon);
          }
          return true;
        });
    for (WeaponInstance weapon : List.copyOf(weaponsAll())) {
      if (weapon.completeReload(tick)) {
        Player player = Bukkit.getPlayer(weapon.ownerId());
        if (player != null) {
          refreshItem(player, weapon);
        }
        publish(WeaponEvent.Type.RELOAD_COMPLETED, weapon, java.util.Map.of());
      }
    }
  }

  public boolean interactMapObject(Player player, Location clicked, long tick) {
    if (!canAct.test(player.getUniqueId())) {
      return false;
    }
    UUID gameId = playerGame.apply(player.getUniqueId()).orElse(null);
    MapDefinition map = gameId == null ? null : gameMap.apply(gameId).orElse(null);
    if (gameId == null || map == null) {
      return false;
    }
    MapDefinition.MapObject object =
        map.objects().values().stream()
            .filter(
                value ->
                    value.type() == MapObjectType.WEAPON_WALL
                        || value.type() == MapObjectType.MYSTERY_BOX
                        || value.type() == MapObjectType.PACK_A_PUNCH)
            .filter(value -> distanceSquared(value.position(), clicked) <= 4)
            .min(Comparator.comparingDouble(value -> distanceSquared(value.position(), clicked)))
            .orElse(null);
    if (object == null) {
      return false;
    }
    return switch (object.type()) {
      case WEAPON_WALL -> wallPurchase(gameId, player, object, tick);
      case MYSTERY_BOX -> mysteryPurchase(gameId, player, object, tick);
      case PACK_A_PUNCH -> packAPunch(gameId, player, object, tick);
      default -> false;
    };
  }

  public Optional<WeaponInstance> current(Player player) {
    return find(player.getInventory().getItemInMainHand())
        .filter(value -> value.ownerId().equals(player.getUniqueId()));
  }

  public Optional<WeaponInstance> find(ItemStack item) {
    if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
      return Optional.empty();
    }
    String raw =
        item.getItemMeta().getPersistentDataContainer().get(instanceKey, PersistentDataType.STRING);
    if (raw == null) {
      return Optional.empty();
    }
    try {
      return weapons.find(UUID.fromString(raw));
    } catch (IllegalArgumentException ignored) {
      return Optional.empty();
    }
  }

  public Collection<WeaponInstance> player(UUID playerId) {
    return weapons.player(playerId);
  }

  public Collection<WeaponDefinition> types() {
    return definitions.current().all();
  }

  public int activeCount() {
    return weapons.size();
  }

  public void removeGame(UUID gameId) {
    Set<UUID> players = new HashSet<>();
    Set<UUID> removedIds = new HashSet<>();
    weapons
        .game(gameId)
        .forEach(
            value -> {
              players.add(value.ownerId());
              removedIds.add(value.id());
            });
    weapons.removeGame(gameId);
    pendingShots.removeIf(value -> removedIds.contains(value.weaponInstanceId()));
    stationAnimations
        .values()
        .removeIf(
            animation -> {
              if (!animation.gameId.equals(gameId)) {
                return false;
              }
              animation.removeDisplay();
              restorePackWeapon(animation);
              return true;
            });
    players.stream()
        .map(Bukkit::getPlayer)
        .filter(java.util.Objects::nonNull)
        .forEach(this::removeItems);
  }

  public void synchronizeInventory(Player player) {
    for (WeaponInstance weapon : weapons.player(player.getUniqueId())) {
      if (!hasInstance(player, weapon.id())) {
        player.getInventory().addItem(item(weapon));
      }
    }
  }

  private void fireProjectiles(Player player, WeaponInstance weapon) {
    WeaponDefinition definition = weapon.definition();
    play(player, definition.sounds().get("fire"));
    for (int pellet = 0; pellet < definition.fire().pellets(); pellet++) {
      Vector direction = spreadDirection(player, definition);
      double blockDistance = blockDistance(player.getEyeLocation(), direction, definition);
      List<Target> targets =
          targets(weapon.gameId(), player.getEyeLocation(), direction, blockDistance);
      int maximum = Math.min(definition.penetration().maximumTargets(), targets.size());
      for (int index = 0; index < maximum; index++) {
        Target target = targets.get(index);
        double rawDamage =
            instaKill.test(weapon.gameId())
                ? 1_000_000
                : damage.calculate(
                    definition,
                    target.distance(),
                    target.headshot(),
                    weapon.damageMultiplier(),
                    index,
                    1);
        PaperZombieEngine.WeaponDamageResult result =
            zombies.damageFromWeapon(
                target.entityId(),
                player.getUniqueId(),
                definition.damage().damageType(),
                rawDamage,
                target.headshot(),
                definition.id());
        if (result.appliedDamage() > 0) {
          weapon.recordHit(result.appliedDamage(), target.headshot());
          rewards.hit(
              weapon.gameId(),
              player.getUniqueId(),
              target.entityId(),
              result.appliedDamage(),
              result.hitPointsReward(),
              operation(
                  "weapon-hit",
                  weapon.gameId(),
                  player.getUniqueId(),
                  target.entityId().toString()));
          hitObserver.hit(
              weapon.gameId(), player.getUniqueId(), result.appliedDamage(), target.headshot());
          publish(
              WeaponEvent.Type.DAMAGE,
              weapon,
              java.util.Map.of(
                  "damage", Double.toString(result.appliedDamage()),
                  "headshot", Boolean.toString(target.headshot())));
        }
      }
    }
    applyRecoil(player, definition);
  }

  private boolean wallPurchase(
      UUID gameId, Player player, MapDefinition.MapObject object, long tick) {
    String weaponId = object.properties().getOrDefault("weapon-id", "starter_pistol");
    WeaponDefinition definition = definitions.current().find(weaponId).orElse(null);
    if (definition == null) {
      player.sendMessage(MINI.deserialize("<red>Arme murale inconnue : " + weaponId));
      return true;
    }
    Optional<WeaponInstance> owned =
        weapons.player(player.getUniqueId()).stream()
            .filter(value -> value.definition().id().equals(weaponId))
            .findFirst();
    int configuredCost =
        parseCost(
            object.properties(),
            owned.isPresent() ? "ammo-cost" : "cost",
            owned.isPresent() ? definition.economy().ammoCost() : definition.economy().wallCost());
    PurchaseResult purchase =
        purchases.purchase(
            purchase(
                gameId,
                player,
                owned.isPresent() ? PurchaseType.WALL_AMMO : PurchaseType.WALL_WEAPON,
                weaponId,
                configuredCost,
                owned.isPresent()
                    ? TransactionReason.WALL_AMMO_PURCHASE
                    : TransactionReason.WALL_WEAPON_PURCHASE,
                () -> {
                  if (owned.isPresent()) {
                    owned.get().refillReserve();
                    refreshItem(player, owned.get());
                    return true;
                  }
                  return give(gameId, player, definition, tick).isPresent();
                }));
    if (!purchase.successful()) {
      purchaseFailure(player, purchase);
      return true;
    }
    player.sendMessage(
        MINI.deserialize(
            owned.isPresent()
                ? "<green>Munitions achetées."
                : "<green>Arme achetée : " + definition.displayName()));
    publish(
        WeaponEvent.Type.PURCHASED,
        owned.orElseGet(
            () ->
                weapons.player(player.getUniqueId()).stream()
                    .filter(value -> value.definition().id().equals(weaponId))
                    .findFirst()
                    .orElseThrow()),
        java.util.Map.of("source", "wall", "cost", Integer.toString(configuredCost)));
    return true;
  }

  private boolean mysteryPurchase(
      UUID gameId, Player player, MapDefinition.MapObject object, long tick) {
    if (stationAnimations.containsKey(player.getUniqueId())) {
      player.sendActionBar(MINI.deserialize("<yellow>Une machine est déjà en cours."));
      return true;
    }
    int cost = parseCost(object.properties(), "cost", 950);
    Set<String> blacklist =
        Set.of(object.properties().getOrDefault("blacklist", "").split(",")).stream()
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    boolean wonder =
        Boolean.parseBoolean(object.properties().getOrDefault("wonder-weapons", "true"));
    Optional<WeaponDefinition> selected =
        mysteryBox.select(
            definitions.current().all(),
            blacklist,
            wonder,
            bound -> ThreadLocalRandom.current().nextInt(bound));
    if (selected.isEmpty()) {
      player.sendMessage(MINI.deserialize("<red>Aucune arme éligible dans cette boîte."));
      return true;
    }
    List<WeaponDefinition> previews =
        definitions.current().all().stream()
            .filter(value -> !blacklist.contains(value.id()))
            .filter(value -> wonder || value.rarity() != WeaponDefinition.WeaponRarity.WONDER)
            .toList();
    ItemDisplay display = spawnWeaponDisplay(player, object, previewItem(previews.getFirst()));
    stationAnimations.put(
        player.getUniqueId(),
        StationAnimation.mystery(
            gameId,
            player.getUniqueId(),
            object.id(),
            cost,
            tick,
            animationTicks(object),
            selected.get(),
            previews,
            display.getUniqueId()));
    player.sendActionBar(MINI.deserialize("<light_purple>Mystery Box… <white>5"));
    player.playSound(display.getLocation(), Sound.BLOCK_CHEST_OPEN, 1, 1);
    return true;
  }

  private void tickStationAnimations(long tick) {
    var iterator = stationAnimations.values().iterator();
    while (iterator.hasNext()) {
      StationAnimation animation = iterator.next();
      Player player = Bukkit.getPlayer(animation.playerId);
      if (tick >= animation.completeAt) {
        animation.removeDisplay();
        if (animation.type == StationAnimation.Type.MYSTERY_BOX) {
          completeMystery(animation, player, tick);
        } else {
          completePack(animation, player);
        }
        iterator.remove();
        continue;
      }
      if (tick < animation.nextVisualAt) {
        continue;
      }
      animation.nextVisualAt = tick + 5;
      Entity rawDisplay = Bukkit.getEntity(animation.displayId);
      if (rawDisplay instanceof ItemDisplay display) {
        display.setRotation((float) ((tick * 12) % 360), 0);
        if (animation.type == StationAnimation.Type.MYSTERY_BOX) {
          WeaponDefinition preview =
              animation.previews.get(animation.previewIndex++ % animation.previews.size());
          display.setItemStack(previewItem(preview));
          display
              .getWorld()
              .playSound(display.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.5f);
        } else {
          display.getWorld().playSound(display.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.25f, 1.8f);
        }
      }
      if (player != null) {
        long seconds = Math.max(1, (animation.completeAt - tick + 19) / 20);
        player.sendActionBar(
            MINI.deserialize(
                (animation.type == StationAnimation.Type.MYSTERY_BOX
                        ? "<light_purple>Mystery Box… "
                        : "<aqua>Pack-a-Punch… ")
                    + "<white>"
                    + seconds));
      }
    }
  }

  private void completeMystery(StationAnimation animation, Player player, long tick) {
    if (player == null
        || !player.isOnline()
        || !playerGame.apply(player.getUniqueId()).filter(animation.gameId::equals).isPresent()) {
      return;
    }
    final WeaponInstance[] granted = new WeaponInstance[1];
    PurchaseResult purchase =
        purchases.purchase(
            purchase(
                animation.gameId,
                player,
                PurchaseType.MYSTERY_BOX,
                animation.objectId,
                animation.cost,
                TransactionReason.MYSTERY_BOX_PURCHASE,
                () -> {
                  granted[0] =
                      give(animation.gameId, player, animation.selected, tick).orElse(null);
                  return granted[0] != null;
                }));
    if (!purchase.successful()) {
      purchaseFailure(player, purchase);
      return;
    }
    publish(
        WeaponEvent.Type.PURCHASED,
        granted[0],
        Map.of("source", "mystery_box", "cost", Integer.toString(animation.cost)));
    player.sendMessage(
        MINI.deserialize("<light_purple>Mystery Box : " + animation.selected.displayName()));
    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1.2f);
  }

  private void completePack(StationAnimation animation, Player player) {
    if (player == null
        || !player.isOnline()
        || !playerGame.apply(player.getUniqueId()).filter(animation.gameId::equals).isPresent()) {
      restorePackWeapon(animation);
      return;
    }
    PurchaseResult purchase =
        purchases.purchase(
            purchase(
                animation.gameId,
                player,
                PurchaseType.PACK_A_PUNCH,
                animation.weapon.id().toString(),
                animation.cost,
                TransactionReason.PACK_A_PUNCH_PURCHASE,
                animation.weapon::upgrade));
    restorePackWeapon(animation);
    if (!purchase.successful()) {
      purchaseFailure(player, purchase);
      return;
    }
    publish(
        WeaponEvent.Type.UPGRADED,
        animation.weapon,
        Map.of("level", Integer.toString(animation.weapon.snapshot().upgradeLevel())));
    player.sendMessage(
        MINI.deserialize(
            "<light_purple>Pack-a-Punch niveau "
                + animation.weapon.snapshot().upgradeLevel()
                + " !"));
    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 0.8f);
  }

  private void restorePackWeapon(StationAnimation animation) {
    if (animation.weapon == null) {
      return;
    }
    Player owner = Bukkit.getPlayer(animation.playerId);
    if (owner != null && owner.isOnline()) {
      owner.getInventory().setItem(animation.inventorySlot, item(animation.weapon));
    }
  }

  private ItemDisplay spawnWeaponDisplay(
      Player player, MapDefinition.MapObject object, ItemStack stack) {
    Location location =
        new Location(
            player.getWorld(),
            Math.floor(object.position().x()) + 0.5,
            Math.floor(object.position().y()) + 1.35,
            Math.floor(object.position().z()) + 0.5);
    ItemDisplay display = player.getWorld().spawn(location, ItemDisplay.class);
    display.setItemStack(stack);
    display.setGlowing(true);
    display.setPersistent(false);
    return display;
  }

  private static ItemStack previewItem(WeaponDefinition definition) {
    Material material = Material.matchMaterial(definition.presentation().material());
    ItemStack stack = new ItemStack(material == null ? Material.STICK : material);
    ItemMeta meta = stack.getItemMeta();
    meta.displayName(MINI.deserialize(definition.displayName()));
    if (definition.presentation().customModelData() > 0) {
      meta.setCustomModelData(definition.presentation().customModelData());
    }
    stack.setItemMeta(meta);
    return stack;
  }

  static int animationTicks(MapDefinition.MapObject object) {
    try {
      return Math.max(
          20, Integer.parseInt(object.properties().getOrDefault("animation-ticks", "100")));
    } catch (NumberFormatException ignored) {
      return 100;
    }
  }

  private boolean packAPunch(
      UUID gameId, Player player, MapDefinition.MapObject object, long tick) {
    if (stationAnimations.containsKey(player.getUniqueId())) {
      player.sendActionBar(MINI.deserialize("<yellow>Une machine est déjà en cours."));
      return true;
    }
    WeaponInstance weapon = current(player).orElse(null);
    if (weapon == null) {
      player.sendMessage(MINI.deserialize("<red>Tenez une arme à améliorer."));
      return true;
    }
    int cost = weapon.nextUpgradeCost();
    if (cost < 0) {
      player.sendMessage(MINI.deserialize("<yellow>Cette arme est au niveau maximum."));
      return true;
    }
    if (!publish(WeaponEvent.Type.PRE_UPGRADE, weapon, java.util.Map.of())) {
      return true;
    }
    int slot = player.getInventory().getHeldItemSlot();
    player.getInventory().setItem(slot, null);
    ItemDisplay display = spawnWeaponDisplay(player, object, item(weapon));
    stationAnimations.put(
        player.getUniqueId(),
        StationAnimation.pack(
            gameId,
            player.getUniqueId(),
            cost,
            tick,
            animationTicks(object),
            weapon,
            slot,
            display.getUniqueId()));
    player.sendActionBar(MINI.deserialize("<aqua>Pack-a-Punch… <white>5"));
    player.playSound(display.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f);
    return true;
  }

  private List<Target> targets(
      UUID gameId, Location origin, Vector direction, double maximumDistance) {
    ArrayList<Target> result = new ArrayList<>();
    for (ZombieInstance zombie : zombies.game(gameId)) {
      Entity entity = Bukkit.getEntity(zombie.entityId());
      if (!(entity instanceof LivingEntity living)
          || !living.getWorld().equals(origin.getWorld())) {
        continue;
      }
      Vector offset =
          living
              .getLocation()
              .toVector()
              .add(new Vector(0, living.getHeight() * 0.5, 0))
              .subtract(origin.toVector());
      double distance = offset.dot(direction);
      if (distance < 0 || distance > maximumDistance) {
        continue;
      }
      Vector closest = origin.toVector().add(direction.clone().multiply(distance));
      double perpendicular =
          closest.distance(
              living.getLocation().toVector().add(new Vector(0, living.getHeight() * 0.5, 0)));
      if (perpendicular > Math.max(0.55, living.getWidth() * 0.75)) {
        continue;
      }
      boolean headshot = closest.getY() >= living.getLocation().getY() + living.getHeight() * 0.72;
      result.add(new Target(living.getUniqueId(), distance, headshot));
    }
    result.sort(Comparator.comparingDouble(Target::distance));
    return result;
  }

  private double blockDistance(Location origin, Vector direction, WeaponDefinition definition) {
    int maximum = (int) Math.ceil(definition.damage().maximumDistance());
    BlockIterator blocks =
        new BlockIterator(origin.getWorld(), origin.toVector(), direction, 0, maximum);
    Set<String> penetrable =
        definition.penetration().penetrableMaterials().stream()
            .map(String::toUpperCase)
            .collect(java.util.stream.Collectors.toSet());
    while (blocks.hasNext()) {
      Block block = blocks.next();
      if (block.isPassable() || penetrable.contains(block.getType().name())) {
        continue;
      }
      return Math.max(0, block.getLocation().toVector().distance(origin.toVector()));
    }
    return definition.damage().maximumDistance();
  }

  private Vector spreadDirection(Player player, WeaponDefinition definition) {
    double value;
    if (!player.getLocation().clone().subtract(0, 0.1, 0).getBlock().getType().isSolid()) {
      value = definition.spread().airborne();
    } else if (player.isSprinting()) {
      value = definition.spread().sprinting();
    } else if (player.getVelocity().lengthSquared() > 0.01) {
      value = definition.spread().walking();
    } else {
      value = definition.spread().standing();
    }
    WeaponSpreadCalculator.Offset offset = spread.calculate(value, ThreadLocalRandom.current());
    Location adjusted = player.getEyeLocation().clone();
    adjusted.setYaw(adjusted.getYaw() + (float) offset.yawDegrees());
    adjusted.setPitch(adjusted.getPitch() + (float) offset.pitchDegrees());
    return adjusted.getDirection().normalize();
  }

  private static void applyRecoil(Player player, WeaponDefinition definition) {
    double horizontal =
        ThreadLocalRandom.current()
            .nextDouble(-definition.recoil().horizontal(), definition.recoil().horizontal());
    Location location = player.getLocation();
    location.setYaw(location.getYaw() + (float) horizontal);
    location.setPitch(
        Math.max(-90, Math.min(90, location.getPitch() - (float) definition.recoil().vertical())));
    player.teleport(location);
  }

  private ItemStack item(WeaponInstance weapon) {
    WeaponDefinition definition = weapon.definition();
    Material material =
        Material.matchMaterial(
            weapon.snapshot().upgradeLevel() > 0
                ? definition.presentation().upgradedMaterial()
                : definition.presentation().material());
    ItemStack item = new ItemStack(material == null ? Material.STICK : material);
    ItemMeta meta = item.getItemMeta();
    meta.displayName(
        MINI.deserialize(
            weapon.snapshot().upgradeLevel() > 0
                ? definition.upgrades().get(weapon.snapshot().upgradeLevel() - 1).displayName()
                : definition.displayName()));
    meta.lore(lore(weapon));
    int modelData =
        weapon.snapshot().upgradeLevel() == 0
            ? definition.presentation().customModelData()
            : definition.upgrades().get(weapon.snapshot().upgradeLevel() - 1).customModelData();
    if (modelData > 0) {
      meta.setCustomModelData(modelData);
    }
    meta.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
    meta.getPersistentDataContainer()
        .set(instanceKey, PersistentDataType.STRING, weapon.id().toString());
    meta.getPersistentDataContainer()
        .set(definitionKey, PersistentDataType.STRING, definition.id());
    item.setItemMeta(meta);
    return item;
  }

  private static List<Component> lore(WeaponInstance weapon) {
    WeaponInstance.Snapshot value = weapon.snapshot();
    return List.of(
        MINI.deserialize("<gray>Munitions : <white>" + value.magazine() + " / " + value.reserve()),
        MINI.deserialize("<gray>Niveau Pack-a-Punch : <white>" + value.upgradeLevel()),
        MINI.deserialize(
            value.reloadCompleteTick().isPresent()
                ? "<yellow>Rechargement…"
                : "<dark_gray>Clic droit : tirer • F : recharger"));
  }

  private void refreshItem(Player player, WeaponInstance weapon) {
    for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
      ItemStack candidate = player.getInventory().getItem(slot);
      if (find(candidate).map(value -> value.id().equals(weapon.id())).orElse(false)) {
        player.getInventory().setItem(slot, item(weapon));
      }
    }
  }

  private boolean hasInstance(Player player, UUID id) {
    for (ItemStack item : player.getInventory().getContents()) {
      if (find(item).map(value -> value.id().equals(id)).orElse(false)) {
        return true;
      }
    }
    return false;
  }

  private void removeItems(Player player) {
    for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
      if (find(player.getInventory().getItem(slot)).isPresent()) {
        player.getInventory().setItem(slot, null);
      }
    }
  }

  private int firstWeaponSlot(Player player) {
    for (int slot = 0; slot < 2; slot++) {
      if (player.getInventory().getItem(slot) == null) {
        return slot;
      }
    }
    return player.getInventory().getHeldItemSlot() < 2
        ? player.getInventory().getHeldItemSlot()
        : 0;
  }

  private Collection<WeaponInstance> weaponsAll() {
    return weapons.all();
  }

  private static int parseCost(java.util.Map<String, String> properties, String key, int fallback) {
    try {
      return Math.max(
          0, Integer.parseInt(properties.getOrDefault(key, Integer.toString(fallback))));
    } catch (NumberFormatException ignored) {
      return fallback;
    }
  }

  private static double distanceSquared(
      fr.heneria.zombie.core.editor.MapPoint point, Location location) {
    double x = point.x() - location.getX();
    double y = point.y() - location.getY();
    double z = point.z() - location.getZ();
    return x * x + y * y + z * z;
  }

  private static void play(Player player, String soundName) {
    if (soundName == null || soundName.isBlank()) {
      return;
    }
    try {
      player.getWorld().playSound(player.getLocation(), Sound.valueOf(soundName), 1, 1);
    } catch (IllegalArgumentException ignored) {
      // Definition validation reports invalid sounds through the diagnostic GUI in a later
      // revision.
    }
  }

  public WeaponEventDispatcher events() {
    return events;
  }

  /** Refills every weapon reserve in the game for the Max Ammo power-up. */
  public void refillGame(UUID gameId) {
    weapons
        .game(gameId)
        .forEach(
            weapon -> {
              weapon.refillReserve();
              Player owner = Bukkit.getPlayer(weapon.ownerId());
              if (owner != null) {
                refreshItem(owner, weapon);
              }
            });
  }

  private PurchaseRequest purchase(
      UUID gameId,
      Player player,
      PurchaseType type,
      String itemId,
      long price,
      TransactionReason reason,
      PurchaseRequest.Grant grant) {
    return new PurchaseRequest(
        gameId,
        player.getUniqueId(),
        type,
        itemId,
        PriceResolver.PriceContext.base(price),
        operation("purchase-" + type.name(), gameId, player.getUniqueId(), itemId),
        reason,
        PurchaseFundingMode.INDIVIDUAL,
        () ->
            player.isOnline()
                && playerGame.apply(player.getUniqueId()).filter(gameId::equals).isPresent(),
        grant,
        java.util.Map.of());
  }

  private String operation(String prefix, UUID gameId, UUID playerId, String itemId) {
    return prefix + ":" + gameId + ":" + playerId + ":" + itemId + ":" + Bukkit.getCurrentTick();
  }

  private static void purchaseFailure(Player player, PurchaseResult result) {
    String message =
        result.status() == PurchaseResult.Status.PAYMENT_FAILED
            ? "<red>Points insuffisants."
            : "<red>Achat impossible : " + result.failureReason();
    player.sendMessage(MINI.deserialize(message));
  }

  private boolean publish(
      WeaponEvent.Type type, WeaponInstance weapon, java.util.Map<String, String> data) {
    return events.publish(
        new WeaponEvent(
            type,
            weapon.gameId(),
            weapon.ownerId(),
            weapon.id(),
            weapon.definition().id(),
            Instant.now(),
            data));
  }

  private record PendingShot(
      UUID playerId, UUID weaponInstanceId, long fireAtTick, boolean consumeAmmo) {}

  private record Target(UUID entityId, double distance, boolean headshot) {}

  private static final class StationAnimation {
    private enum Type {
      MYSTERY_BOX,
      PACK_A_PUNCH
    }

    private final Type type;
    private final UUID gameId;
    private final UUID playerId;
    private final String objectId;
    private final int cost;
    private final long completeAt;
    private final WeaponDefinition selected;
    private final List<WeaponDefinition> previews;
    private final WeaponInstance weapon;
    private final int inventorySlot;
    private final UUID displayId;
    private long nextVisualAt;
    private int previewIndex;

    private StationAnimation(
        Type type,
        UUID gameId,
        UUID playerId,
        String objectId,
        int cost,
        long startedAt,
        int durationTicks,
        WeaponDefinition selected,
        List<WeaponDefinition> previews,
        WeaponInstance weapon,
        int inventorySlot,
        UUID displayId) {
      this.type = type;
      this.gameId = gameId;
      this.playerId = playerId;
      this.objectId = objectId;
      this.cost = cost;
      this.completeAt = startedAt + durationTicks;
      this.selected = selected;
      this.previews = previews;
      this.weapon = weapon;
      this.inventorySlot = inventorySlot;
      this.displayId = displayId;
      this.nextVisualAt = startedAt;
    }

    private static StationAnimation mystery(
        UUID gameId,
        UUID playerId,
        String objectId,
        int cost,
        long startedAt,
        int durationTicks,
        WeaponDefinition selected,
        List<WeaponDefinition> previews,
        UUID displayId) {
      return new StationAnimation(
          Type.MYSTERY_BOX,
          gameId,
          playerId,
          objectId,
          cost,
          startedAt,
          durationTicks,
          selected,
          previews,
          null,
          -1,
          displayId);
    }

    private static StationAnimation pack(
        UUID gameId,
        UUID playerId,
        int cost,
        long startedAt,
        int durationTicks,
        WeaponInstance weapon,
        int inventorySlot,
        UUID displayId) {
      return new StationAnimation(
          Type.PACK_A_PUNCH,
          gameId,
          playerId,
          weapon.id().toString(),
          cost,
          startedAt,
          durationTicks,
          null,
          List.of(),
          weapon,
          inventorySlot,
          displayId);
    }

    private void removeDisplay() {
      Entity display = Bukkit.getEntity(displayId);
      if (display != null) {
        display.remove();
      }
    }
  }

  @FunctionalInterface
  public interface HitObserver {
    void hit(UUID gameId, UUID playerId, double appliedDamage, boolean headshot);
  }
}
