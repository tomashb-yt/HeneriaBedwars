package fr.heneria.zombie.plugin.game;

import fr.heneria.zombie.plugin.message.MessageService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/** Thin Paper translation for enemy deaths, downing and hold-to-revive initiation. */
public final class GameCombatListener implements Listener {
  private final PaperGameRuntime games;
  private final MessageService messages;

  public GameCombatListener(PaperGameRuntime games, MessageService messages) {
    this.games = games;
    this.messages = messages;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEnemyDeath(EntityDeathEvent event) {
    Player killer = event.getEntity().getKiller();
    games.zombieDefeated(
        event.getEntity().getUniqueId(), killer == null ? null : killer.getUniqueId());
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onPlayerDamage(EntityDamageEvent event) {
    if (event.getEntity() instanceof Player player
        && event.getFinalDamage() >= player.getHealth()
        && games.down(player)) {
      event.setCancelled(true);
    }
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
}
