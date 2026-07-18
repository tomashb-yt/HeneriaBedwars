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
import fr.heneria.bedwars.core.config.GameSettings;
import fr.heneria.bedwars.core.game.countdown.GameCountdownService;
import fr.heneria.bedwars.core.game.event.BedDestroyedEvent;
import fr.heneria.bedwars.core.game.event.GameCreateEvent;
import fr.heneria.bedwars.core.game.event.GameDestroyEvent;
import fr.heneria.bedwars.core.game.event.GameEvent;
import fr.heneria.bedwars.core.game.event.GameEventBus;
import fr.heneria.bedwars.core.game.event.GameVictoryEvent;
import fr.heneria.bedwars.core.game.event.GameWaitingEvent;
import fr.heneria.bedwars.core.game.event.PlayerGameJoinEvent;
import fr.heneria.bedwars.core.game.event.PlayerGameRespawnEvent;
import fr.heneria.bedwars.core.game.event.ShopPurchaseEvent;
import fr.heneria.bedwars.core.game.lobby.GameLobbyService;
import fr.heneria.bedwars.core.game.shop.ShopCategory;
import fr.heneria.bedwars.core.game.shop.ShopCurrency;
import fr.heneria.bedwars.core.game.shop.ShopInventory;
import fr.heneria.bedwars.core.game.shop.ShopOffer;
import fr.heneria.bedwars.core.game.shop.ShopOfferId;
import fr.heneria.bedwars.core.game.shop.ShopPurchaseCode;
import fr.heneria.bedwars.core.game.shop.ShopPurchaseService;
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
import java.util.function.Function;
import java.util.function.Supplier;
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
    assertTrue(fixture.eventTypes().contains(PlayerGameJoinEvent.class));
  }

  @Test
  void startLocationUsesTheAssignedRuntimeTeamSpawn() {
    GameInstance game = instance();
    game.transition(GameState.WAITING, NOW.plusSeconds(1));
    UUID playerId = UUID.randomUUID();
    RuntimePlayer player = game.addPlayer(playerId, NOW.plusSeconds(2));
    RuntimeLocation spawn = new RuntimeLocation(12.5, 70, -4.5, 90, 0);
    game.team(player.teamId().orElseThrow()).orElseThrow().spawn(spawn);

    assertEquals(spawn, game.startLocation(playerId).orElseThrow());
    assertTrue(game.startLocation(UUID.randomUUID()).isEmpty());
  }

  @Test
  void onlyPlayerPlacedBlocksCanBeConsumedFromTheRuntimeMap() {
    GameInstance game = instance();
    RuntimeBlockPosition placed = new RuntimeBlockPosition(4, 65, -2);
    game.transition(GameState.WAITING, NOW.plusSeconds(1));
    assertFalse(game.recordPlacedBlock(placed));
    game.transition(GameState.STARTING, NOW.plusSeconds(2));
    game.transition(GameState.PLAYING, NOW.plusSeconds(3));

    assertTrue(game.recordPlacedBlock(placed));
    assertFalse(game.recordPlacedBlock(placed));
    assertTrue(game.isPlacedBlock(placed));
    assertEquals(1, game.placedBlockCount());
    assertTrue(game.removePlacedBlock(placed));
    assertFalse(game.removePlacedBlock(placed));
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

  @Test
  void destructionWaitsForLobbyReturnBeforeDeletingTheRuntimeWorld() {
    Fixture fixture = new Fixture();
    fixture.players.finished = new CompletableFuture<>();
    GameInstance game =
        fixture.manager.create("arena1").toCompletableFuture().join().instance().orElseThrow();
    UUID player = UUID.randomUUID();
    fixture.manager.join(game.id(), player).toCompletableFuture().join();

    CompletableFuture<GameOperationResult> destruction =
        fixture.manager.destroy(game.id(), "victory").toCompletableFuture();
    assertFalse(destruction.isDone());
    assertFalse(fixture.worlds.destroyed);
    fixture.players.finished.complete(true);

    assertTrue(destruction.join().successful());
    assertTrue(fixture.worlds.destroyed);
  }

  @Test
  void lobbyStartsCancelsAndCompletesCountdownThroughStateMachine() {
    Fixture fixture = new Fixture();
    GameCountdownService countdowns = countdowns(fixture, settings(2, false, 30));
    GameLobbyService lobby =
        new GameLobbyService(
            fixture.manager,
            countdowns,
            () -> settings(2, false, 30),
            Clock.fixed(NOW, ZoneOffset.UTC));
    GameInstance game =
        fixture.manager.create("arena1").toCompletableFuture().join().instance().orElseThrow();
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();

    assertTrue(lobby.join(game.id(), first).toCompletableFuture().join().successful());
    assertEquals(GameState.WAITING, game.state());
    assertTrue(lobby.join(game.id(), second).toCompletableFuture().join().successful());
    assertEquals(GameState.STARTING, game.state());
    assertTrue(countdowns.snapshot(game.id()).isPresent());

    assertTrue(lobby.leave(second).toCompletableFuture().join().successful());
    assertEquals(GameState.WAITING, game.state());
    assertTrue(countdowns.snapshot(game.id()).isEmpty());

    assertTrue(lobby.join(game.id(), second).toCompletableFuture().join().successful());
    lobby.tick();
    lobby.tick();
    assertEquals(GameState.PLAYING, game.state());
    assertTrue(countdowns.snapshot(game.id()).isEmpty());
  }

  @Test
  void emptyWaitingInstanceIsDestroyedByCentralLobbyTicker() {
    Fixture fixture = new Fixture();
    GameSettings settings = settings(10, true, 0);
    GameLobbyService lobby =
        new GameLobbyService(
            fixture.manager,
            countdowns(fixture, settings),
            () -> settings,
            Clock.fixed(NOW, ZoneOffset.UTC));
    GameInstance game =
        fixture.manager.create("arena1").toCompletableFuture().join().instance().orElseThrow();
    UUID player = UUID.randomUUID();

    assertTrue(lobby.join(game.id(), player).toCompletableFuture().join().successful());
    assertTrue(lobby.leave(player).toCompletableFuture().join().successful());
    lobby.tick();

    assertEquals(0, fixture.manager.size());
    assertTrue(fixture.worlds.destroyed);
  }

  @Test
  void forceStartPassesThroughStartingToPlayingWithoutMinimumPlayers() {
    Fixture fixture = new Fixture();
    GameSettings settings = settings(10, true, 30);
    GameCountdownService countdowns = countdowns(fixture, settings);
    GameLobbyService lobby =
        new GameLobbyService(
            fixture.manager, countdowns, () -> settings, Clock.fixed(NOW, ZoneOffset.UTC));
    GameInstance game =
        fixture.manager.create("arena1").toCompletableFuture().join().instance().orElseThrow();

    assertTrue(lobby.start(game.id(), true).successful());
    assertEquals(GameState.PLAYING, game.state());
  }

  @Test
  void shortGameLookupRefusesAmbiguityButAcceptsTheFullUuid() {
    UUID firstId = UUID.fromString("00000000-0000-0000-0000-000000000009");
    UUID secondId = UUID.fromString("000000ff-0000-0000-0000-000000000010");
    java.util.concurrent.atomic.AtomicInteger index =
        new java.util.concurrent.atomic.AtomicInteger();
    Fixture fixture =
        new Fixture(
            () -> index.getAndIncrement() == 0 ? firstId : secondId,
            id ->
                id.equals("arena1") || id.equals("arena2")
                    ? Optional.of(arena(id))
                    : Optional.empty());
    GameInstance first =
        fixture.manager.create("arena1").toCompletableFuture().join().instance().orElseThrow();
    fixture.manager.create("arena2").toCompletableFuture().join();

    assertEquals(GameLookupStatus.AMBIGUOUS, fixture.manager.lookup("000000").status());
    assertEquals(
        first.id(), fixture.manager.lookup(first.id().toString()).instance().orElseThrow().id());
  }

  @Test
  void bedIndexProtectsOwnBedAndCreditsOnlyOneEnemyDestroyer() {
    Fixture fixture = new Fixture();
    GameInstance game =
        fixture.manager.create("arena1").toCompletableFuture().join().instance().orElseThrow();
    UUID red = UUID.randomUUID();
    UUID blue = UUID.randomUUID();
    fixture.manager.join(game.id(), red).toCompletableFuture().join();
    fixture.manager.join(game.id(), blue).toCompletableFuture().join();
    fixture.manager.selectTeam(red, "red");
    fixture.manager.selectTeam(blue, "blue");
    game.registerBed("red", new RuntimeBlockPosition(0, 64, 0), new RuntimeBlockPosition(1, 64, 0));
    game.registerBed(
        "blue", new RuntimeBlockPosition(10, 64, 0), new RuntimeBlockPosition(11, 64, 0));
    fixture.manager.transition(game.id(), GameState.STARTING);
    fixture.manager.transition(game.id(), GameState.PLAYING);
    GameBedService beds =
        new GameBedService(fixture.manager, fixture.events, Clock.fixed(NOW, ZoneOffset.UTC));

    assertEquals(
        BedDestroyCode.OWN_BED, beds.destroy(red, new RuntimeBlockPosition(0, 64, 0)).code());
    assertEquals(
        BedDestroyCode.DESTROYED, beds.destroy(blue, new RuntimeBlockPosition(1, 64, 0)).code());
    assertEquals(
        BedDestroyCode.ALREADY_DESTROYED,
        beds.destroy(blue, new RuntimeBlockPosition(0, 64, 0)).code());
    assertFalse(game.team("red").orElseThrow().bedAlive());
    assertEquals(1, game.player(blue).orElseThrow().snapshot(NOW).bedsDestroyed());
    assertEquals(1, fixture.published.stream().filter(BedDestroyedEvent.class::isInstance).count());
  }

  @Test
  void aliveBedSchedulesOneCentralRespawnWithProtection() {
    Fixture fixture = new Fixture();
    GameInstance game = playingGame(fixture);
    UUID red = game.team("red").orElseThrow().playerIds().iterator().next();
    GameDeathService deaths =
        new GameDeathService(
            fixture.manager, fixture.events, Clock.fixed(NOW, ZoneOffset.UTC), () -> 5);

    assertEquals(DeathDecision.RESPAWN, deaths.handle(red, null).decision());
    assertTrue(game.player(red).orElseThrow().respawning());
    new GameRespawnService(
            fixture.manager,
            fixture.events,
            Clock.fixed(NOW.plusSeconds(6), ZoneOffset.UTC),
            () -> 3)
        .tick();

    RuntimePlayer player = game.player(red).orElseThrow();
    assertFalse(player.respawning());
    assertTrue(player.protectedAt(NOW.plusSeconds(7)));
    assertTrue(fixture.eventTypes().contains(PlayerGameRespawnEvent.class));
  }

  @Test
  void finalDeathEliminatesTeamAndTransitionsToEnding() {
    Fixture fixture = new Fixture();
    GameInstance game = playingGame(fixture);
    UUID red = game.team("red").orElseThrow().playerIds().iterator().next();
    UUID blue = game.team("blue").orElseThrow().playerIds().iterator().next();
    GameBedService beds =
        new GameBedService(fixture.manager, fixture.events, Clock.fixed(NOW, ZoneOffset.UTC));
    assertTrue(beds.destroy(blue, new RuntimeBlockPosition(0, 64, 0)).successful());
    GameDeathService deaths =
        new GameDeathService(
            fixture.manager, fixture.events, Clock.fixed(NOW, ZoneOffset.UTC), () -> 5);

    GameDeathResult result = deaths.handle(red, blue);

    assertEquals(DeathDecision.FINAL_DEATH, result.decision());
    assertEquals(Optional.of("red"), result.eliminatedTeam());
    assertEquals(Optional.of("blue"), result.winnerTeam());
    assertEquals(GameState.ENDING, game.state());
    assertEquals(1, game.player(blue).orElseThrow().snapshot(NOW).finalKills());
    assertTrue(fixture.eventTypes().contains(GameVictoryEvent.class));
  }

  @Test
  void shopPurchaseIsAtomicAndPublishesAnInternalEvent() {
    Fixture fixture = new Fixture();
    GameInstance game = playingGame(fixture);
    UUID playerId = game.team("red").orElseThrow().playerIds().iterator().next();
    FakeShopInventory inventory = new FakeShopInventory(12, true, true);
    ShopOffer offer =
        new ShopOffer(
            new ShopOfferId("wool"),
            ShopCategory.BLOCKS,
            "WHITE_WOOL",
            16,
            ShopCurrency.IRON,
            4,
            "shop.offer.wool",
            10);

    var result =
        new ShopPurchaseService(fixture.manager, fixture.events, Clock.fixed(NOW, ZoneOffset.UTC))
            .purchase(playerId, offer, inventory);

    assertEquals(ShopPurchaseCode.SUCCESS, result.code());
    assertEquals(8, result.balance());
    assertEquals(1, inventory.exchanges);
    assertTrue(fixture.eventTypes().contains(ShopPurchaseEvent.class));
  }

  @Test
  void shopPurchaseNeverExchangesWhenFundsOrSpaceAreMissing() {
    Fixture fixture = new Fixture();
    GameInstance game = playingGame(fixture);
    UUID playerId = game.team("red").orElseThrow().playerIds().iterator().next();
    ShopOffer offer =
        new ShopOffer(
            new ShopOfferId("tnt"),
            ShopCategory.UTILITY,
            "TNT",
            1,
            ShopCurrency.GOLD,
            4,
            "shop.offer.tnt",
            10);
    ShopPurchaseService purchases =
        new ShopPurchaseService(fixture.manager, fixture.events, Clock.fixed(NOW, ZoneOffset.UTC));
    FakeShopInventory poor = new FakeShopInventory(3, true, true);
    FakeShopInventory full = new FakeShopInventory(8, false, true);

    assertEquals(
        ShopPurchaseCode.INSUFFICIENT_FUNDS, purchases.purchase(playerId, offer, poor).code());
    assertEquals(ShopPurchaseCode.INVENTORY_FULL, purchases.purchase(playerId, offer, full).code());
    assertEquals(0, poor.exchanges);
    assertEquals(0, full.exchanges);
  }

  private static GameInstance playingGame(Fixture fixture) {
    GameInstance game =
        fixture.manager.create("arena1").toCompletableFuture().join().instance().orElseThrow();
    UUID red = UUID.randomUUID();
    UUID blue = UUID.randomUUID();
    fixture.manager.join(game.id(), red).toCompletableFuture().join();
    fixture.manager.join(game.id(), blue).toCompletableFuture().join();
    fixture.manager.selectTeam(red, "red");
    fixture.manager.selectTeam(blue, "blue");
    game.registerBed("red", new RuntimeBlockPosition(0, 64, 0), new RuntimeBlockPosition(1, 64, 0));
    game.registerBed(
        "blue", new RuntimeBlockPosition(10, 64, 0), new RuntimeBlockPosition(11, 64, 0));
    fixture.manager.transition(game.id(), GameState.STARTING);
    fixture.manager.transition(game.id(), GameState.PLAYING);
    return game;
  }

  private static GameCountdownService countdowns(Fixture fixture, GameSettings settings) {
    return new GameCountdownService(
        fixture.manager, fixture.events, () -> settings, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private static GameSettings settings(
      int normalCountdownSeconds, boolean destroyEmptyInstance, int emptyDestroyDelaySeconds) {
    return new GameSettings(
        "ADVENTURE",
        true,
        true,
        true,
        true,
        -64,
        destroyEmptyInstance,
        emptyDestroyDelaySeconds,
        true,
        normalCountdownSeconds,
        1,
        true,
        true,
        java.util.Set.of(5),
        java.util.Set.of(3),
        true,
        "BLUE",
        "SOLID",
        true,
        20,
        true,
        "title",
        java.util.List.of("waiting"),
        java.util.List.of("starting"),
        java.util.List.of("playing"),
        "test",
        "test.local",
        7,
        1,
        500,
        true,
        10);
  }

  private static GameInstance instance() {
    GameId id = new GameId(GAME_ID);
    return new GameInstance(new RuntimeArena(id, arena(), template()), NOW);
  }

  private static ArenaDefinition arena() {
    return arena("arena1");
  }

  private static ArenaDefinition arena(String id) {
    ArenaLocation waiting = new ArenaLocation("hbw_template_map1", new ArenaVector(1, 65, 1), 0, 0);
    return new ArenaDefinition(
        ArenaDefinition.CURRENT_CONFIG_VERSION,
        1,
        ArenaId.parse(id),
        "Arena " + id,
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
      this(() -> GAME_ID, id -> id.equals("arena1") ? Optional.of(arena()) : Optional.empty());
    }

    Fixture(Supplier<UUID> ids, Function<String, Optional<ArenaDefinition>> arenaFinder) {
      events.subscribe(published::add);
      manager =
          new GameInstanceManager(
              arenaFinder,
              ignored -> true,
              id -> id.equals("map1") ? Optional.of(template()) : Optional.empty(),
              worlds,
              players,
              events,
              Clock.fixed(NOW, ZoneOffset.UTC),
              ids);
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

  private static final class FakeShopInventory implements ShopInventory {
    private int balance;
    private final boolean space;
    private final boolean exchangeSucceeds;
    private int exchanges;

    private FakeShopInventory(int balance, boolean space, boolean exchangeSucceeds) {
      this.balance = balance;
      this.space = space;
      this.exchangeSucceeds = exchangeSucceeds;
    }

    @Override
    public int balance(ShopCurrency currency) {
      return balance;
    }

    @Override
    public boolean canExchange(ShopOffer offer) {
      return space;
    }

    @Override
    public boolean exchange(ShopOffer offer) {
      exchanges++;
      if (!exchangeSucceeds) return false;
      balance -= offer.price();
      return true;
    }
  }

  private static final class FakePlayers implements RuntimePlayerGateway {
    CompletableFuture<Boolean> finished = CompletableFuture.completedFuture(true);

    @Override
    public CompletionStage<Boolean> enter(
        UUID playerId,
        RuntimeWorldHandle world,
        RuntimeLocation destination,
        WaitingPlayerContext context) {
      return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletionStage<Boolean> leave(UUID playerId) {
      return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletionStage<Boolean> finish(UUID playerId) {
      return finished;
    }
  }
}
