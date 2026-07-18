package fr.heneria.bedwars.plugin.game.shop;

import fr.heneria.bedwars.api.game.GameState;
import fr.heneria.bedwars.core.config.PlaceholderContext;
import fr.heneria.bedwars.core.game.GameInstanceManager;
import fr.heneria.bedwars.core.game.shop.ShopCategory;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import fr.heneria.bedwars.plugin.gui.GuiService;
import java.util.Objects;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/** Opens the custom shop only for a living member of the matching PLAYING instance. */
public final class BukkitShopListener implements Listener {
  private final GameInstanceManager games;
  private final ConfigurationService configurations;
  private final BukkitShopNpcService npcs;
  private final GuiService gui;
  private final ShopMenuFactory menus;

  public BukkitShopListener(
      GameInstanceManager games,
      ConfigurationService configurations,
      BukkitShopNpcService npcs,
      GuiService gui,
      ShopMenuFactory menus) {
    this.games = Objects.requireNonNull(games, "games");
    this.configurations = Objects.requireNonNull(configurations, "configurations");
    this.npcs = Objects.requireNonNull(npcs, "npcs");
    this.gui = Objects.requireNonNull(gui, "gui");
    this.menus = Objects.requireNonNull(menus, "menus");
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onInteract(PlayerInteractEntityEvent event) {
    var token = npcs.token(event.getRightClicked()).orElse(null);
    if (token == null) return;
    event.setCancelled(true);
    var game = games.byPlayer(event.getPlayer().getUniqueId()).orElse(null);
    if (game == null
        || !game.id().toString().equals(token.gameId())
        || game.state() != GameState.PLAYING
        || game.player(event.getPlayer().getUniqueId())
            .map(player -> player.spectator())
            .orElse(true)) {
      event
          .getPlayer()
          .sendMessage(
              configurations
                  .language()
                  .message(
                      "shop.unavailable",
                      configurations.snapshot().plugin().locale(),
                      PlaceholderContext.EMPTY));
      return;
    }
    gui.open(event.getPlayer(), menus.menu(event.getPlayer().getUniqueId(), ShopCategory.BLOCKS));
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onDamage(EntityDamageEvent event) {
    if (npcs.token(event.getEntity()).isPresent()) event.setCancelled(true);
  }
}
