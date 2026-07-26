package fr.heneria.zombie.core.game;

import fr.heneria.zombie.core.editor.MapPoint;
import java.util.Optional;
import java.util.UUID;

/** Platform port used by the round loop to create and remove temporary enemies. */
public interface ZombieSpawner {
  Optional<UUID> spawn(SpawnRequest request);

  void remove(UUID entityId);

  record SpawnRequest(
      UUID gameId, int round, String worldName, String spawnId, MapPoint point, double health) {}
}
