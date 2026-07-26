package fr.heneria.zombie.core.weapon;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Owns runtime weapon instances and O(1) indexes without platform inventory references. */
public final class WeaponService {
  private final Map<UUID, WeaponInstance> byId = new ConcurrentHashMap<>();
  private final Map<UUID, Set<UUID>> byPlayer = new ConcurrentHashMap<>();
  private final Map<UUID, Set<UUID>> byGame = new ConcurrentHashMap<>();

  public WeaponInstance create(
      UUID gameId, UUID playerId, WeaponDefinition definition, long currentTick) {
    WeaponInstance instance =
        new WeaponInstance(UUID.randomUUID(), gameId, playerId, definition, currentTick);
    byId.put(instance.id(), instance);
    byPlayer.computeIfAbsent(playerId, ignored -> ConcurrentHashMap.newKeySet()).add(instance.id());
    byGame.computeIfAbsent(gameId, ignored -> ConcurrentHashMap.newKeySet()).add(instance.id());
    return instance;
  }

  public Optional<WeaponInstance> find(UUID id) {
    return Optional.ofNullable(byId.get(id));
  }

  public Collection<WeaponInstance> player(UUID playerId) {
    return resolve(byPlayer.getOrDefault(playerId, Set.of()));
  }

  public Collection<WeaponInstance> game(UUID gameId) {
    return resolve(byGame.getOrDefault(gameId, Set.of()));
  }

  public Collection<WeaponInstance> all() {
    return List.copyOf(byId.values());
  }

  public Optional<WeaponInstance> remove(UUID id) {
    WeaponInstance removed = byId.remove(id);
    if (removed == null) {
      return Optional.empty();
    }
    removeIndex(byPlayer, removed.ownerId(), id);
    removeIndex(byGame, removed.gameId(), id);
    return Optional.of(removed);
  }

  public Collection<WeaponInstance> removeGame(UUID gameId) {
    java.util.List<WeaponInstance> removed =
        byGame.getOrDefault(gameId, Set.of()).stream()
            .map(this::remove)
            .flatMap(Optional::stream)
            .toList();
    byGame.remove(gameId);
    return removed;
  }

  public int size() {
    return byId.size();
  }

  private Collection<WeaponInstance> resolve(Set<UUID> ids) {
    return ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
  }

  private static void removeIndex(Map<UUID, Set<UUID>> index, UUID owner, UUID id) {
    index.computeIfPresent(
        owner,
        (ignored, ids) -> {
          ids.remove(id);
          return ids.isEmpty() ? null : ids;
        });
  }
}
