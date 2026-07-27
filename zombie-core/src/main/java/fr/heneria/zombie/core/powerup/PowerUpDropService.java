package fr.heneria.zombie.core.powerup;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

/** Chance, caps, cooldown, collection idempotence and expiry for bonus drops. */
public final class PowerUpDropService {
  private final PowerUpRegistry registry;
  private final Options options;
  private final Clock clock;
  private final RandomGenerator random;
  private final Consumer<PowerUpEvent> events;
  private final Map<UUID, MutableDrop> drops = new LinkedHashMap<>();
  private final Map<GameRound, Integer> roundCounts = new LinkedHashMap<>();
  private final Map<UUID, Instant> lastDrop = new LinkedHashMap<>();

  public PowerUpDropService(
      PowerUpRegistry registry,
      Options options,
      Clock clock,
      RandomGenerator random,
      Consumer<PowerUpEvent> events) {
    this.registry = registry;
    this.options = options;
    this.clock = clock;
    this.random = random;
    this.events = events;
  }

  public synchronized Optional<PowerUpDrop> roll(UUID gameId, int round) {
    Instant now = clock.instant();
    GameRound key = new GameRound(gameId, round);
    if (!options.enabled()
        || roundCounts.getOrDefault(key, 0) >= options.maximumPerRound()
        || lastDrop.getOrDefault(gameId, Instant.MIN).plus(options.minimumInterval()).isAfter(now)
        || random.nextDouble() >= options.chance()) {
      return Optional.empty();
    }
    List<PowerUpDefinition> definitions = List.copyOf(registry.all());
    int total = definitions.stream().mapToInt(PowerUpDefinition::dropWeight).sum();
    int selected = random.nextInt(total);
    PowerUpDefinition chosen = definitions.getFirst();
    for (PowerUpDefinition definition : definitions) {
      selected -= definition.dropWeight();
      if (selected < 0) {
        chosen = definition;
        break;
      }
    }
    MutableDrop mutable =
        new MutableDrop(
            UUID.randomUUID(), gameId, round, chosen.type(), now, now.plus(options.lifetime()));
    drops.put(mutable.id, mutable);
    roundCounts.merge(key, 1, Math::addExact);
    lastDrop.put(gameId, now);
    PowerUpDrop drop = mutable.snapshot();
    events.accept(new PowerUpEvent(PowerUpEvent.Type.DROP_CREATED, gameId, now, drop));
    return Optional.of(drop);
  }

  public synchronized CollectResult collect(UUID dropId, UUID gameId, UUID playerId) {
    MutableDrop drop = drops.get(dropId);
    if (drop == null || !drop.gameId.equals(gameId)) {
      return new CollectResult(false, Optional.empty(), "Drop not found");
    }
    if (drop.state != PowerUpDrop.State.AVAILABLE) {
      return new CollectResult(false, Optional.of(drop.snapshot()), "Drop already resolved");
    }
    if (!drop.expiresAt.isAfter(clock.instant())) {
      expire(drop);
      return new CollectResult(false, Optional.of(drop.snapshot()), "Drop expired");
    }
    drop.state = PowerUpDrop.State.COLLECTED;
    drop.collectorId = playerId;
    PowerUpDrop snapshot = drop.snapshot();
    events.accept(
        new PowerUpEvent(PowerUpEvent.Type.DROP_COLLECTED, gameId, clock.instant(), snapshot));
    return new CollectResult(true, Optional.of(snapshot), "");
  }

  public synchronized List<PowerUpDrop> tick() {
    ArrayList<PowerUpDrop> expired = new ArrayList<>();
    for (MutableDrop drop : drops.values()) {
      if (drop.state == PowerUpDrop.State.AVAILABLE && !drop.expiresAt.isAfter(clock.instant())) {
        expire(drop);
        expired.add(drop.snapshot());
      }
    }
    return List.copyOf(expired);
  }

  public synchronized List<PowerUpDrop> active(UUID gameId) {
    return drops.values().stream()
        .filter(value -> value.gameId.equals(gameId))
        .filter(value -> value.state == PowerUpDrop.State.AVAILABLE)
        .map(MutableDrop::snapshot)
        .toList();
  }

  /** Returns all unresolved drops across games for one grouped adapter tick. */
  public synchronized List<PowerUpDrop> active() {
    return drops.values().stream()
        .filter(value -> value.state == PowerUpDrop.State.AVAILABLE)
        .map(MutableDrop::snapshot)
        .toList();
  }

  public synchronized void clear(UUID gameId) {
    drops.entrySet().removeIf(entry -> entry.getValue().gameId.equals(gameId));
    roundCounts.keySet().removeIf(key -> key.gameId.equals(gameId));
    lastDrop.remove(gameId);
  }

  private void expire(MutableDrop drop) {
    drop.state = PowerUpDrop.State.EXPIRED;
    events.accept(
        new PowerUpEvent(
            PowerUpEvent.Type.DROP_EXPIRED, drop.gameId, clock.instant(), drop.snapshot()));
  }

  public record Options(
      boolean enabled,
      double chance,
      int maximumPerRound,
      Duration minimumInterval,
      Duration lifetime) {
    public Options {
      if (!Double.isFinite(chance)
          || chance < 0
          || chance > 1
          || maximumPerRound < 0
          || minimumInterval.isNegative()
          || lifetime.isNegative()) {
        throw new IllegalArgumentException("Invalid drop options");
      }
    }
  }

  public record CollectResult(
      boolean collected, Optional<PowerUpDrop> drop, String failureReason) {}

  private record GameRound(UUID gameId, int round) {}

  private static final class MutableDrop {
    private final UUID id;
    private final UUID gameId;
    private final int round;
    private final PowerUpType type;
    private final Instant createdAt;
    private final Instant expiresAt;
    private PowerUpDrop.State state = PowerUpDrop.State.AVAILABLE;
    private UUID collectorId;

    private MutableDrop(
        UUID id, UUID gameId, int round, PowerUpType type, Instant createdAt, Instant expiresAt) {
      this.id = id;
      this.gameId = gameId;
      this.round = round;
      this.type = type;
      this.createdAt = createdAt;
      this.expiresAt = expiresAt;
    }

    private PowerUpDrop snapshot() {
      return new PowerUpDrop(
          id, gameId, round, type, createdAt, expiresAt, state, Optional.ofNullable(collectorId));
    }
  }
}
