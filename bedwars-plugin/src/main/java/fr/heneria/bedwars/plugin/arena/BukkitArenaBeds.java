package fr.heneria.bedwars.plugin.arena;

import fr.heneria.bedwars.core.arena.ArenaDefinition;
import fr.heneria.bedwars.core.arena.ArenaLocation;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;

/** Validates and normalizes an administrator's targeted Bukkit bed to its foot block. */
public final class BukkitArenaBeds {
  private static final int SELECTION_DISTANCE = 8;

  private BukkitArenaBeds() {}

  public static Selection select(Player player, ArenaDefinition arena) {
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(arena, "arena");
    String expectedWorld = arena.worldName().orElse(null);
    if (expectedWorld == null || !player.getWorld().getName().equals(expectedWorld))
      return Selection.failure(SelectionCode.WRONG_WORLD);

    Block selected = player.getTargetBlockExact(SELECTION_DISTANCE);
    if (selected == null || !(selected.getBlockData() instanceof Bed selectedBed))
      return Selection.failure(SelectionCode.NOT_A_BED);

    Block foot =
        selectedBed.getPart() == Bed.Part.FOOT
            ? selected
            : selected.getRelative(selectedBed.getFacing().getOppositeFace());
    if (!(foot.getBlockData() instanceof Bed footBed) || footBed.getPart() != Bed.Part.FOOT)
      return Selection.failure(SelectionCode.INCOMPLETE_BED);

    Block head = foot.getRelative(footBed.getFacing());
    if (!(head.getBlockData() instanceof Bed headBed)
        || headBed.getPart() != Bed.Part.HEAD
        || headBed.getFacing() != footBed.getFacing())
      return Selection.failure(SelectionCode.INCOMPLETE_BED);

    return new Selection(
        SelectionCode.SUCCESS, Optional.of(BukkitArenaLocations.from(foot.getLocation())));
  }

  public enum SelectionCode {
    SUCCESS,
    WRONG_WORLD,
    NOT_A_BED,
    INCOMPLETE_BED
  }

  public record Selection(SelectionCode code, Optional<ArenaLocation> location) {
    public Selection {
      code = Objects.requireNonNull(code, "code");
      location = Objects.requireNonNull(location, "location");
      if ((code == SelectionCode.SUCCESS) != location.isPresent())
        throw new IllegalArgumentException("A successful bed selection requires a location");
    }

    private static Selection failure(SelectionCode code) {
      return new Selection(code, Optional.empty());
    }

    public boolean successful() {
      return code == SelectionCode.SUCCESS;
    }
  }
}
