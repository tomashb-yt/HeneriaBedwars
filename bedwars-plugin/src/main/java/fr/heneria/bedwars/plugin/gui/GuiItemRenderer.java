package fr.heneria.bedwars.plugin.gui;

import fr.heneria.bedwars.core.gui.GuiItem;
import java.util.ArrayList;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Converts immutable GUI item descriptions into fresh Bukkit stacks. */
public final class GuiItemRenderer {
  public ItemStack render(GuiItem item) {
    Material material = Material.matchMaterial(item.material());
    if (material == null) material = Material.BARRIER;
    ItemStack stack = new ItemStack(material, item.amount());
    ItemMeta meta = stack.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(item.name());
      meta.setLore(new ArrayList<>(item.lore()));
      if (item.customModelData() != null) meta.setCustomModelData(item.customModelData());
      if (item.glow()) {
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
      }
      stack.setItemMeta(meta);
    }
    return stack;
  }
}
