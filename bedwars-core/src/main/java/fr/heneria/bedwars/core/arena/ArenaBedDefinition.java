package fr.heneria.bedwars.core.arena;

import java.util.Objects;

/** Complete persistent identity of a two-block Minecraft bed. */
public record ArenaBedDefinition(ArenaBlockPosition foot, ArenaBlockPosition head, String facing) {
  public ArenaBedDefinition {
    foot = Objects.requireNonNull(foot, "foot");
    head = Objects.requireNonNull(head, "head");
    facing = Objects.requireNonNull(facing, "facing").trim().toUpperCase(java.util.Locale.ROOT);
    if (facing.isEmpty()) throw new IllegalArgumentException("Bed facing cannot be blank");
    if (!foot.world().equalsIgnoreCase(head.world()))
      throw new IllegalArgumentException("Bed halves must be in the same world");
    int distance =
        Math.abs(foot.x() - head.x())
            + Math.abs(foot.y() - head.y())
            + Math.abs(foot.z() - head.z());
    if (distance != 1) throw new IllegalArgumentException("Bed halves must be adjacent");
  }
}
