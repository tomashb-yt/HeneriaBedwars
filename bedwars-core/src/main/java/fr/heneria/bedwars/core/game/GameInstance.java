package fr.heneria.bedwars.core.game;

import fr.heneria.bedwars.api.game.GameSnapshot;
import fr.heneria.bedwars.api.game.GameState;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** One isolated live match scaffold. It intentionally contains no BedWars mechanics yet. */
public final class GameInstance {
  private static final Map<GameState, Set<GameState>> TRANSITIONS = transitions();
  private static final List<String> TEAM_COLORS =
      List.of(
          "RED",
          "BLUE",
          "GREEN",
          "YELLOW",
          "AQUA",
          "WHITE",
          "PINK",
          "GRAY",
          "PURPLE",
          "ORANGE",
          "LIME",
          "CYAN",
          "MAGENTA",
          "LIGHT_BLUE",
          "BLACK",
          "BROWN");

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
    int count = Math.max(1, arena.definition().teamCount());
    for (int index = 0; index < count; index++) {
      String color = TEAM_COLORS.get(index % TEAM_COLORS.size());
      String teamId = "team-" + (index + 1);
      teams.put(teamId, new RuntimeTeam(teamId, "Team " + (index + 1), color));
    }
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
    RuntimeTeam team =
        teams.values().stream()
            .min(Comparator.comparingInt(RuntimeTeam::size).thenComparing(RuntimeTeam::id))
            .orElseThrow();
    player.assignTeam(team.id());
    team.add(playerId);
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
        GameState.STARTING, Set.of(GameState.PLAYING, GameState.ENDING, GameState.RESETTING));
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
