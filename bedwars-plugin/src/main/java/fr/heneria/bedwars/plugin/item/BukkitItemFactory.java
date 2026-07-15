package fr.heneria.bedwars.plugin.item;

import fr.heneria.bedwars.core.config.LanguageService;
import fr.heneria.bedwars.core.item.HeadDefinition;
import fr.heneria.bedwars.core.item.ItemBuildException;
import fr.heneria.bedwars.core.item.ItemContext;
import fr.heneria.bedwars.core.item.ItemDefinition;
import fr.heneria.bedwars.core.item.ItemText;
import java.util.ArrayList;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** Builds independent Bukkit stacks from prevalidated immutable definitions. */
public final class BukkitItemFactory {
  private static final Set<String> PDC_TAG_ALLOWLIST = Set.of("category", "action");
  private final LanguageService language;
  private final NamespacedKey itemKey;
  private final NamespacedKey categoryKey;
  private final NamespacedKey actionKey;
  private final EnchantmentResolver enchantments = new EnchantmentResolver();

  /** Creates a factory with reusable namespace keys but no mutable stack cache. */
  public BukkitItemFactory(JavaPlugin plugin, LanguageService language) {
    this.language = language;
    itemKey = new NamespacedKey(plugin, "item_key");
    categoryKey = new NamespacedKey(plugin, "category");
    actionKey = new NamespacedKey(plugin, "action");
  }

  /**
   * Builds one new stack; no stack or metadata object is cached or shared. Player/head operations
   * must run on the Bukkit server thread.
   *
   * @throws ItemBuildException when required context or Bukkit metadata application fails
   */
  public ItemStack build(ItemDefinition definition, ItemContext context) {
    try {
      validateRequiredPlaceholders(definition, context);
      Material material = Material.matchMaterial(definition.material());
      if (material == null) throw new IllegalStateException("Prevalidated material disappeared");
      ItemStack stack = new ItemStack(material, definition.amount());
      ItemMeta meta = stack.getItemMeta();
      if (meta == null) return stack;
      String name = render(definition.name(), context);
      if (!name.isEmpty()) meta.setDisplayName(name);
      if (!definition.lore().isEmpty()) {
        ArrayList<String> lore = new ArrayList<>();
        definition.lore().forEach(line -> lore.add(render(line, context)));
        meta.setLore(lore);
      }
      if (context.placeholders().containsKey("preview_key")) {
        java.util.List<String> lore =
            meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add("§8Clé : " + context.placeholders().get("preview_key"));
        meta.setLore(lore);
      }
      meta.setUnbreakable(definition.unbreakable());
      if (definition.customModelData() != null)
        meta.setCustomModelData(definition.customModelData());
      for (String flag : definition.itemFlags()) meta.addItemFlags(ItemFlag.valueOf(flag));
      definition
          .enchantments()
          .forEach(
              (key, level) -> {
                Enchantment enchantment = enchantments.bukkit(key);
                if (enchantment != null) meta.addEnchant(enchantment, level, false);
              });
      if (definition.glow() && meta.getEnchants().isEmpty()) {
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
      }
      if (meta instanceof LeatherArmorMeta leather && definition.leatherColor() != null)
        leather.setColor(Color.fromRGB(definition.leatherColor()));
      if (meta instanceof SkullMeta skull) applyHead(skull, definition.head(), context);
      meta.getPersistentDataContainer()
          .set(itemKey, PersistentDataType.STRING, definition.key().value());
      definition
          .tags()
          .forEach(
              (key, value) -> {
                if (!PDC_TAG_ALLOWLIST.contains(key)) return;
                NamespacedKey namespaced = key.equals("category") ? categoryKey : actionKey;
                meta.getPersistentDataContainer().set(namespaced, PersistentDataType.STRING, value);
              });
      stack.setItemMeta(meta);
      return stack;
    } catch (RuntimeException exception) {
      throw new ItemBuildException(definition.key().value(), exception);
    }
  }

  private String render(ItemText text, ItemContext context) {
    if (text.translationKey() != null) {
      String locale = context.locale().orElse(null);
      return language.message(
          text.translationKey(), locale == null ? "" : locale, context.placeholderContext());
    }
    return language.render(
        text.direct() == null ? "" : text.direct(), context.placeholderContext());
  }

  private static void validateRequiredPlaceholders(ItemDefinition definition, ItemContext context) {
    for (String required : definition.requiredPlaceholders()) {
      boolean supplied = context.placeholders().containsKey(required);
      if (required.equals("player")) supplied |= context.playerName().isPresent();
      if (required.equals("page")) supplied = true;
      if (required.equals("menu")) supplied |= context.menuId().isPresent();
      if (!supplied)
        throw new IllegalArgumentException("Missing required placeholder: " + required);
    }
  }

  private static void applyHead(SkullMeta meta, HeadDefinition head, ItemContext context) {
    if (head == null) return;
    if (head.type() == HeadDefinition.Type.CONTEXT_PLAYER) {
      context.playerId().map(Bukkit::getOfflinePlayer).ifPresent(meta::setOwningPlayer);
      return;
    }
    String name = head.value();
    if (name.contains("{player}")) name = context.playerName().orElse("");
    if (!name.isBlank()) {
      org.bukkit.entity.Player online = Bukkit.getPlayerExact(name);
      if (online != null) meta.setOwningPlayer(online);
    }
  }
}
