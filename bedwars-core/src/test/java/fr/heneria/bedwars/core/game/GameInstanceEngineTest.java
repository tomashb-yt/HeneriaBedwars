package fr.heneria.bedwars.core.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.heneria.bedwars.api.game.GameState;
import fr.heneria.bedwars.core.arena.ArenaDefinition;
import fr.heneria.bedwars.core.arena.ArenaId;
import fr.heneria.bedwars.core.arena.ArenaLocation;
import fr.heneria.bedwars.core.arena.ArenaMetadata;
import fr.heneria.bedwars.core.arena.ArenaStatus;
import fr.heneria.bedwars.core.arena.ArenaVector;
import fr.heneria.bedwars.core.game.event.GameCreateEvent;
import fr.heneria.bedwars.core.game.event.GameDestroyEvent;
import fr.heneria.bedwars.core.game.event.GameEvent;
import fr.heneria.bedwars.core.game.event.GameEventBus;
import fr.heneria.bedwars.core.game.event.GameWaitingEvent;
import fr.heneria.bedwars.core.game.event.PlayerJoinGameEvent;
import fr.heneria.bedwars.core.map.MapId;
import fr.heneria.bedwars.core.map.MapTemplate;
import fr.heneria.bedwars.core.map.MapType;
import fr.heneria.bedwars.core.map.MapWorldSettings;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

class GameInstanceEngineTest {
  private static final Instant NOW = Instant.parse("2026-07-16T22:00:00Z");
  private static final UUID GAME_ID = UUID.fromString("00000000-0000-0000-0000-000000000009");

  @Test
  void stateMachineAllowsOnlyDeclaredTransitions() {
    GameInstance game = instance();
    game.timer("countdown", 200, NOW);
    game.incrementStatistic("joins", 1, NOW);
    assertEquals(200, game.snapshot(NOW).timers().get("countdown"));
    assertEquals(1, game.snapshot(NOW).statistics().get("joins"));
    assertEquals(GameState.CREATING, game.state());
    game.transition(GameState.WAITING, NOW.plusSeconds(1));
    game.transition(GameState.STARTING, NOW.plusSeconds(2));
    game.transition(GameState.PLAYING, NOW.plusSeconds(3));
    game.transition(GameState.ENDING, NOW.plusSeconds(4));
    game.transition(GameState.RESETTING, NOW.plusSeconds(5));
    game.transition(GameState.DESTROYED, NOW.plusSeconds(6));
    assertThrows(
        GameTransitionException.class,
        () -> game.transition(GameState.WAITING, NOW.plusSeconds(7)));
  }

  @Test
  void creationReservesArenaPublishesEventsAndReachesWaiting() {
    Fixture fixture = new Fixture();
    GameOperationResult created = fixture.manager.create("arena1").toCompletableFuture().join();
    assertTrue(created.successful());
    assertEquals(GameState.WAITING, created.instance().orElseThrow().state());
    assertEquals(1, fixture.manager.size());
    assertEquals(
        GameOperationCode.ARENA_OCCUPIED,
        fixture.manager.create("arena1").toCompletableFuture().join().code());
    assertEquals(List.of(GameCreateEvent.class, GameWaitingEvent.class), fixture.eventTypes());
  }

  @Test
  void failedCloneRollsBackEveryIndex() {
    Fixture fixture = new Fixture();
    fixture.worlds.failCreate = true;
    assertEquals(
        GameOperationCode.WORLD_CREATION_FAILED,
        fixture.manager.create("arena1").toCompletableFuture().join().code());
    assertEquals(0, fixture.manager.size());
    assertTrue(fixture.manager.byArena("arena1").isEmpty());
  }

  @Test
  void playerJoinCreatesRuntimeStateAndUniqueIndexes() {
    Fixture fixture = new Fixture();
    GameInstance game =
        fixture.manager.create("arena1").toCompletableFuture().join().instance().orElseThrow();
    UUID player = UUID.randomUUID();
    assertTrue(fixture.manager.join(game.id(), player).toCompletableFuture().join().successful());
    assertEquals(game.id(), fixture.manager.byPlayer(player).orElseThrow().id());
    assertEquals(1, game.snapshot(NOW.plusSeconds(4)).players().size());
    assertEquals(
        GameOperationCode.PLAYER_OCCUPIED,
        fixture.manager.join(game.id(), player).toCompletableFuture().join().code());
    assertTrue(fixture.eventTypes().contains(PlayerJoinGameEvent.class));
  }

  @Test
  void destructionEvacuatesPlayersDeletesWorldAndReleasesArena() {
    Fixture fixture = new Fixture();
    GameInstance game =
        fixture.manager.create("arena1").toCompletableFuture().join().instance().orElseThrow();
    UUID player = UUID.randomUUID();
    fixture.manager.join(game.id(), player).toCompletableFuture().join();
    GameOperationResult destroyed =
        fixture.manager.destroy(game.id(), "test").toCompletableFuture().join();
    assertTrue(destroyed.successful());
    assertEquals(GameState.DESTROYED, game.state());
    assertEquals(0, fixture.manager.size());
    assertTrue(fixture.worlds.destroyed);
    assertFalse(fixture.manager.byPlayer(player).isPresent());
    assertTrue(fixture.eventTypes().contains(GameDestroyEvent.class));
  }

  private static GameInstance instance() {
    GameId id = new GameId(GAME_ID);
    return new GameInstance(new RuntimeArena(id, arena(), template()), NOW);
  }

  private static ArenaDefinition arena() {
    ArenaLocation waiting = new ArenaLocation("hbw_template_map1", new ArenaVector(1, 65, 1), 0, 0);
    return new ArenaDefinition(
        ArenaDefinition.CURRENT_CONFIG_VERSION,
        1,
        ArenaId.parse("arena1"),
        "Arena 1",
        ArenaStatus.ENABLED,
        Optional.of("hbw_template_map1"),
        Optional.of("map1"),
        "NORMAL",
        2,
        8,
        2,
        4,
        Optional.of(waiting),
        Optional.empty(),
        Optional.empty(),
        new ArenaMetadata(NOW, NOW, java.util.Map.of()));
  }

  private static MapTemplate template() {
    return MapTemplate.create(
        MapId.parse("map1"),
        MapType.BEDWARS,
        "hbw_template_map1",
        "NORMAL",
        new MapWorldSettings(false, false, false, 6000, true),
        64,
        "Admin",
        NOW);
  }

  private static final class Fixture {
    final FakeWorlds worlds = new FakeWorlds();
    final FakePlayers players = new FakePlayers();
    final GameEventBus events = new GameEventBus();
    final List<GameEvent> published = new ArrayList<>();
    final GameInstanceManager manager;

    Fixture() {
      events.subscribe(published::add);
      manager =
          new GameInstanceManager(
              id -> id.equals("arena1") ? Optional.of(arena()) : Optional.empty(),
              ignored -> true,
              id -> id.equals("map1") ? Optional.of(template()) : Optional.empty(),
              worlds,
              players,
              events,
              Clock.fixed(NOW, ZoneOffset.UTC),
              () -> GAME_ID);
    }

    List<Class<?>> eventTypes() {
      return published.stream().map(Object::getClass).toList();
    }
  }

  private static final class FakeWorlds implements RuntimeWorldService {
    boolean failCreate;
    boolean destroyed;

    @Override
    public CompletionStage<RuntimeWorldHandle> create(RuntimeArena arena) {
      if (failCreate) return CompletableFuture.failedFuture(new IllegalStateException("copy"));
      return CompletableFuture.completedFuture(
          new RuntimeWorldHandle(arena.gameId(), "hbw_game_test", "game-" + arena.gameId()));
    }

    @Override
    public CompletionStage<Void> destroy(RuntimeWorldHandle world) {
      destroyed = true;
      return CompletableFuture.completedFuture(null);
    }
  }

  private static final class FakePlayers implements RuntimePlayerGateway {
    @Override
    public CompletionStage<Boolean> enter(
        UUID playerId, RuntimeWorldHandle world, RuntimeLocation destination) {
      return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletionStage<Boolean> leave(UUID playerId) {
      return CompletableFuture.completedFuture(true);
    }
  }
}
