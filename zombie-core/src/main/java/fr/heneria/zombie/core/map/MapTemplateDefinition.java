package fr.heneria.zombie.core.map;

import java.util.Objects;

/**
 * Minimal validated metadata required to create a Ticket 002 instance.
 *
 * @param mapId stable identifier and template folder name
 * @param maximumPlayers positive capacity
 * @param spawn spawn coordinates inside the copied world
 */
public record MapTemplateDefinition(String mapId, int maximumPlayers, MapSpawn spawn) {

  /** Validates metadata. */
  public MapTemplateDefinition {
    Objects.requireNonNull(mapId, "mapId");
    Objects.requireNonNull(spawn, "spawn");
    if (!mapId.matches("[a-z0-9][a-z0-9_-]{0,63}")) {
      throw new IllegalArgumentException("Invalid map id: " + mapId);
    }
    if (maximumPlayers <= 0) {
      throw new IllegalArgumentException("maximumPlayers must be positive");
    }
  }

  /**
   * Spawn inside a template world.
   *
   * @param x x coordinate
   * @param y y coordinate
   * @param z z coordinate
   * @param yaw yaw
   * @param pitch pitch
   */
  public record MapSpawn(double x, double y, double z, float yaw, float pitch) {}
}
