package fr.heneria.bedwars.core.game;

import fr.heneria.bedwars.api.game.GameSnapshot;
import fr.heneria.bedwars.api.game.GameState;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** One isolated live match scaffold. It intentionally contains no BedWars mechanics yet. */
public final class GameInstance {
  private static final Map<GameState, Set<GameState>> TRANSITIONS = transitions();

  private final GameId id;
  private final RuntimeArena arena;
  private final Instant createdAt;
  private final Map<UUID, RuntimePlayer> players = new LinkedHashMap<>();
  private final Map<String, RuntimeTeam> teams = new LinkedHashMap<>();
  private final Map<String, Long> timers = new LinkedHashMap<>();
  private final Map<String, Long> statistics = new LinkedHashMap<>();
  private GameState state = GameState.CREATING;
  private RuntimeWorldHandle world;
  private Instant updatedAt;

  public GameInstance(RuntimeArena arena, Instant now) {
    this.arena = Objects.requireNonNull(arena, "arena");
    this.id = arena.gameId();
    this.createdAt = Objects.requireNonNull(now, "now");
    this.updatedAt = now;
    arena.definition().teams().stream()
        .sorted(Comparator.comparingInt(team -> team.order()))
        .forEach(team -> teams.put(team.id().value(), new RuntimeTeam(team)));
  }

  public GameId id() {
    return id;
  }

  public RuntimeArena arena() {
    return arena;
  }

  public synchronized GameState state() {
    return state;
  }

  public synchronized Optional<RuntimeWorldHandle> world() {
    return Optional.ofNullable(world);
  }

  synchronized void attachWorld(RuntimeWorldHandle value, Instant now) {
    if (world != null) throw new IllegalStateException("Runtime world already attached");
    world = Objects.requireNonNull(value, "value");
    updatedAt = now;
  }

  public synchronized void transition(GameState target, Instant now) {
    if (!TRANSITIONS.getOrDefault(state, Set.of()).contains(target))
      throw new GameTransitionException(state, target);
    state = target;
    updatedAt = Objects.requireNonNull(now, "now");
  }

  synchronized RuntimePlayer addPlayer(UUID playerId, Instant now) {
    if (state != GameState.WAITING && state != GameState.STARTING)
      throw new GameTransitionException(state, state);
    RuntimePlayer player = new RuntimePlayer(playerId, now);
    RuntimeTeam selectedTeam =
        teams.values().stream()
            .filter(candidate -> !candidate.full())
            .min(Comparator.comparingInt(RuntimeTeam::size).thenComparing(RuntimeTeam::id))
            .orElseThrow(() -> new IllegalStateException("No team capacity remains"));
    player.assignTeam(selectedTeam.id());
    selectedTeam.add(playerId);
    players.put(playerId, player);
    updatedAt = now;
    return player;
  }

  synchronized Optional<RuntimePlayer> removePlayer(UUID playerId, Instant now) {
    RuntimePlayer removed = players.remove(playerId);
    if (removed == null) return Optional.empty();
    removed.teamId().map(teams::get).ifPresent(team -> team.remove(playerId));
    updatedAt = now;
    return Optional.of(removed);
  }

  public synchronized Optional<RuntimePlayer> player(UUID playerId) {
    return Optional.ofNullable(players.get(playerId));
  }

  public synchronized Set<UUID> playerIds() {
    return Set.copyOf(players.keySet());
  }

  public synchronized Optional<RuntimeTeam> team(String id) {
    return Optional.ofNullable(teams.get(id));
  }

  /** Resolves the selected team's configured spawn inside this instance's cloned world. */
  public synchronized Optional<RuntimeLocation> startLocation(UUID playerId) {
    RuntimePlayer player = players.get(playerId);
    if (player == null) return Optional.empty();
    return player.teamId().map(teams::get).flatMap(RuntimeTeam::spawn);
  }

  /** Reassigns a waiting player only when the destination has capacity. */
  synchronized boolean selectTeam(UUID playerId, String teamId, Instant now) {
    if (state != GameState.WAITING) return false;
    RuntimePlayer player = players.get(playerId);
    RuntimeTeam destination = teams.get(teamId);
    if (player == null || destination == null || destination.full()) return false;
    player.teamId().map(teams::get).ifPresent(team -> team.remove(playerId));
    destination.add(playerId);
    player.assignTeam(destination.id());
    updatedAt = now;
    return true;
  }

  public synchronized void timer(String key, long ticksRemaining, Instant now) {
    if (ticksRemaining < 0) throw new IllegalArgumentException("Timer cannot be negative");
    timers.put(metricKey(key), ticksRemaining);
    updatedAt = Objects.requireNonNull(now, "now");
  }

  public synchronized void incrementStatistic(String key, long amount, Instant now) {
    if (amount < 0) throw new IllegalArgumentException("Statistic increment cannot be negative");
    statistics.merge(metricKey(key), amount, Long::sum);
    updatedAt = Objects.requireNonNull(now, "now");
  }

  public synchronized GameSnapshot snapshot(Instant now) {
    return new GameSnapshot(
        id.value(),
        state,
        arena.definition().id().value(),
        arena.template().id().value(),
        world().map(RuntimeWorldHandle::worldName),
        players.values().stream().map(player -> player.snapshot(now)).toList(),
        teams.values().stream().map(RuntimeTeam::snapshot).toList(),
        timers,
        statistics,
        createdAt,
        updatedAt);
  }

  private static Map<GameState, Set<GameState>> transitions() {
    Map<GameState, Set<GameState>> values = new EnumMap<>(GameState.class);
    values.put(GameState.CREATING, Set.of(GameState.WAITING, GameState.RESETTING));
    values.put(
        GameState.WAITING, Set.of(GameState.STARTING, GameState.ENDING, GameState.RESETTING));
    values.put(
        GameState.STARTING,
        Set.of(GameState.WAITING, GameState.PLAYING, GameState.ENDING, GameState.RESETTING));
    values.put(GameState.PLAYING, Set.of(GameState.ENDING));
    values.put(GameState.ENDING, Set.of(GameState.RESETTING));
    values.put(GameState.RESETTING, Set.of(GameState.DESTROYED));
    values.put(GameState.DESTROYED, Set.of());
    return Map.copyOf(values);
  }

  private static String metricKey(String value) {
    String clean = Objects.requireNonNull(value, "value").trim();
    if (clean.isEmpty() || clean.length() > 64)
      throw new IllegalArgumentException("Runtime metric key is invalid");
    return clean;
  }
}
