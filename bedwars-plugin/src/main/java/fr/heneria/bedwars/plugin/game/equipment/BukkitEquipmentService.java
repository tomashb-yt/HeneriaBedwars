package fr.heneria.bedwars.plugin.game.equipment;

import fr.heneria.bedwars.core.game.GameInstanceManager;
import fr.heneria.bedwars.core.game.RuntimeTeam;
import fr.heneria.bedwars.core.game.equipment.PlayerEquipmentSnapshot;
import fr.heneria.bedwars.core.game.upgrade.TeamUpgradeType;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Rebuilds the Bukkit loadout from match-scoped pure runtime state. */
public final class BukkitEquipmentService {
  private final GameInstanceManager games;

  public BukkitEquipmentService(GameInstanceManager games) {
    this.games = Objects.requireNonNull(games, "games");
  }

  public boolean apply(UUID playerId) {
    if (!Bukkit.isPrimaryThread())
      throw new IllegalStateException("Equipment must be applied on the server thread");
    var game = games.byPlayer(playerId).orElse(null);
    Player player = Bukkit.getPlayer(playerId);
    if (game == null || player == null) return false;
    var runtimePlayer = game.player(playerId).orElse(null);
    RuntimeTeam team =
        runtimePlayer == null ? null : runtimePlayer.teamId().flatMap(game::team).orElse(null);
    if (runtimePlayer == null || team == null || runtimePlayer.spectator()) return false;
    PlayerEquipmentSnapshot equipment = runtimePlayer.equipment();
    int protection = team.upgradeLevel(TeamUpgradeType.PROTECTION);
    equipArmor(player, equipment.armorTier(), team.snapshot().color(), protection);
    removeTools(player);
    addTool(player, pickaxe(equipment.pickaxeTier()), equipment.pickaxeTier());
    addTool(player, axe(equipment.axeTier()), equipment.axeTier());
    if (equipment.shears()) addTool(player, Material.SHEARS, 1);
    ensureWoodenSword(player);
    applySharpness(player, team.upgradeLevel(TeamUpgradeType.SHARPNESS));
    applyHaste(player, team.upgradeLevel(TeamUpgradeType.HASTE));
    player.updateInventory();
    return true;
  }

  public void applyTeam(fr.heneria.bedwars.core.game.GameInstance game, String teamId) {
    game.team(teamId).ifPresent(team -> team.playerIds().forEach(this::apply));
  }

  private static void equipArmor(Player player, int tier, String color, int protection) {
    ItemStack helmet = leather(Material.LEATHER_HELMET, color);
    ItemStack chestplate = leather(Material.LEATHER_CHESTPLATE, color);
    Material leggings =
        switch (tier) {
          case 1 -> Material.CHAINMAIL_LEGGINGS;
          case 2 -> Material.IRON_LEGGINGS;
          case 3 -> Material.DIAMOND_LEGGINGS;
          default -> Material.LEATHER_LEGGINGS;
        };
    Material boots =
        switch (tier) {
          case 1 -> Material.CHAINMAIL_BOOTS;
          case 2 -> Material.IRON_BOOTS;
          case 3 -> Material.DIAMOND_BOOTS;
          default -> Material.LEATHER_BOOTS;
        };
    ItemStack legItem =
        tier == 0 ? leather(leggings, color) : durable(new ItemStack(leggings), protection);
    ItemStack bootItem =
        tier == 0 ? leather(boots, color) : durable(new ItemStack(boots), protection);
    protect(helmet, protection);
    protect(chestplate, protection);
    protect(legItem, protection);
    protect(bootItem, protection);
    player.getInventory().setHelmet(helmet);
    player.getInventory().setChestplate(chestplate);
    player.getInventory().setLeggings(legItem);
    player.getInventory().setBoots(bootItem);
  }

  private static ItemStack leather(Material material, String teamColor) {
    ItemStack stack = new ItemStack(material);
    if (stack.getItemMeta() instanceof LeatherArmorMeta meta) {
      meta.setColor(color(teamColor));
      meta.setUnbreakable(true);
      stack.setItemMeta(meta);
    }
    return stack;
  }

  private static void removeTools(Player player) {
    for (int slot = 0; slot < player.getInventory().getStorageContents().length; slot++) {
      ItemStack stack = player.getInventory().getItem(slot);
      if (stack == null) continue;
      String name = stack.getType().name();
      if (name.endsWith("_PICKAXE") || name.endsWith("_AXE") || stack.getType() == Material.SHEARS)
        player.getInventory().setItem(slot, null);
    }
  }

  private static void addTool(Player player, Material material, int tier) {
    if (material == null) return;
    ItemStack stack = durable(new ItemStack(material), 0);
    Enchantment efficiency = enchantment("efficiency");
    if (efficiency != null && tier > 0) stack.addUnsafeEnchantment(efficiency, Math.min(4, tier));
    player.getInventory().addItem(stack);
  }

  private static void ensureWoodenSword(Player player) {
    boolean hasSword =
        java.util.Arrays.stream(player.getInventory().getStorageContents())
            .filter(Objects::nonNull)
            .anyMatch(stack -> stack.getType().name().endsWith("_SWORD"));
    if (!hasSword) player.getInventory().addItem(durable(new ItemStack(Material.WOODEN_SWORD), 0));
  }

  private static void applySharpness(Player player, int level) {
    Enchantment sharpness = enchantment("sharpness");
    if (sharpness == null) return;
    for (ItemStack stack : player.getInventory().getStorageContents()) {
      if (stack == null || !stack.getType().name().endsWith("_SWORD")) continue;
      stack.removeEnchantment(sharpness);
      if (level > 0) stack.addUnsafeEnchantment(sharpness, level);
    }
  }

  private static void applyHaste(Player player, int level) {
    PotionEffectType haste = PotionEffectType.getByKey(NamespacedKey.minecraft("haste"));
    if (haste == null) return;
    player.removePotionEffect(haste);
    if (level > 0)
      player.addPotionEffect(
          new PotionEffect(haste, Integer.MAX_VALUE, level - 1, true, false, true));
  }

  private static void protect(ItemStack stack, int level) {
    if (level < 1) return;
    Enchantment protection = enchantment("protection");
    if (protection != null) stack.addUnsafeEnchantment(protection, level);
  }

  private static ItemStack durable(ItemStack stack, int protection) {
    ItemMeta meta = stack.getItemMeta();
    if (meta != null) {
      meta.setUnbreakable(true);
      stack.setItemMeta(meta);
    }
    protect(stack, protection);
    return stack;
  }

  private static Enchantment enchantment(String key) {
    return Enchantment.getByKey(NamespacedKey.minecraft(key));
  }

  private static Material pickaxe(int tier) {
    return switch (tier) {
      case 1 -> Material.WOODEN_PICKAXE;
      case 2 -> Material.IRON_PICKAXE;
      case 3 -> Material.GOLDEN_PICKAXE;
      case 4 -> Material.DIAMOND_PICKAXE;
      default -> null;
    };
  }

  private static Material axe(int tier) {
    return switch (tier) {
      case 1 -> Material.WOODEN_AXE;
      case 2 -> Material.STONE_AXE;
      case 3 -> Material.IRON_AXE;
      case 4 -> Material.DIAMOND_AXE;
      default -> null;
    };
  }

  private static Color color(String value) {
    return switch (Objects.requireNonNullElse(value, "WHITE").toUpperCase(Locale.ROOT)) {
      case "RED" -> Color.RED;
      case "BLUE" -> Color.BLUE;
      case "GREEN" -> Color.GREEN;
      case "YELLOW" -> Color.YELLOW;
      case "AQUA" -> Color.AQUA;
      case "PINK" -> Color.FUCHSIA;
      case "GRAY" -> Color.GRAY;
      case "LIME" -> Color.LIME;
      case "ORANGE" -> Color.ORANGE;
      case "PURPLE" -> Color.PURPLE;
      case "BLACK" -> Color.BLACK;
      default -> Color.WHITE;
    };
  }
}
