package fr.heneria.bedwars.plugin.game.upgrade;

import fr.heneria.bedwars.core.config.PlaceholderContext;
import fr.heneria.bedwars.core.game.GameInstanceManager;
import fr.heneria.bedwars.core.game.shop.ShopCurrency;
import fr.heneria.bedwars.core.game.upgrade.TeamUpgradeDefinition;
import fr.heneria.bedwars.core.game.upgrade.TeamUpgradePurchaseService;
import fr.heneria.bedwars.core.game.upgrade.TeamUpgradeType;
import fr.heneria.bedwars.core.gui.Gui;
import fr.heneria.bedwars.core.gui.GuiButton;
import fr.heneria.bedwars.core.gui.GuiItem;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import fr.heneria.bedwars.plugin.game.equipment.BukkitEquipmentService;
import fr.heneria.bedwars.plugin.game.shop.BukkitShopInventory;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Compact team-upgrade shop with explicit current and next levels. */
public final class UpgradeMenuFactory {
  private static final List<Integer> SLOTS = List.of(11, 13, 15);
  private final GameInstanceManager games;
  private final ConfigurationService configurations;
  private final BukkitUpgradeCatalog catalogs;
  private final TeamUpgradePurchaseService purchases;
  private final BukkitEquipmentService equipment;

  public UpgradeMenuFactory(
      GameInstanceManager games,
      ConfigurationService configurations,
      BukkitUpgradeCatalog catalogs,
      TeamUpgradePurchaseService purchases,
      BukkitEquipmentService equipment) {
    this.games = Objects.requireNonNull(games, "games");
    this.configurations = Objects.requireNonNull(configurations, "configurations");
    this.catalogs = Objects.requireNonNull(catalogs, "catalogs");
    this.purchases = Objects.requireNonNull(purchases, "purchases");
    this.equipment = Objects.requireNonNull(equipment, "equipment");
  }

  public Gui menu(UUID playerId) {
    Player player = Bukkit.getPlayer(playerId);
    var game = games.byPlayer(playerId).orElse(null);
    if (player == null || game == null) return unavailable();
    var runtimePlayer = game.player(playerId).orElse(null);
    var team =
        runtimePlayer == null ? null : runtimePlayer.teamId().flatMap(game::team).orElse(null);
    if (team == null) return unavailable();
    BukkitShopInventory wallet = new BukkitShopInventory(player, team.snapshot().color());
    Gui.Builder builder =
        Gui.builder()
            .id("game.upgrades")
            .title(message("upgrade.menu.title", Map.of("team", team.snapshot().displayName())))
            .rows(4)
            .fillEmptySlots(true)
            .button(
                4,
                GuiButton.builder()
                    .item(
                        context ->
                            new GuiItem(
                                "DIAMOND",
                                1,
                                message("upgrade.menu.wallet", Map.of()),
                                List.of(
                                    message(
                                        "upgrade.menu.diamonds",
                                        Map.of("balance", wallet.balance(ShopCurrency.DIAMOND))),
                                    "",
                                    message("upgrade.menu.shared", Map.of())),
                                false,
                                null))
                    .build())
            .button(
                31,
                GuiButton.builder()
                    .itemKey("gui.close")
                    .onLeftClick(context -> context.close())
                    .build());
    List<TeamUpgradeDefinition> definitions = catalogs.current().all();
    for (int index = 0; index < Math.min(definitions.size(), SLOTS.size()); index++) {
      TeamUpgradeDefinition definition = definitions.get(index);
      builder.button(
          SLOTS.get(index),
          GuiButton.builder()
              .item(context -> item(team.upgradeLevel(definition.type()), wallet, definition))
              .onLeftClick(context -> buy(context, player, definition))
              .build());
    }
    return builder.build();
  }

  private void buy(
      fr.heneria.bedwars.core.gui.GuiClickContext context,
      Player player,
      TeamUpgradeDefinition definition) {
    var game = games.byPlayer(player.getUniqueId()).orElse(null);
    String color =
        game == null
            ? "WHITE"
            : game.player(player.getUniqueId())
                .flatMap(fr.heneria.bedwars.core.game.RuntimePlayer::teamId)
                .flatMap(game::team)
                .map(team -> team.snapshot().color())
                .orElse("WHITE");
    var result =
        purchases.purchase(
            player.getUniqueId(), definition, new BukkitShopInventory(player, color));
    if (result.successful() && game != null)
      game.player(player.getUniqueId())
          .flatMap(fr.heneria.bedwars.core.game.RuntimePlayer::teamId)
          .ifPresent(teamId -> equipment.applyTeam(game, teamId));
    player.sendMessage(
        message(
            "upgrade.purchase." + result.code().name().toLowerCase(Locale.ROOT),
            Map.of(
                "upgrade", message(definition.translationKey(), Map.of()),
                "level", result.level(),
                "price", result.price())));
    player.playSound(
        player.getLocation(),
        result.successful() ? Sound.ENTITY_PLAYER_LEVELUP : Sound.BLOCK_NOTE_BLOCK_BASS,
        0.8f,
        result.successful() ? 1.3f : 0.8f);
    context.replace(menu(player.getUniqueId()));
  }

  private GuiItem item(int current, BukkitShopInventory wallet, TeamUpgradeDefinition definition) {
    boolean maximum = current >= definition.maximumLevel();
    int next = maximum ? current : current + 1;
    int price = maximum ? 0 : definition.priceForLevel(next);
    boolean available = !maximum && wallet.balance(definition.currency()) >= price;
    String state =
        maximum
            ? message("upgrade.menu.maximum", Map.of())
            : available
                ? message("upgrade.menu.available", Map.of())
                : message(
                    "upgrade.menu.insufficient",
                    Map.of("missing", price - wallet.balance(definition.currency())));
    return new GuiItem(
        material(definition.type()).name(),
        1,
        message(definition.translationKey(), Map.of()),
        List.of(
            message(
                "upgrade.menu.level",
                Map.of("current", current, "maximum", definition.maximumLevel())),
            maximum ? "" : message("upgrade.menu.price", Map.of("price", price)),
            "",
            state),
        available,
        null);
  }

  private String message(String key, Map<String, ?> values) {
    PlaceholderContext.Builder context = PlaceholderContext.builder();
    values.forEach(context::put);
    return configurations
        .language()
        .message(key, configurations.snapshot().plugin().locale(), context.build());
  }

  private static Material material(TeamUpgradeType type) {
    return switch (type) {
      case SHARPNESS -> Material.IRON_SWORD;
      case PROTECTION -> Material.IRON_CHESTPLATE;
      case HASTE -> Material.GOLDEN_PICKAXE;
    };
  }

  private static Gui unavailable() {
    return Gui.builder()
        .id("game.upgrades.unavailable")
        .title("Améliorations indisponibles")
        .rows(3)
        .fillEmptySlots(true)
        .button(13, GuiButton.builder().itemKey("shop.empty-v2").build())
        .build();
  }
}
