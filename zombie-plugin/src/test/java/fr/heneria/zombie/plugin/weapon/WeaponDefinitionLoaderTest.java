package fr.heneria.zombie.plugin.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class WeaponDefinitionLoaderTest {

  @Test
  void loadsACompleteDataDrivenWeapon() {
    var definition = WeaponDefinitionLoader.parse(yaml("CROSSBOW", "BURST", 12), "test.yml");

    assertEquals("test_weapon", definition.id());
    assertEquals(12, definition.damage().baseDamage());
    assertEquals(1, definition.upgrades().size());
  }

  @Test
  void rejectsUnknownMaterialModeAndInvalidDamage() {
    assertThrows(
        IllegalArgumentException.class,
        () -> WeaponDefinitionLoader.parse(yaml("NOT_A_MATERIAL", "BURST", 12), "material.yml"));
    assertThrows(
        IllegalArgumentException.class,
        () -> WeaponDefinitionLoader.parse(yaml("CROSSBOW", "INVALID", 12), "mode.yml"));
    assertThrows(
        IllegalArgumentException.class,
        () -> WeaponDefinitionLoader.parse(yaml("CROSSBOW", "BURST", -1), "damage.yml"));
  }

  private static YamlConfiguration yaml(String material, String mode, double damage) {
    return YamlConfiguration.loadConfiguration(
        new StringReader(
            """
            schema-version: 1
            id: test_weapon
            display-name: Test
            category: ASSAULT_RIFLE
            rarity: COMMON
            presentation:
              material: %s
              upgraded-material: CROSSBOW
            fire:
              mode: %s
              cooldown-ticks: 3
              burst-size: 3
              burst-interval-ticks: 1
              pellets: 1
              consumes-ammo: true
            damage:
              base: %s
              minimum-distance: 0
              maximum-distance: 40
              minimum-multiplier: 0.5
              headshot-multiplier: 2
              type: BULLET
            ammo:
              magazine-size: 30
              starting-reserve: 60
              maximum-reserve: 120
            reload:
              duration-ticks: 40
              interruptible: true
            penetration:
              maximum-targets: 2
              damage-retention: 0.7
            economy:
              wall-cost: 1000
              ammo-cost: 500
              mystery-cost: 950
            mystery-box:
              weight: 100
            upgrades:
              - level: 1
                display-name: Test PAP
                cost: 5000
                damage-multiplier: 1.5
                magazine-bonus: 10
                custom-model-data: 1
                effects: [GLOW]
            """
                .formatted(material, mode, damage)));
  }
}
