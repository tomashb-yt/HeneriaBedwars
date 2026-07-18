package fr.heneria.bedwars.plugin.game.shop;

import fr.heneria.bedwars.core.config.PlaceholderContext;
import fr.heneria.bedwars.core.game.GameInstanceManager;
import fr.heneria.bedwars.core.game.shop.ShopCategory;
import fr.heneria.bedwars.core.game.shop.ShopCurrency;
import fr.heneria.bedwars.core.game.shop.ShopOffer;
import fr.heneria.bedwars.core.game.shop.ShopPurchaseCode;
import fr.heneria.bedwars.core.game.shop.ShopPurchaseService;
import fr.heneria.bedwars.core.gui.Gui;
import fr.heneria.bedwars.core.gui.GuiButton;
import fr.heneria.bedwars.core.gui.GuiItem;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Runtime item shop grouped into four compact categories. */
public final class ShopMenuFactory {
  private static final List<Integer> OFFER_SLOTS =
      List.of(19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34);
  private final GameInstanceManager games;
  private final ConfigurationService configurations;
  private final BukkitShopCatalog catalogs;
  private final ShopPurchaseService purchases;

  public ShopMenuFactory(
      GameInstanceManager games,
      ConfigurationService configurations,
      BukkitShopCatalog catalogs,
      ShopPurchaseService purchases) {
    this.games = Objects.requireNonNull(games, "games");
    this.configurations = Objects.requireNonNull(configurations, "configurations");
    this.catalogs = Objects.requireNonNull(catalogs, "catalogs");
    this.purchases = Objects.requireNonNull(purchases, "purchases");
  }

  public Gui menu(UUID playerId, ShopCategory selected) {
    Player player = Bukkit.getPlayer(playerId);
    var game = games.byPlayer(playerId).orElse(null);
    if (player == null || game == null) return unavailable();
    var catalog = catalogs.current();
    BukkitShopInventory inventory = new BukkitShopInventory(player);
    Map<String, Object> wallet = wallet(inventory);
    Gui.Builder builder =
        Gui.builder()
            .id("game.shop." + selected.name().toLowerCase(Locale.ROOT))
            .title(message("shop.menu.title", Map.of("category", categoryName(selected))))
            .rows(6)
            .fillEmptySlots(true)
            .button(
                4,
                GuiButton.builder()
                    .itemKey("shop.wallet-v1")
                    .itemPlaceholders(context -> wallet)
                    .build());
    ShopCategory[] categories = ShopCategory.values();
    int[] categorySlots = {10, 12, 14, 16};
    for (int index = 0; index < categories.length; index++) {
      ShopCategory category = categories[index];
      builder.button(
          categorySlots[index],
          GuiButton.builder()
              .itemKey(
                  "shop.category."
                      + category.name().toLowerCase(Locale.ROOT)
                      + (category == selected ? "-selected-v1" : "-v1"))
              .onLeftClick(context -> context.replace(menu(playerId, category)))
              .build());
    }
    List<ShopOffer> offers = catalog.category(selected);
    if (offers.isEmpty()) builder.button(31, GuiButton.builder().itemKey("shop.empty-v1").build());
    for (int index = 0; index < Math.min(offers.size(), OFFER_SLOTS.size()); index++) {
      ShopOffer offer = offers.get(index);
      builder.button(
          OFFER_SLOTS.get(index),
          GuiButton.builder()
              .item(context -> offerItem(inventory, offer))
              .cooldown(java.time.Duration.ofMillis(150))
              .onLeftClick(context -> purchase(context, player, selected, offer))
              .build());
    }
    return builder
        .button(
            49,
            GuiButton.builder()
                .itemKey("gui.refresh")
                .onLeftClick(context -> context.replace(menu(playerId, selected)))
                .build())
        .button(
            53,
            GuiButton.builder()
                .itemKey("gui.close")
                .onLeftClick(context -> context.close())
                .build())
        .build();
  }

  private void purchase(
      fr.heneria.bedwars.core.gui.GuiClickContext context,
      Player player,
      ShopCategory category,
      ShopOffer offer) {
    var result = purchases.purchase(player.getUniqueId(), offer, new BukkitShopInventory(player));
    Map<String, Object> values =
        Map.of(
            "item",
            message(offer.translationKey(), Map.of()),
            "amount",
            offer.amount(),
            "price",
            offer.price(),
            "currency",
            currencyName(offer.currency()),
            "missing",
            Math.max(0, offer.price() - result.balance()));
    player.sendMessage(
        message("shop.purchase." + result.code().name().toLowerCase(Locale.ROOT), values));
    player.playSound(
        player.getLocation(),
        result.successful() ? Sound.ENTITY_EXPERIENCE_ORB_PICKUP : Sound.BLOCK_NOTE_BLOCK_BASS,
        0.8f,
        result.successful() ? 1.25f : 0.8f);
    if (result.code() == ShopPurchaseCode.NOT_IN_GAME
        || result.code() == ShopPurchaseCode.NOT_PLAYING
        || result.code() == ShopPurchaseCode.SPECTATOR) context.close();
    else context.replace(menu(player.getUniqueId(), category));
  }

  private GuiItem offerItem(BukkitShopInventory inventory, ShopOffer offer) {
    int balance = inventory.balance(offer.currency());
    boolean affordable = balance >= offer.price() && inventory.canExchange(offer);
    String state = message(affordable ? "shop.menu.available" : "shop.menu.unavailable", Map.of());
    return new GuiItem(
        offer.material(),
        Math.min(99, offer.amount()),
        message(offer.translationKey(), Map.of()),
        List.of(
            message("shop.menu.amount", Map.of("amount", offer.amount())),
            message(
                "shop.menu.price",
                Map.of("price", offer.price(), "currency", currencyName(offer.currency()))),
            message("shop.menu.balance", Map.of("balance", balance)),
            "",
            state),
        affordable,
        null);
  }

  private Map<String, Object> wallet(BukkitShopInventory inventory) {
    Map<String, Object> values = new LinkedHashMap<>();
    for (ShopCurrency currency : ShopCurrency.values())
      values.put(currency.name().toLowerCase(Locale.ROOT), inventory.balance(currency));
    return Map.copyOf(values);
  }

  private String categoryName(ShopCategory category) {
    return message("shop.category." + category.name().toLowerCase(Locale.ROOT), Map.of());
  }

  private String currencyName(ShopCurrency currency) {
    return message("shop.currency." + currency.name().toLowerCase(Locale.ROOT), Map.of());
  }

  private String message(String key, Map<String, ?> values) {
    PlaceholderContext.Builder context = PlaceholderContext.builder();
    values.forEach(context::put);
    return configurations
        .language()
        .message(key, configurations.snapshot().plugin().locale(), context.build());
  }

  private static Gui unavailable() {
    return Gui.builder()
        .id("game.shop.unavailable")
        .title("Boutique indisponible")
        .rows(3)
        .fillEmptySlots(true)
        .button(13, GuiButton.builder().itemKey("shop.empty-v1").build())
        .build();
  }
}
