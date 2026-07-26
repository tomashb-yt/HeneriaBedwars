package fr.heneria.zombie.core.enemy;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** O(1) indexes by internal id, Bukkit entity id and game id. */
public final class ZombieTracker {
  private final Map<UUID, ZombieInstance> byInternalId = new ConcurrentHashMap<>();
  private final Map<UUID, UUID> internalByEntityId = new ConcurrentHashMap<>();
  private final Map<UUID, Set<UUID>> internalByGameId = new ConcurrentHashMap<>();

  public void register(ZombieInstance zombie) {
    if (byInternalId.putIfAbsent(zombie.id(), zombie) != null
        || internalByEntityId.putIfAbsent(zombie.entityId(), zombie.id()) != null) {
      throw new IllegalStateException("Zombie is already registered");
    }
    internalByGameId
        .computeIfAbsent(zombie.gameId(), ignored -> ConcurrentHashMap.newKeySet())
        .add(zombie.id());
  }

  public Optional<ZombieInstance> findByEntity(UUID entityId) {
    return Optional.ofNullable(internalByEntityId.get(entityId)).map(byInternalId::get);
  }

  public Optional<ZombieInstance> find(UUID internalId) {
    return Optional.ofNullable(byInternalId.get(internalId));
  }

  public Collection<ZombieInstance> game(UUID gameId) {
    return internalByGameId.getOrDefault(gameId, Set.of()).stream()
        .map(byInternalId::get)
        .filter(java.util.Objects::nonNull)
        .toList();
  }

  public Collection<ZombieInstance> all() {
    return java.util.List.copyOf(byInternalId.values());
  }

  public Optional<ZombieInstance> unregister(UUID internalId) {
    ZombieInstance removed = byInternalId.remove(internalId);
    if (removed == null) {
      return Optional.empty();
    }
    internalByEntityId.remove(removed.entityId(), internalId);
    internalByGameId.computeIfPresent(
        removed.gameId(),
        (ignored, ids) -> {
          ids.remove(internalId);
          return ids.isEmpty() ? null : ids;
        });
    return Optional.of(removed);
  }

  public int size() {
    return byInternalId.size();
  }
}
