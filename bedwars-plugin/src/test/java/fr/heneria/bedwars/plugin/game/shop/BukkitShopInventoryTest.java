package fr.heneria.bedwars.plugin.game.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class BukkitShopInventoryTest {
  @Test
  void mapsEveryRuntimeTeamColorToItsWool() {
    assertEquals(Material.RED_WOOL, BukkitShopInventory.teamWool("RED"));
    assertEquals(Material.BLUE_WOOL, BukkitShopInventory.teamWool("BLUE"));
    assertEquals(Material.LIGHT_BLUE_WOOL, BukkitShopInventory.teamWool("AQUA"));
    assertEquals(Material.LIME_WOOL, BukkitShopInventory.teamWool("LIME"));
    assertEquals(Material.WHITE_WOOL, BukkitShopInventory.teamWool("unknown"));
  }
}
