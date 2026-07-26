package fr.heneria.zombie.core.game;

import java.time.Clock;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Concurrent registry and factory for isolated game aggregates. */
public final class ZombieGameService {
  private final Map<UUID, ZombieGame> games = new ConcurrentHashMap<>();
  private final Clock clock;
  private final GameEventDispatcher events;

  public ZombieGameService(Clock clock, GameEventDispatcher events) {
    this.clock = java.util.Objects.requireNonNull(clock, "clock");
    this.events = java.util.Objects.requireNonNull(events, "events");
  }

  public ZombieGame create(UUID instanceId, String mapId, RoundConfiguration configuration) {
    ZombieGame candidate = new ZombieGame(instanceId, mapId, configuration, clock, events);
    ZombieGame existing = games.putIfAbsent(instanceId, candidate);
    return existing == null ? candidate : existing;
  }

  public Optional<ZombieGame> find(UUID gameId) {
    return Optional.ofNullable(games.get(gameId));
  }

  public Collection<ZombieGame.Snapshot> snapshots() {
    return games.values().stream().map(ZombieGame::snapshot).toList();
  }

  public boolean remove(UUID gameId) {
    return games.remove(gameId) != null;
  }

  public void clear() {
    games.clear();
  }
}
