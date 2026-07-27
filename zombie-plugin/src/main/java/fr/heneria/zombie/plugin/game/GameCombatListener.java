package fr.heneria.zombie.plugin.game;

import fr.heneria.zombie.core.enemy.ZombieDamageType;
import fr.heneria.zombie.plugin.enemy.PaperZombieEngine;
import fr.heneria.zombie.plugin.message.MessageService;
import java.util.UUID;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/** Thin Paper translation for enemy deaths, downing and hold-to-revive initiation. */
public final class GameCombatListener implements Listener {
  private final PaperGameRuntime games;
  private final PaperZombieEngine zombies;
  private final MessageService messages;

  public GameCombatListener(
      PaperGameRuntime games, PaperZombieEngine zombies, MessageService messages) {
    this.games = games;
    this.zombies = zombies;
    this.messages = messages;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEnemyDeath(EntityDeathEvent event) {
    if (zombies.find(event.getEntity().getUniqueId()).isPresent()) {
      event.getDrops().clear();
      event.setDroppedExp(0);
      return;
    }
    Player killer = event.getEntity().getKiller();
    games.zombieDefeated(
        event.getEntity().getUniqueId(), killer == null ? null : killer.getUniqueId());
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onPlayerDamage(EntityDamageEvent event) {
    if (event instanceof EntityDamageByEntityEvent byEntity
        && event.getEntity() instanceof Player player
        && zombies.find(byEntity.getDamager().getUniqueId()).isPresent()) {
      event.setCancelled(true);
      zombies.attackPlayer(
          byEntity.getDamager().getUniqueId(), player, org.bukkit.Bukkit.getCurrentTick());
      return;
    }
    if (event.getEntity() instanceof Player player
        && event.getFinalDamage() >= player.getHealth()
        && games.down(player)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onZombieDamage(EntityDamageEvent event) {
    if (zombies.find(event.getEntity().getUniqueId()).isEmpty()) {
      return;
    }
    UUID attacker = null;
    if (event instanceof EntityDamageByEntityEvent byEntity) {
      Entity damager = byEntity.getDamager();
      if (damager instanceof Player player) {
        attacker = player.getUniqueId();
      } else if (damager instanceof Projectile projectile
          && projectile.getShooter() instanceof Player player) {
        attacker = player.getUniqueId();
      }
    }
    event.setCancelled(true);
    zombies.damage(
        event.getEntity().getUniqueId(),
        attacker,
        damageType(event.getCause()),
        event.getFinalDamage(),
        false);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onRevive(PlayerInteractEntityEvent event) {
    if (event.getRightClicked() instanceof Player target
        && event.getPlayer().isSneaking()
        && games.beginRevive(event.getPlayer(), target)) {
      event.setCancelled(true);
      event.getPlayer().sendMessage(messages.render("game.revive-started"));
    }
  }

  private static ZombieDamageType damageType(EntityDamageEvent.DamageCause cause) {
    return switch (cause) {
      case FIRE, FIRE_TICK, LAVA, HOT_FLOOR -> ZombieDamageType.FIRE;
      case ENTITY_EXPLOSION, BLOCK_EXPLOSION -> ZombieDamageType.EXPLOSIVE;
      case VOID -> ZombieDamageType.VOID;
      case ENTITY_ATTACK, ENTITY_SWEEP_ATTACK -> ZombieDamageType.MELEE;
      default -> ZombieDamageType.PHYSICAL;
    };
  }
}
