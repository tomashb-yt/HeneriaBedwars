package fr.heneria.bedwars.plugin.game.shop;

import fr.heneria.bedwars.core.game.equipment.EquipmentKind;
import fr.heneria.bedwars.core.game.shop.ShopCurrency;
import fr.heneria.bedwars.core.game.shop.ShopInventory;
import fr.heneria.bedwars.core.game.shop.ShopOffer;
import fr.heneria.bedwars.core.game.upgrade.TeamUpgradeWallet;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Atomic storage-content exchange used by the pure purchase policy. */
public final class BukkitShopInventory implements ShopInventory, TeamUpgradeWallet {
  private final Player player;
  private final String teamColor;

  public BukkitShopInventory(Player player) {
    this(player, "WHITE");
  }

  public BukkitShopInventory(Player player, String teamColor) {
    this.player = Objects.requireNonNull(player, "player");
    this.teamColor = Objects.requireNonNullElse(teamColor, "WHITE");
  }

  @Override
  public int balance(ShopCurrency currency) {
    Material material = currency(currency);
    int total = 0;
    for (ItemStack stack : player.getInventory().getStorageContents())
      if (stack != null && stack.getType() == material) total += stack.getAmount();
    return total;
  }

  @Override
  public boolean canExchange(ShopOffer offer) {
    return simulate(offer) != null;
  }

  @Override
  public boolean exchange(ShopOffer offer) {
    ItemStack[] result = simulate(offer);
    if (result == null) return false;
    player.getInventory().setStorageContents(result);
    player.updateInventory();
    return true;
  }

  @Override
  public boolean pay(ShopCurrency currency, int amount) {
    ItemStack[] result = simulatePayment(currency, amount);
    if (result == null) return false;
    player.getInventory().setStorageContents(result);
    player.updateInventory();
    return true;
  }

  private ItemStack[] simulate(ShopOffer offer) {
    ItemStack[] next = simulatePayment(offer.currency(), offer.price());
    if (next == null) return null;
    if (offer.kind() != EquipmentKind.ITEM) return next;
    Material product = productMaterial(offer);
    if (product == null || !product.isItem()) return null;
    int remainingProduct = offer.amount();
    for (ItemStack stack : next) {
      if (stack == null || stack.getType() != product || stack.hasItemMeta()) continue;
      int accepted = Math.min(remainingProduct, stack.getMaxStackSize() - stack.getAmount());
      if (accepted < 1) continue;
      stack.setAmount(stack.getAmount() + accepted);
      remainingProduct -= accepted;
      if (remainingProduct == 0) return next;
    }
    for (int index = 0; index < next.length && remainingProduct > 0; index++) {
      if (next[index] != null) continue;
      int amount = Math.min(remainingProduct, product.getMaxStackSize());
      next[index] = new ItemStack(product, amount);
      remainingProduct -= amount;
    }
    return remainingProduct == 0 ? next : null;
  }

  private ItemStack[] simulatePayment(ShopCurrency shopCurrency, int price) {
    ItemStack[] contents = player.getInventory().getStorageContents();
    ItemStack[] next = new ItemStack[contents.length];
    for (int index = 0; index < contents.length; index++)
      next[index] = contents[index] == null ? null : contents[index].clone();
    int remainingPrice = price;
    Material currency = currency(shopCurrency);
    for (int index = 0; index < next.length && remainingPrice > 0; index++) {
      ItemStack stack = next[index];
      if (stack == null || stack.getType() != currency) continue;
      int removed = Math.min(stack.getAmount(), remainingPrice);
      remainingPrice -= removed;
      if (removed == stack.getAmount()) next[index] = null;
      else stack.setAmount(stack.getAmount() - removed);
    }
    return remainingPrice == 0 ? next : null;
  }

  public Material productMaterial(ShopOffer offer) {
    Material configured = Material.matchMaterial(offer.material());
    if (configured != Material.WHITE_WOOL) return configured;
    return teamWool(teamColor);
  }

  public static Material teamWool(String color) {
    if (color == null) return Material.WHITE_WOOL;
    return switch (color.toUpperCase(java.util.Locale.ROOT)) {
      case "RED" -> Material.RED_WOOL;
      case "BLUE" -> Material.BLUE_WOOL;
      case "GREEN" -> Material.GREEN_WOOL;
      case "YELLOW" -> Material.YELLOW_WOOL;
      case "AQUA" -> Material.LIGHT_BLUE_WOOL;
      case "PINK" -> Material.PINK_WOOL;
      case "GRAY" -> Material.GRAY_WOOL;
      case "LIME" -> Material.LIME_WOOL;
      case "ORANGE" -> Material.ORANGE_WOOL;
      case "PURPLE" -> Material.PURPLE_WOOL;
      case "BLACK" -> Material.BLACK_WOOL;
      default -> Material.WHITE_WOOL;
    };
  }

  public static Material currency(ShopCurrency currency) {
    return switch (currency) {
      case IRON -> Material.IRON_INGOT;
      case GOLD -> Material.GOLD_INGOT;
      case DIAMOND -> Material.DIAMOND;
      case EMERALD -> Material.EMERALD;
    };
  }
}
