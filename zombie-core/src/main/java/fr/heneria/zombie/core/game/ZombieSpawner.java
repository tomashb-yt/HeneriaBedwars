package fr.heneria.zombie.core.game;

import fr.heneria.zombie.core.editor.MapPoint;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Platform port used by the round loop to create and remove registered enemy instances. */
public interface ZombieSpawner {
  Optional<UUID> spawn(SpawnRequest request);

  void remove(UUID entityId);

  record SpawnRequest(
      UUID gameId,
      int round,
      String worldName,
      String spawnId,
      MapPoint point,
      Optional<String> zoneId,
      Set<String> allowedTypes) {
    public SpawnRequest {
      zoneId = java.util.Objects.requireNonNull(zoneId, "zoneId");
      allowedTypes = Set.copyOf(allowedTypes);
    }
  }
}
