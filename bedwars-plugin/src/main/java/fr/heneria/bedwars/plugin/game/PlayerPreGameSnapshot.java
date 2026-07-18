package fr.heneria.bedwars.plugin.game;

import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Defensive in-memory copy of the Bukkit state replaced by the waiting lobby.
 *
 * <p>No snapshot is persisted. A process crash can therefore prevent restoration and is explicitly
 * outside Ticket 010.
 */
public record PlayerPreGameSnapshot(
    String worldName,
    double x,
    double y,
    double z,
    float yaw,
    float pitch,
    GameMode gameMode,
    ItemStack[] storage,
    ItemStack[] armor,
    ItemStack offHand,
    int level,
    float experience,
    double health,
    int food,
    float saturation,
    int fireTicks,
    float fallDistance,
    boolean allowFlight,
    boolean flying,
    float walkSpeed,
    float flySpeed,
    double attackSpeed,
    int maximumNoDamageTicks,
    List<PotionEffect> effects,
    Scoreboard scoreboard) {
  public PlayerPreGameSnapshot {
    storage = cloneItems(storage);
    armor = cloneItems(armor);
    offHand = offHand == null ? null : offHand.clone();
    effects = List.copyOf(effects);
  }

  @Override
  public ItemStack[] storage() {
    return cloneItems(storage);
  }

  @Override
  public ItemStack[] armor() {
    return cloneItems(armor);
  }

  @Override
  public ItemStack offHand() {
    return offHand == null ? null : offHand.clone();
  }

  private static ItemStack[] cloneItems(ItemStack[] values) {
    ItemStack[] copy = new ItemStack[values.length];
    for (int index = 0; index < values.length; index++)
      copy[index] = values[index] == null ? null : values[index].clone();
    return copy;
  }
}
