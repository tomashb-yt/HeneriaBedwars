package fr.heneria.zombie.core.instance;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Concurrent source of truth for all live instances. */
public final class GameInstanceRegistry {

  private final ConcurrentMap<UUID, GameInstance> instances = new ConcurrentHashMap<>();

  /**
   * Adds a newly allocated instance.
   *
   * @param instance instance
   * @throws IllegalStateException on identifier collision
   */
  public void register(GameInstance instance) {
    Objects.requireNonNull(instance, "instance");
    if (instances.putIfAbsent(instance.id(), instance) != null) {
      throw new IllegalStateException("Duplicate instance id " + instance.id());
    }
  }

  /**
   * Finds an instance.
   *
   * @param id identifier
   * @return optional instance
   */
  public Optional<GameInstance> find(UUID id) {
    return Optional.ofNullable(instances.get(Objects.requireNonNull(id, "id")));
  }

  /**
   * Removes an instance only when the expected aggregate is still registered.
   *
   * @param instance expected aggregate
   * @return whether it was removed
   */
  public boolean remove(GameInstance instance) {
    return instances.remove(instance.id(), instance);
  }

  /**
   * Returns stable snapshots ordered by creation time.
   *
   * @return immutable snapshots
   */
  public Collection<GameInstanceSnapshot> snapshots() {
    return instances.values().stream()
        .map(GameInstance::snapshot)
        .sorted(Comparator.comparing(GameInstanceSnapshot::createdAt))
        .toList();
  }

  /**
   * Returns the currently registered aggregate count.
   *
   * @return live count
   */
  public int size() {
    return instances.size();
  }

  /**
   * Finds instances containing a player.
   *
   * @param playerId player identifier
   * @return matching aggregates
   */
  public List<GameInstance> containing(UUID playerId) {
    return instances.values().stream().filter(instance -> instance.contains(playerId)).toList();
  }
}
