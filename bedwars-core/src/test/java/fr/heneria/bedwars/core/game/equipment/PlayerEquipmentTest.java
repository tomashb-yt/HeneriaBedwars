package fr.heneria.bedwars.core.game.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlayerEquipmentTest {
  @Test
  void toolsRequireTheNextTierAndLoseOneTierOnDeath() {
    PlayerEquipment equipment = new PlayerEquipment();
    assertEquals(EquipmentPurchaseCode.WRONG_TIER, equipment.canPurchase(EquipmentKind.PICKAXE, 2));
    assertTrue(equipment.purchase(EquipmentKind.PICKAXE, 1));
    assertTrue(equipment.purchase(EquipmentKind.PICKAXE, 2));
    assertFalse(equipment.purchase(EquipmentKind.PICKAXE, 2));
    equipment.onDeath();
    assertEquals(1, equipment.snapshot().pickaxeTier());
    assertEquals(EquipmentPurchaseCode.AVAILABLE, equipment.canPurchase(EquipmentKind.PICKAXE, 2));
  }

  @Test
  void armorAndShearsRemainPermanentAcrossDeaths() {
    PlayerEquipment equipment = new PlayerEquipment();
    assertTrue(equipment.purchase(EquipmentKind.ARMOR, 2));
    assertTrue(equipment.purchase(EquipmentKind.SHEARS, 1));
    equipment.onDeath();
    assertEquals(2, equipment.snapshot().armorTier());
    assertTrue(equipment.snapshot().shears());
    assertEquals(
        EquipmentPurchaseCode.ALREADY_OWNED, equipment.canPurchase(EquipmentKind.ARMOR, 1));
  }
}
