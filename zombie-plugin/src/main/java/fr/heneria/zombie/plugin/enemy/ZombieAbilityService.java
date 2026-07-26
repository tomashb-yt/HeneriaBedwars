package fr.heneria.zombie.plugin.enemy;

import fr.heneria.zombie.core.enemy.ZombieInstance;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Registry-driven ability dispatcher; abilities share the grouped engine tick and own no tasks. */
final class ZombieAbilityService {
  private final Map<String, ZombieAbility> abilities;
  private final Function<UUID, Collection<UUID>> gamePlayers;
  private final BiFunction<UUID, UUID, Boolean> targetable;
  private final PaperZombieEngine.PlayerDamageGateway damage;

  ZombieAbilityService(
      Function<UUID, Collection<UUID>> gamePlayers,
      BiFunction<UUID, UUID, Boolean> targetable,
      PaperZombieEngine.PlayerDamageGateway damage) {
    this.gamePlayers = gamePlayers;
    this.targetable = targetable;
    this.damage = damage;
    LinkedHashMap<String, ZombieAbility> builtins = new LinkedHashMap<>();
    register(builtins, new PoisonHitAbility());
    register(builtins, new ExplodeOnDeathAbility());
    abilities = Map.copyOf(builtins);
  }

  void onAttack(ZombieInstance zombie, Player target, long tick) {
    for (String id : zombie.definition().abilities()) {
      ZombieAbility ability = abilities.get(id);
      if (ability != null && zombie.abilityReady(id, tick)) {
        ability.onAttack(new Context(zombie, target, target.getLocation(), tick));
      }
    }
  }

  void onDeath(ZombieInstance zombie, Location location, long tick) {
    for (String id : zombie.definition().abilities()) {
      ZombieAbility ability = abilities.get(id);
      if (ability != null) {
        ability.onDeath(new Context(zombie, null, location, tick));
      }
    }
  }

  private static void register(Map<String, ZombieAbility> registry, ZombieAbility ability) {
    if (registry.putIfAbsent(ability.id(), ability) != null) {
      throw new IllegalStateException("Duplicate zombie ability " + ability.id());
    }
  }

  private interface ZombieAbility {
    String id();

    default void onAttack(Context context) {}

    default void onDeath(Context context) {}
  }

  private record Context(ZombieInstance zombie, Player target, Location location, long tick) {}

  private static final class PoisonHitAbility implements ZombieAbility {
    @Override
    public String id() {
      return "poison_hit";
    }

    @Override
    public void onAttack(Context context) {
      context
          .target()
          .addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0, true, true));
      context.zombie().abilityCooldown(id(), context.tick() + 100);
    }
  }

  private final class ExplodeOnDeathAbility implements ZombieAbility {
    @Override
    public String id() {
      return "explode_on_death";
    }

    @Override
    public void onDeath(Context context) {
      context.location().getWorld().spawnParticle(Particle.EXPLOSION, context.location(), 1);
      for (UUID playerId : gamePlayers.apply(context.zombie().gameId())) {
        Player player = org.bukkit.Bukkit.getPlayer(playerId);
        if (player != null
            && player.getWorld().equals(context.location().getWorld())
            && player.getLocation().distanceSquared(context.location()) <= 16
            && Boolean.TRUE.equals(targetable.apply(context.zombie().gameId(), playerId))) {
          damage.damage(context.zombie(), player, 4);
        }
      }
    }
  }
}
