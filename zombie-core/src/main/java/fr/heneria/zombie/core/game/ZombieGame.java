package fr.heneria.zombie.core.game;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Thread-safe aggregate owning all business state for one isolated Zombies instance. */
public final class ZombieGame {
  private final UUID gameId;
  private final String mapId;
  private final RoundConfiguration configuration;
  private final Clock clock;
  private final GameEventDispatcher events;
  private final Map<UUID, GamePlayer> players = new LinkedHashMap<>();
  private GameState state = GameState.CREATED;
  private RoundState round;
  private Instant startedAt;
  private Instant endedAt;
  private GameEndReason endReason;

  public ZombieGame(
      UUID gameId,
      String mapId,
      RoundConfiguration configuration,
      Clock clock,
      GameEventDispatcher events) {
    this.gameId = Objects.requireNonNull(gameId, "gameId");
    this.mapId = Objects.requireNonNull(mapId, "mapId");
    this.configuration = Objects.requireNonNull(configuration, "configuration");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.events = Objects.requireNonNull(events, "events");
    transition(GameState.WAITING_FOR_PLAYERS);
    emit(GameEvent.Type.GAME_CREATED);
  }

  public synchronized boolean addPlayer(UUID playerId) {
    if (state != GameState.WAITING_FOR_PLAYERS
        && !(configuration.joinInProgress() && state == GameState.ROUND_ACTIVE)) {
      return false;
    }
    if (players.containsKey(playerId)) {
      return true;
    }
    GamePlayer player = new GamePlayer(playerId, configuration.startingPoints());
    player.state(state == GameState.ROUND_ACTIVE ? GamePlayerState.ALIVE : GamePlayerState.WAITING);
    players.put(playerId, player);
    return true;
  }

  public synchronized void prepare() {
    requireState(GameState.WAITING_FOR_PLAYERS);
    if (continuingPlayers() < configuration.minimumPlayers()) {
      throw new IllegalStateException("Not enough players");
    }
    transition(GameState.PREPARING);
    emit(GameEvent.Type.GAME_PREPARING);
    transition(GameState.COUNTDOWN);
    emit(GameEvent.Type.COUNTDOWN_STARTED);
  }

  public synchronized void cancelCountdown() {
    requireState(GameState.COUNTDOWN);
    transition(GameState.WAITING_FOR_PLAYERS);
  }

  public synchronized void start(int totalEnemies) {
    requireState(GameState.COUNTDOWN);
    transition(GameState.STARTING);
    startedAt = clock.instant();
    players.values().forEach(player -> player.state(GamePlayerState.ALIVE));
    emit(GameEvent.Type.GAME_STARTED);
    startRound(totalEnemies);
  }

  public synchronized void startNextRound(int totalEnemies) {
    requireState(GameState.ROUND_TRANSITION);
    players
        .values()
        .forEach(
            player -> {
              if (player.snapshot().state() == GamePlayerState.DEAD
                  || player.snapshot().state() == GamePlayerState.SPECTATING) {
                player.state(GamePlayerState.ALIVE);
              }
            });
    startRoundAt(round.snapshot(clock.instant()).number() + 1, totalEnemies);
  }

  private void startRound(int totalEnemies) {
    int number = round == null ? 1 : round.snapshot(clock.instant()).number() + 1;
    startRoundAt(number, totalEnemies);
  }

  public synchronized void startRoundAt(int number, int totalEnemies) {
    if (state != GameState.STARTING && state != GameState.ROUND_TRANSITION) {
      throw new IllegalStateException(
          "Round override is only safe while starting or transitioning");
    }
    round = new RoundState(number, totalEnemies, clock.instant());
    round.beginSpawning();
    transition(GameState.ROUND_ACTIVE);
    players.values().forEach(player -> player.maximumRound(number));
    emit(GameEvent.Type.ROUND_STARTED);
  }

  public synchronized int reserveSpawns(int amount, int maximumAlive) {
    requireState(GameState.ROUND_ACTIVE);
    return round.reserveSpawns(amount, maximumAlive);
  }

  public synchronized void spawned() {
    requireState(GameState.ROUND_ACTIVE);
    round.spawned();
    emit(GameEvent.Type.ZOMBIE_REGISTERED);
  }

  public synchronized void spawnFailed() {
    if (state == GameState.ROUND_ACTIVE) {
      round.spawnFailed();
    }
  }

  public synchronized boolean zombieDefeated(UUID killer) {
    return zombieDefeated(killer, configuration.pointsPerKill());
  }

  public synchronized boolean zombieDefeated(UUID killer, int reward) {
    if (reward < 0) {
      throw new IllegalArgumentException("reward cannot be negative");
    }
    if (state != GameState.ROUND_ACTIVE || !round.defeated()) {
      return false;
    }
    Optional.ofNullable(players.get(killer))
        .filter(player -> player.snapshot().state() == GamePlayerState.ALIVE)
        .ifPresent(player -> player.killed(reward));
    emit(GameEvent.Type.ZOMBIE_DEFEATED);
    if (round.canComplete()) {
      completeRound();
    }
    return true;
  }

  public synchronized boolean spendPoints(UUID playerId, int amount) {
    GamePlayer player = players.get(playerId);
    return state == GameState.ROUND_ACTIVE
        && player != null
        && player.snapshot().state() == GamePlayerState.ALIVE
        && player.spend(amount);
  }

  public synchronized boolean weaponHit(
      UUID playerId, int reward, double appliedDamage, boolean headshot) {
    GamePlayer player = players.get(playerId);
    if (state != GameState.ROUND_ACTIVE
        || player == null
        || player.snapshot().state() != GamePlayerState.ALIVE) {
      return false;
    }
    player.weaponHit(reward, appliedDamage, headshot);
    return true;
  }

  public synchronized boolean completeRound() {
    if (state != GameState.ROUND_ACTIVE || !round.complete()) {
      return false;
    }
    emit(GameEvent.Type.ROUND_COMPLETED);
    transition(GameState.ROUND_TRANSITION);
    return true;
  }

  public synchronized boolean down(UUID playerId) {
    GamePlayer player = players.get(playerId);
    if (state != GameState.ROUND_ACTIVE
        || player == null
        || player.snapshot().state() != GamePlayerState.ALIVE) {
      return false;
    }
    player.downed();
    emit(GameEvent.Type.PLAYER_DOWNED);
    return true;
  }

  public synchronized boolean revive(UUID targetId, UUID reviverId) {
    GamePlayer target = players.get(targetId);
    GamePlayer reviver = players.get(reviverId);
    if (target == null
        || reviver == null
        || target.snapshot().state() != GamePlayerState.DOWNED
        || reviver.snapshot().state() != GamePlayerState.ALIVE) {
      return false;
    }
    target.state(GamePlayerState.ALIVE);
    reviver.revived();
    emit(GameEvent.Type.PLAYER_REVIVED);
    return true;
  }

  public synchronized boolean eliminate(UUID playerId) {
    GamePlayer player = players.get(playerId);
    if (player == null
        || (player.snapshot().state() != GamePlayerState.DOWNED
            && player.snapshot().state() != GamePlayerState.ALIVE
            && player.snapshot().state() != GamePlayerState.DISCONNECTED)) {
      return false;
    }
    player.state(GamePlayerState.DEAD);
    emit(GameEvent.Type.PLAYER_ELIMINATED);
    return true;
  }

  public synchronized boolean spectate(UUID playerId) {
    GamePlayer player = players.get(playerId);
    if (player == null || player.snapshot().state() != GamePlayerState.DEAD) {
      return false;
    }
    player.state(GamePlayerState.SPECTATING);
    return true;
  }

  public synchronized void leave(UUID playerId) {
    GamePlayer player = players.get(playerId);
    if (player != null) {
      player.state(GamePlayerState.LEFT);
    }
  }

  public synchronized boolean disconnect(UUID playerId, Instant deadline) {
    GamePlayer player = players.get(playerId);
    if (player == null
        || player.snapshot().state() == GamePlayerState.LEFT
        || player.snapshot().state() == GamePlayerState.DEAD) {
      return false;
    }
    player.disconnectedUntil(deadline);
    return true;
  }

  public synchronized boolean reconnect(UUID playerId) {
    GamePlayer player = players.get(playerId);
    return player != null && player.reconnect(clock.instant());
  }

  public synchronized int expireDisconnectedPlayers() {
    int expired = 0;
    for (GamePlayer player : players.values()) {
      if (player.expireDisconnect(clock.instant())) {
        expired++;
        emit(GameEvent.Type.PLAYER_ELIMINATED);
      }
    }
    return expired;
  }

  public synchronized boolean defeated() {
    return players.values().stream()
        .map(GamePlayer::snapshot)
        .noneMatch(
            player ->
                player.state() == GamePlayerState.ALIVE
                    || player.state() == GamePlayerState.DOWNED
                    || player.state() == GamePlayerState.DISCONNECTED);
  }

  public synchronized Optional<GameResult> end(GameEndReason reason) {
    if (state == GameState.ENDING
        || state == GameState.FINISHED
        || state == GameState.CLEANING
        || state == GameState.FAILED) {
      return Optional.empty();
    }
    if (round != null) {
      round.cancel();
    }
    transition(GameState.ENDING);
    endReason = Objects.requireNonNull(reason, "reason");
    endedAt = clock.instant();
    emit(GameEvent.Type.GAME_ENDING);
    transition(GameState.FINISHED);
    emit(GameEvent.Type.GAME_ENDED);
    return Optional.of(result());
  }

  public synchronized Snapshot snapshot() {
    Instant now = clock.instant();
    Map<UUID, GamePlayer.Snapshot> playerSnapshots = new LinkedHashMap<>();
    players.forEach((id, player) -> playerSnapshots.put(id, player.snapshot()));
    return new Snapshot(
        gameId,
        mapId,
        state,
        Optional.ofNullable(round).map(value -> value.snapshot(now)),
        playerSnapshots,
        Optional.ofNullable(endReason),
        startedAt == null
            ? Duration.ZERO
            : Duration.between(startedAt, endedAt == null ? now : endedAt));
  }

  public UUID id() {
    return gameId;
  }

  public RoundConfiguration configuration() {
    return configuration;
  }

  private GameResult result() {
    Snapshot snapshot = snapshot();
    int maximumRound = snapshot.round().map(RoundState.Snapshot::number).orElse(0);
    return new GameResult(
        gameId, mapId, endReason, maximumRound, snapshot.duration(), snapshot.players());
  }

  private int continuingPlayers() {
    return (int)
        players.values().stream()
            .map(GamePlayer::snapshot)
            .filter(player -> player.state() != GamePlayerState.LEFT)
            .count();
  }

  private void requireState(GameState expected) {
    if (state != expected) {
      throw new IllegalStateException("Expected " + expected + " but was " + state);
    }
  }

  private void transition(GameState target) {
    if (!state.canTransitionTo(target)) {
      throw new IllegalStateException("Invalid game transition " + state + " -> " + target);
    }
    state = target;
  }

  private void emit(GameEvent.Type type) {
    events.publish(
        new GameEvent(
            type,
            gameId,
            round == null ? 0 : round.snapshot(clock.instant()).number(),
            clock.instant()));
  }

  public record Snapshot(
      UUID gameId,
      String mapId,
      GameState state,
      Optional<RoundState.Snapshot> round,
      Map<UUID, GamePlayer.Snapshot> players,
      Optional<GameEndReason> endReason,
      Duration duration) {
    public Snapshot {
      players = Map.copyOf(players);
    }
  }
}
