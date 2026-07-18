package fr.heneria.bedwars.core.game.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fr.heneria.bedwars.core.game.RuntimeLocation;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class GeneratorDefinitionTest {
  @Test
  void normalizesStableIdsAndKeepsPlatformTypesOutsideTheCore() {
    GeneratorDefinition definition = definition(" Diamond-Center ");

    assertEquals("diamond-center", definition.id().value());
    assertEquals(GeneratorResource.DIAMOND, definition.resource());
    assertEquals(Duration.ofSeconds(30), definition.interval());
  }

  @Test
  void rejectsUnsafeOrUnboundedRules() {
    assertThrows(IllegalArgumentException.class, () -> new GeneratorId("x"));
    assertThrows(IllegalArgumentException.class, () -> new GeneratorId("../diamond"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new GeneratorDefinition(
                new GeneratorId("diamond"),
                GeneratorResource.DIAMOND,
                location(),
                1,
                Duration.ZERO,
                1,
                4,
                GeneratorStackingStrategy.MERGE_NEARBY));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new GeneratorDefinition(
                new GeneratorId("diamond"),
                GeneratorResource.DIAMOND,
                location(),
                1,
                Duration.ofSeconds(1),
                0,
                4,
                GeneratorStackingStrategy.MERGE_NEARBY));
  }

  @Test
  void upgradesReturnANewDefinitionWithoutChangingIdentityOrCapacity() {
    GeneratorDefinition original = definition("diamond");
    GeneratorDefinition upgraded = original.withLevel(2, Duration.ofSeconds(20));

    assertEquals(1, original.level());
    assertEquals(Duration.ofSeconds(30), original.interval());
    assertEquals(original.id(), upgraded.id());
    assertEquals(2, upgraded.level());
    assertEquals(Duration.ofSeconds(20), upgraded.interval());
    assertEquals(original.localCapacity(), upgraded.localCapacity());
  }

  @Test
  void populationPacingIsBoundedForSparseAndBusyGames() {
    GeneratorDefinition base = definition("diamond");
    GeneratorPacingPolicy policy = new GeneratorPacingPolicy(true, 0.85, 1.60);

    assertEquals(Duration.ofSeconds(48), policy.adjust(base, 8, 1).interval());
    assertEquals(Duration.ofSeconds(30), policy.adjust(base, 4, 4).interval());
    assertEquals(Duration.ofMillis(25_500), policy.adjust(base, 2, 16).interval());
    assertEquals(
        base.interval(),
        new GeneratorPacingPolicy(false, 0.85, 1.60).adjust(base, 8, 1).interval());
  }

  @Test
  void populationPacingRejectsIncoherentBounds() {
    assertThrows(IllegalArgumentException.class, () -> new GeneratorPacingPolicy(true, 0, 1));
    assertThrows(IllegalArgumentException.class, () -> new GeneratorPacingPolicy(true, 2, 1));
  }

  private static GeneratorDefinition definition(String id) {
    return new GeneratorDefinition(
        new GeneratorId(id),
        GeneratorResource.DIAMOND,
        location(),
        1,
        Duration.ofSeconds(30),
        1,
        4,
        GeneratorStackingStrategy.MERGE_NEARBY);
  }

  private static RuntimeLocation location() {
    return new RuntimeLocation(10.5, 65, -3.5, 0, 0);
  }
}
