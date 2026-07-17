package fr.heneria.bedwars.plugin.game;

import fr.heneria.bedwars.core.arena.ArenaTeamDefinition;
import fr.heneria.bedwars.core.game.GameInstance;
import fr.heneria.bedwars.core.game.RuntimeBlockPosition;
import fr.heneria.bedwars.core.logging.ProjectLogger;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;

/** Resolves copied Bukkit bed blocks and builds the core's two-block runtime index. */
public final class BukkitGameBedRegistry {
  private final ProjectLogger logger;

  public BukkitGameBedRegistry(ProjectLogger logger) {
    this.logger = Objects.requireNonNull(logger, "logger");
  }

  public boolean initialize(GameInstance game) {
    if (!Bukkit.isPrimaryThread())
      throw new IllegalStateException("Runtime beds must be initialized on the server thread");
    World world = game.world().map(handle -> Bukkit.getWorld(handle.worldName())).orElse(null);
    if (world == null) return false;
    boolean complete = true;
    for (ArenaTeamDefinition team : game.arena().definition().teams()) {
      if (game.bed(team.id().value()).isPresent()) continue;
      if (team.bedLocation().isEmpty()) {
        complete = false;
        continue;
      }
      var location = team.bedLocation().orElseThrow();
      Block selected =
          world.getBlockAt(
              (int) Math.floor(location.position().x()),
              (int) Math.floor(location.position().y()),
              (int) Math.floor(location.position().z()));
      if (!(selected.getBlockData() instanceof Bed selectedData)) {
        complete = false;
        logger.warning(
            "[Games] Missing runtime bed for team "
                + team.id().value()
                + " in game "
                + game.id().shortId());
        continue;
      }
      Block foot =
          selectedData.getPart() == Bed.Part.FOOT
              ? selected
              : selected.getRelative(selectedData.getFacing().getOppositeFace());
      if (!(foot.getBlockData() instanceof Bed footData) || footData.getPart() != Bed.Part.FOOT) {
        complete = false;
        continue;
      }
      Block head = foot.getRelative(footData.getFacing());
      if (!(head.getBlockData() instanceof Bed headData)
          || headData.getPart() != Bed.Part.HEAD
          || headData.getFacing() != footData.getFacing()) {
        complete = false;
        continue;
      }
      game.registerBed(team.id().value(), position(foot), position(head));
    }
    return complete && game.indexedBedBlocks() == game.arena().definition().teams().size() * 2;
  }

  private static RuntimeBlockPosition position(Block block) {
    return new RuntimeBlockPosition(block.getX(), block.getY(), block.getZ());
  }
}
