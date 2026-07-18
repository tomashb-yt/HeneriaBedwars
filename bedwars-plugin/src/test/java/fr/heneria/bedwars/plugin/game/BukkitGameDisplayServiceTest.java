package fr.heneria.bedwars.plugin.game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BukkitGameDisplayServiceTest {
  @Test
  void mapsEveryRuntimeTeamColorToItsMinecraftColor() {
    assertEquals("§c", BukkitGameDisplayService.teamColor("RED"));
    assertEquals("§9", BukkitGameDisplayService.teamColor("BLUE"));
    assertEquals("§2", BukkitGameDisplayService.teamColor("GREEN"));
    assertEquals("§e", BukkitGameDisplayService.teamColor("YELLOW"));
    assertEquals("§b", BukkitGameDisplayService.teamColor("AQUA"));
    assertEquals("§f", BukkitGameDisplayService.teamColor("WHITE"));
    assertEquals("§d", BukkitGameDisplayService.teamColor("PINK"));
    assertEquals("§7", BukkitGameDisplayService.teamColor("GRAY"));
    assertEquals("§a", BukkitGameDisplayService.teamColor("LIME"));
    assertEquals("§6", BukkitGameDisplayService.teamColor("ORANGE"));
    assertEquals("§5", BukkitGameDisplayService.teamColor("PURPLE"));
    assertEquals("§8", BukkitGameDisplayService.teamColor("BLACK"));
    assertEquals("§f", BukkitGameDisplayService.teamColor("unknown"));
  }
}
