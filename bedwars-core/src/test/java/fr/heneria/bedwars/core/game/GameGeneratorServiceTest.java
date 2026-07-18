package fr.heneria.bedwars.core.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.heneria.bedwars.api.game.GameState;
import fr.heneria.bedwars.core.arena.ArenaDefinition;
import fr.heneria.bedwars.core.arena.ArenaId;
import fr.heneria.bedwars.core.game.generator.GameGeneratorService;
import fr.heneria.bedwars.core.game.generator.GeneratorCapacityView;
import fr.heneria.bedwars.core.game.generator.GeneratorDefinition;
import fr.heneria.bedwars.core.game.generator.GeneratorId;
import fr.heneria.bedwars.core.game.generator.GeneratorResource;
import fr.heneria.bedwars.core.game.generator.GeneratorStackingStrategy;
import fr.heneria.bedwars.core.map.MapId;
import fr.heneria.bedwars.core.map.MapTemplate;
import fr.heneria.bedwars.core.map.MapType;
import fr.heneria.bedwars.core.map.MapWorldSettings;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class GameGeneratorServiceTest {
  private static final Instant NOW = Instant.parse("2026-07-18T12:00:00Z");

  @Test
  void emitsOnlyWhenThePlayingWorldIsActiveAndDeadlineIsDue() {
    GameInstance game = game(false, generator("iron", 1, 8));
    GameGeneratorService service = new GameGeneratorService(16);

    assertTrue(
        service
            .tick(List.of(game), NOW.plusSeconds(10), GeneratorCapacityView.empty())
            .emissions()
            .isEmpty());
    start(game, NOW.plusSeconds(10));
    var report = service.tick(List.of(game), NOW.plusSeconds(10), GeneratorCapacityView.empty());

    assertEquals(1, report.emissions().size());
    assertEquals(GeneratorResource.IRON, report.emissions().getFirst().generator().resource());
    assertEquals(NOW.plusSeconds(1), report.emissions().getFirst().scheduledAt());
    assertEquals(NOW.plusSeconds(11), game.generators().getFirst().nextEmissionAt());
  }

  @Test
  void aLongPauseNeverCreatesACatchUpBurst() {
    GameInstance game = game(true, generator("emerald", 2, 8));
    GameGeneratorService service = new GameGeneratorService(16);

    var report = service.tick(List.of(game), NOW.plusSeconds(100), GeneratorCapacityView.empty());

    assertEquals(1, report.emissions().size());
    assertEquals(2, report.emissions().getFirst().amount());
    assertEquals(NOW.plusSeconds(101), game.generators().getFirst().nextEmissionAt());
    assertTrue(
        service
            .tick(List.of(game), NOW.plusSeconds(100), GeneratorCapacityView.empty())
            .emissions()
            .isEmpty());
  }

  @Test
  void localCapacityClipsThenBlocksDrops() {
    GameInstance game = game(true, generator("gold", 3, 5));
    GameGeneratorService service = new GameGeneratorService(16);

    var clipped = service.tick(List.of(game), NOW.plusSeconds(1), (gameId, definition) -> 4);
    var blocked = service.tick(List.of(game), NOW.plusSeconds(2), (gameId, definition) -> 5);

    assertEquals(1, clipped.emissions().getFirst().amount());
    assertEquals(1, blocked.capacityBlocks());
    assertTrue(blocked.emissions().isEmpty());
    assertEquals(1, game.generators().getFirst().capacityBlocks());
  }

  @Test
  void globalBudgetRotatesFairlyBetweenDueGenerators() {
    GameInstance game = game(true, generator("a_iron", 1, 8), generator("b_gold", 1, 8));
    GameGeneratorService service = new GameGeneratorService(1);

    var first = service.tick(List.of(game), NOW.plusSeconds(1), GeneratorCapacityView.empty());
    var second = service.tick(List.of(game), NOW.plusSeconds(1), GeneratorCapacityView.empty());
    var third = service.tick(List.of(game), NOW.plusSeconds(1), GeneratorCapacityView.empty());

    assertEquals("a_iron", first.emissions().getFirst().generator().id().value());
    assertTrue(first.truncated());
    assertEquals("b_gold", second.emissions().getFirst().generator().id().value());
    assertTrue(second.truncated());
    assertTrue(third.emissions().isEmpty());
    assertFalse(third.truncated());
    assertEquals(2, third.visitedGenerators());
  }

  @Test
  void registrationRejectsDuplicatesAndChangesAfterStart() {
    GeneratorDefinition definition = generator("diamond", 1, 8);
    GameInstance game = game(false, definition);

    assertThrows(IllegalStateException.class, () -> game.registerGenerator(definition, NOW));
    start(game, NOW);
    assertThrows(
        IllegalStateException.class,
        () -> game.registerGenerator(generator("emerald", 1, 8), NOW.plusSeconds(1)));
  }

  @Test
  void capacityIsReadOnlyWhenADeadlineIsDue() {
    GameInstance game = game(true, generator("iron", 1, 8));
    GameGeneratorService service = new GameGeneratorService(16);
    AtomicInteger probes = new AtomicInteger();

    service.tick(
        List.of(game),
        NOW,
        (gameId, definition) -> {
          probes.incrementAndGet();
          return 0;
        });

    assertEquals(0, probes.get());
    service.tick(
        List.of(game),
        NOW.plusSeconds(1),
        (gameId, definition) -> {
          probes.incrementAndGet();
          return 0;
        });
    assertEquals(1, probes.get());
  }

  private static GameInstance game(boolean playing, GeneratorDefinition... generators) {
    GameId gameId = new GameId(UUID.randomUUID());
    ArenaDefinition arena = ArenaDefinition.draft(new ArenaId("arena1"), NOW);
    MapTemplate template =
        MapTemplate.create(
            new MapId("template"),
            MapType.BEDWARS,
            "hbw_template",
            "NORMAL",
            new MapWorldSettings(false, false, false, 6000, true),
            64,
            "test",
            NOW);
    GameInstance game = new GameInstance(new RuntimeArena(gameId, arena, template), NOW);
    game.attachWorld(new RuntimeWorldHandle(gameId, "hbw_game_test", "runtime/test/world"), NOW);
    for (GeneratorDefinition generator : generators) game.registerGenerator(generator, NOW);
    if (playing) start(game, NOW);
    else game.transition(GameState.WAITING, NOW);
    return game;
  }

  private static void start(GameInstance game, Instant now) {
    if (game.state() == GameState.CREATING) game.transition(GameState.WAITING, now);
    game.transition(GameState.STARTING, now);
    game.transition(GameState.PLAYING, now);
  }

  private static GeneratorDefinition generator(String id, int amount, int capacity) {
    return new GeneratorDefinition(
        new GeneratorId(id),
        id.contains("gold") ? GeneratorResource.GOLD : GeneratorResource.IRON,
        new RuntimeLocation(0.5, 65, 0.5, 0, 0),
        1,
        Duration.ofSeconds(1),
        amount,
        capacity,
        GeneratorStackingStrategy.MERGE_NEARBY);
  }
}
