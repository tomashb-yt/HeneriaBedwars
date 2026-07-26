package fr.heneria.zombie.core.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.heneria.zombie.core.enemy.ZombieDamageType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WeaponEngineDomainTest {

  @Test
  void appliesDistanceHeadshotUpgradeAndPenetrationMultipliers() {
    var calculator = new WeaponDamageCalculator();
    WeaponDefinition weapon = weapon("rifle", 10);

    assertEquals(20, calculator.calculate(weapon, 0, true, 1, 0, 1));
    assertEquals(5, calculator.calculate(weapon, 40, false, 1, 0, 1));
    assertEquals(10.5, calculator.calculate(weapon, 0, false, 1.5, 1, 1));
  }

  @Test
  void controlsCadenceAmmoReloadAndUpgradeWithoutScheduler() {
    WeaponInstance instance =
        new WeaponInstance(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), weapon("rifle", 10), 0);

    assertEquals(WeaponInstance.FireDecision.FIRED, instance.tryFire(0, false));
    assertEquals(WeaponInstance.FireDecision.COOLDOWN, instance.tryFire(1, false));
    assertEquals(WeaponInstance.FireDecision.FIRED, instance.tryFire(2, false));
    assertTrue(instance.beginReload(3));
    assertFalse(instance.completeReload(4));
    assertTrue(instance.completeReload(8));
    assertTrue(instance.upgrade());
    assertEquals(1.5, instance.damageMultiplier());
    assertEquals(7, instance.magazineCapacity());
  }

  @Test
  void mysteryBoxRespectsBlacklistAndWonderFilter() {
    WeaponDefinition normal = weapon("normal", 10);
    WeaponDefinition wonder =
        copy(normal, "wonder", WeaponDefinition.WeaponCategory.WONDER_WEAPON, 100);
    MysteryBoxSelector selector = new MysteryBoxSelector();

    assertEquals(
        "normal",
        selector
            .select(List.of(wonder, normal), Set.of(), false, bound -> bound - 1)
            .orElseThrow()
            .id());
    assertTrue(selector.select(List.of(normal), Set.of("normal"), true, ignored -> 0).isEmpty());
  }

  @Test
  void runtimeIndexesAreIsolatedAndCleanedPerGame() {
    WeaponService service = new WeaponService();
    UUID firstGame = UUID.randomUUID();
    UUID secondGame = UUID.randomUUID();
    UUID player = UUID.randomUUID();
    service.create(firstGame, player, weapon("one", 10), 0);
    service.create(secondGame, player, weapon("two", 10), 0);

    service.removeGame(firstGame);

    assertTrue(service.game(firstGame).isEmpty());
    assertEquals(1, service.game(secondGame).size());
    assertEquals(1, service.player(player).size());
  }

  @Test
  void rejectsInvalidDefinitionsAndDuplicateIds() {
    WeaponDefinition invalid = weapon("invalid", -1);
    assertFalse(invalid.validate().isEmpty());
    assertThrows(
        IllegalArgumentException.class,
        () -> new WeaponRegistry(List.of(weapon("same", 10), weapon("same", 10))));
  }

  private static WeaponDefinition weapon(String id, double damage) {
    return new WeaponDefinition(
        id,
        id,
        WeaponDefinition.WeaponCategory.ASSAULT_RIFLE,
        WeaponDefinition.WeaponRarity.COMMON,
        new WeaponDefinition.Presentation("CROSSBOW", 0, "CROSSBOW"),
        new WeaponDefinition.Fire(WeaponDefinition.FireMode.AUTOMATIC, 2, 1, 1, 0, 1, true),
        new WeaponDefinition.Damage(damage, 0, 40, 0.5, 2, ZombieDamageType.BULLET),
        new WeaponDefinition.Ammo(5, 10, 20, false, 0),
        new WeaponDefinition.Reload(5, true),
        new WeaponDefinition.Spread(0, 0, 0, 0, 0),
        new WeaponDefinition.Recoil(0, 0, 0),
        new WeaponDefinition.Penetration(2, 0.7, Set.of()),
        new WeaponDefinition.Economy(500, 250, 950),
        100,
        List.of(new WeaponDefinition.Upgrade(1, "upgraded", 5000, 1.5, 2, 0, Set.of())),
        Map.of(),
        Set.of());
  }

  private static WeaponDefinition copy(
      WeaponDefinition source, String id, WeaponDefinition.WeaponCategory category, int weight) {
    return new WeaponDefinition(
        id,
        id,
        category,
        source.rarity(),
        source.presentation(),
        source.fire(),
        source.damage(),
        source.ammo(),
        source.reload(),
        source.spread(),
        source.recoil(),
        source.penetration(),
        source.economy(),
        weight,
        source.upgrades(),
        source.sounds(),
        source.effects());
  }
}
