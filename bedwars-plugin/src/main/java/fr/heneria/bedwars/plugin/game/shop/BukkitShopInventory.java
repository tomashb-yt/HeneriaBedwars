package fr.heneria.bedwars.plugin.game.shop;

import fr.heneria.bedwars.core.game.shop.ShopCurrency;
import fr.heneria.bedwars.core.game.shop.ShopInventory;
import fr.heneria.bedwars.core.game.shop.ShopOffer;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Atomic storage-content exchange used by the pure purchase policy. */
public final class BukkitShopInventory implements ShopInventory {
  private final Player player;

  public BukkitShopInventory(Player player) {
    this.player = Objects.requireNonNull(player, "player");
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

  private ItemStack[] simulate(ShopOffer offer) {
    ItemStack[] contents = player.getInventory().getStorageContents();
    ItemStack[] next = new ItemStack[contents.length];
    for (int index = 0; index < contents.length; index++)
      next[index] = contents[index] == null ? null : contents[index].clone();
    int remainingPrice = offer.price();
    Material currency = currency(offer.currency());
    for (int index = 0; index < next.length && remainingPrice > 0; index++) {
      ItemStack stack = next[index];
      if (stack == null || stack.getType() != currency) continue;
      int removed = Math.min(stack.getAmount(), remainingPrice);
      remainingPrice -= removed;
      if (removed == stack.getAmount()) next[index] = null;
      else stack.setAmount(stack.getAmount() - removed);
    }
    if (remainingPrice > 0) return null;
    Material product = Material.matchMaterial(offer.material());
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

  public static Material currency(ShopCurrency currency) {
    return switch (currency) {
      case IRON -> Material.IRON_INGOT;
      case GOLD -> Material.GOLD_INGOT;
      case DIAMOND -> Material.DIAMOND;
      case EMERALD -> Material.EMERALD;
    };
  }
}
