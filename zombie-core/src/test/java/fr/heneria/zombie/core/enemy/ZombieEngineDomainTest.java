package fr.heneria.zombie.core.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.heneria.zombie.core.editor.MapPoint;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ZombieEngineDomainTest {

  @Test
  void rejectsInvalidIdentifierAndNegativeHealth() {
    assertThrows(IllegalArgumentException.class, () -> definition("Invalid Id", 20, 1, 100));
    ZombieDefinition invalid = definition("invalid_health", -1, 1, 100);
    assertFalse(invalid.validate().isEmpty());
  }

  @Test
  void registryRejectsDuplicateAndReturnsProtectedCollections() {
    ZombieDefinition definition = definition("classic", 20, 1, 100);
    assertThrows(
        IllegalArgumentException.class,
        () -> new ZombieDefinitionRegistry(List.of(definition, definition)));
    ZombieDefinitionRegistry registry = new ZombieDefinitionRegistry(List.of(definition));
    assertEquals(definition, registry.find("classic").orElseThrow());
    assertThrows(UnsupportedOperationException.class, () -> registry.all().clear());
  }

  @Test
  void attributesApplyRoundAndDirectorMultipliersInOrder() {
    ZombieAttributeCalculator.CalculatedAttributes calculated =
        new ZombieAttributeCalculator().calculate(definition("classic", 20, 1, 100), 3, 1.5, 2);
    assertEquals(36.3, calculated.maximumHealth(), 0.0001);
    assertEquals(6.0, calculated.attackDamage(), 0.0001);
  }

  @Test
  void weightedSelectionFiltersRoundZonePoolCategoryAndAliveCap() {
    ZombieDefinition early = definition("early", 20, 1, 10);
    ZombieDefinition late = definition("late", 20, 5, 100);
    ZombieSelectionService service = new ZombieSelectionService();
    var context =
        new ZombieSelectionService.SelectionContext(
            2,
            Optional.empty(),
            "default",
            Set.of(ZombieDefinition.ZombieCategory.NORMAL),
            Map.of());
    assertEquals(
        "early", service.select(List.of(early, late), context, ignored -> 0).orElseThrow().id());
    var capped =
        new ZombieSelectionService.SelectionContext(
            2, Optional.empty(), "default", Set.of(), Map.of("early", 2));
    ZombieDefinition capTwo = withMaximumAlive(early, 2);
    assertTrue(service.select(List.of(capTwo), capped, ignored -> 0).isEmpty());
  }

  @Test
  void damageAppliesImmunityResistanceVulnerabilityAndHeadshot() {
    ZombieInstance normal = instance(definition("classic", 20, 1, 100));
    ZombieDamageService service = new ZombieDamageService();
    ZombieDamageService.Result headshot =
        service.damage(
            normal,
            new ZombieDamageService.Request(
                UUID.randomUUID(),
                ZombieDamageType.BULLET,
                5,
                true,
                Optional.of("rifle"),
                Optional.empty()));
    assertEquals(10, headshot.appliedDamage(), 0.0001);

    ZombieDefinition resistant =
        withDamage(
            definition("elemental", 20, 1, 100),
            new ZombieDefinition.DamageProfile(
                Set.of(ZombieDamageType.POISON),
                Map.of(ZombieDamageType.FIRE, 0.5, ZombieDamageType.ICE, 1.5),
                false,
                2));
    ZombieInstance target = instance(resistant);
    assertTrue(
        service
            .damage(
                target,
                new ZombieDamageService.Request(
                    null, ZombieDamageType.POISON, 5, false, Optional.empty(), Optional.empty()))
            .immune());
    assertEquals(
        2.5,
        service
            .damage(
                target,
                new ZombieDamageService.Request(
                    null, ZombieDamageType.FIRE, 5, false, Optional.empty(), Optional.empty()))
            .appliedDamage(),
        0.0001);
    assertEquals(
        7.5,
        service
            .damage(
                target,
                new ZombieDamageService.Request(
                    null, ZombieDamageType.ICE, 5, false, Optional.empty(), Optional.empty()))
            .appliedDamage(),
        0.0001);
  }

  @Test
  void deathAndRewardsCanOnlyBeClaimedOnce() {
    ZombieInstance zombie = instance(definition("classic", 20, 1, 100));
    assertTrue(zombie.claimDeath(ZombieRemovalReason.KILLED_BY_PLAYER));
    assertFalse(zombie.claimDeath(ZombieRemovalReason.KILLED_BY_PLAYER));
    zombie.completeRemoval();
    assertEquals(ZombieState.DEAD, zombie.snapshot().state());
  }

  @Test
  void trackerMaintainsAllIndexesAndCleansOneGameOnly() {
    ZombieTracker tracker = new ZombieTracker();
    ZombieInstance zombie = instance(definition("classic", 20, 1, 100));
    tracker.register(zombie);
    assertEquals(zombie, tracker.find(zombie.id()).orElseThrow());
    assertEquals(zombie, tracker.findByEntity(zombie.entityId()).orElseThrow());
    assertEquals(1, tracker.game(zombie.gameId()).size());
    tracker.unregister(zombie.id());
    assertEquals(0, tracker.size());
    assertTrue(tracker.game(zombie.gameId()).isEmpty());
  }

  @Test
  void attackAndAbilityCooldownsAreDeterministic() {
    ZombieInstance zombie = instance(definition("classic", 20, 1, 100));
    assertTrue(zombie.canAttack(100));
    zombie.attackedAt(100);
    assertFalse(zombie.canAttack(119));
    assertTrue(zombie.canAttack(120));
    assertTrue(zombie.abilityReady("poison_hit", 10));
    zombie.abilityCooldown("poison_hit", 30);
    assertFalse(zombie.abilityReady("poison_hit", 29));
    assertTrue(zombie.abilityReady("poison_hit", 30));
  }

  @Test
  void stuckDetectionRequiresMovementStateAndTimeout() {
    ZombieInstance zombie = instance(definition("classic", 20, 1, 100));
    zombie.state(ZombieState.MOVING);
    assertFalse(zombie.stuck(159));
    assertTrue(zombie.stuck(160));
    zombie.observedPosition(new MapPoint("world", 2, 64, 0, 0, 0), 160);
    assertFalse(zombie.stuck(200));
  }

  @Test
  void targetSelectionRejectsOtherGamesAndSpectators() {
    UUID game = UUID.randomUUID();
    UUID otherGame = UUID.randomUUID();
    var valid =
        new ZombieTargetSelector.Candidate(
            UUID.randomUUID(), game, "world", true, true, false, 20, 0, 9, false);
    var closerOtherGame =
        new ZombieTargetSelector.Candidate(
            UUID.randomUUID(), otherGame, "world", true, true, false, 20, 0, 1, false);
    var spectator =
        new ZombieTargetSelector.Candidate(
            UUID.randomUUID(), game, "world", true, true, true, 20, 0, 0.5, false);
    assertEquals(
        valid,
        new ZombieTargetSelector()
            .select(
                game,
                "world",
                ZombieDefinition.TargetStrategy.NEAREST_VALID_PLAYER,
                List.of(closerOtherGame, spectator, valid),
                ignored -> 0)
            .orElseThrow());
  }

  @Test
  void eventBusSupportsOnlyPreEventCancellationAndIsolatesFailures() {
    java.util.concurrent.atomic.AtomicInteger failures =
        new java.util.concurrent.atomic.AtomicInteger();
    ZombieEventDispatcher dispatcher =
        new ZombieEventDispatcher(ignored -> failures.incrementAndGet());
    dispatcher.subscribe(
        event -> {
          throw new IllegalStateException("listener failure");
        });
    dispatcher.subscribe(ZombieEvent::cancel);
    ZombieEvent event =
        new ZombieEvent(
            ZombieEvent.Type.PRE_ATTACK,
            UUID.randomUUID(),
            UUID.randomUUID(),
            "classic",
            1,
            Instant.EPOCH,
            Map.of());
    assertFalse(dispatcher.publish(event));
    assertEquals(1, failures.get());
    ZombieEvent immutable =
        new ZombieEvent(
            ZombieEvent.Type.ATTACKED,
            UUID.randomUUID(),
            UUID.randomUUID(),
            "classic",
            1,
            Instant.EPOCH,
            Map.of());
    assertThrows(IllegalStateException.class, immutable::cancel);
  }

  private static ZombieInstance instance(ZombieDefinition definition) {
    return new ZombieInstance(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        1,
        "spawn",
        "world",
        definition,
        new ZombieAttributeCalculator().calculate(definition, 1, 1, 1),
        new MapPoint("world", 0, 64, 0, 0, 0),
        0);
  }

  private static ZombieDefinition definition(
      String id, double health, int minimumRound, int weight) {
    return new ZombieDefinition(
        id,
        id,
        "ZOMBIE",
        ZombieDefinition.ZombieCategory.NORMAL,
        new ZombieDefinition.Attributes(health, true, 1.1, 1000, 3, false, 1, 0.23, 40, 0, 0),
        new ZombieDefinition.Behavior(
            ZombieDefinition.BehaviorType.MELEE,
            ZombieDefinition.TargetStrategy.NEAREST_VALID_PLAYER,
            1.8,
            20,
            20,
            48),
        new ZombieDefinition.Navigation(
            ZombieDefinition.NavigationMode.GROUND,
            false,
            false,
            false,
            true,
            40,
            0.35,
            160,
            3,
            ZombieDefinition.StuckAction.TELEPORT_TO_VALID_SPAWN),
        new ZombieDefinition.DamageProfile(Set.of(), Map.of(), true, 2),
        new ZombieDefinition.SpawnRules(
            minimumRound, Integer.MAX_VALUE, weight, 2, Set.of(), Set.of("default")),
        new ZombieDefinition.Rewards(10, 60, 100, 0),
        List.of(),
        new ZombieDefinition.Environment(false, false),
        new ZombieDefinition.Appearance(false, false, false, false, Map.of()));
  }

  private static ZombieDefinition withMaximumAlive(ZombieDefinition source, int maximum) {
    return new ZombieDefinition(
        source.id(),
        source.displayName(),
        source.entityType(),
        source.category(),
        source.attributes(),
        source.behavior(),
        source.navigation(),
        source.damage(),
        new ZombieDefinition.SpawnRules(
            source.spawnRules().minimumRound(),
            source.spawnRules().maximumRound(),
            source.spawnRules().weight(),
            maximum,
            source.spawnRules().zones(),
            source.spawnRules().pools()),
        source.rewards(),
        source.abilities(),
        source.environment(),
        source.appearance());
  }

  private static ZombieDefinition withDamage(
      ZombieDefinition source, ZombieDefinition.DamageProfile damage) {
    return new ZombieDefinition(
        source.id(),
        source.displayName(),
        source.entityType(),
        source.category(),
        source.attributes(),
        source.behavior(),
        source.navigation(),
        damage,
        source.spawnRules(),
        source.rewards(),
        source.abilities(),
        source.environment(),
        source.appearance());
  }
}
