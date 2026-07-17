package fr.heneria.bedwars.core.game;

import fr.heneria.bedwars.core.arena.ArenaLocation;

/** Maps persistent template coordinates onto the temporary world owned by one game instance. */
public final class GameLocationMapper {
  /** Preserves coordinates and rotation while deliberately discarding the administrative world. */
  public RuntimeLocation waiting(GameInstance instance) {
    return instance
        .arena()
        .definition()
        .waitingLocation()
        .map(GameLocationMapper::coordinates)
        .orElseGet(
            () -> {
              var spawn = instance.arena().template().spawn();
              return new RuntimeLocation(
                  spawn.x(), spawn.y(), spawn.z(), spawn.yaw(), spawn.pitch());
            });
  }

  /** Uses the configured spectator point, falling back to the waiting point. */
  public RuntimeLocation spectator(GameInstance instance) {
    return instance
        .arena()
        .definition()
        .spectatorLocation()
        .map(GameLocationMapper::coordinates)
        .orElseGet(() -> waiting(instance));
  }

  private static RuntimeLocation coordinates(ArenaLocation location) {
    return new RuntimeLocation(
        location.position().x(),
        location.position().y(),
        location.position().z(),
        location.yaw(),
        location.pitch());
  }
}
