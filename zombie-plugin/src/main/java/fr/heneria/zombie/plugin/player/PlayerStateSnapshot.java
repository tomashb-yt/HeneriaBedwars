package fr.heneria.zombie.plugin.player;

import java.util.List;
import java.util.Objects;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

/** Immutable Paper-bound capture of all player fields managed by Ticket 002. */
public record PlayerStateSnapshot(
    ItemStack[] storage,
    ItemStack[] armor,
    ItemStack offHand,
    int heldSlot,
    float experience,
    int level,
    int totalExperience,
    double health,
    int food,
    float saturation,
    float exhaustion,
    List<PotionEffect> effects,
    GameMode gameMode,
    Location location,
    float walkSpeed,
    float flySpeed,
    boolean allowFlight,
    boolean flying) {

  /** Defensively copies mutable platform objects. */
  public PlayerStateSnapshot {
    storage = cloneItems(storage);
    armor = cloneItems(armor);
    offHand = offHand == null ? null : offHand.clone();
    effects = List.copyOf(effects);
    Objects.requireNonNull(gameMode, "gameMode");
    location = Objects.requireNonNull(location, "location").clone();
  }

  /**
   * Captures a player on the Paper server thread.
   *
   * @param player player
   * @return snapshot
   */
  public static PlayerStateSnapshot capture(Player player) {
    return new PlayerStateSnapshot(
        player.getInventory().getStorageContents(),
        player.getInventory().getArmorContents(),
        player.getInventory().getItemInOffHand(),
        player.getInventory().getHeldItemSlot(),
        player.getExp(),
        player.getLevel(),
        player.getTotalExperience(),
        player.getHealth(),
        player.getFoodLevel(),
        player.getSaturation(),
        player.getExhaustion(),
        List.copyOf(player.getActivePotionEffects()),
        player.getGameMode(),
        player.getLocation(),
        player.getWalkSpeed(),
        player.getFlySpeed(),
        player.getAllowFlight(),
        player.isFlying());
  }

  /**
   * Restores the snapshot on the Paper server thread.
   *
   * @param player target player
   */
  public void restore(Player player) {
    player.getInventory().setStorageContents(cloneItems(storage));
    player.getInventory().setArmorContents(cloneItems(armor));
    player.getInventory().setItemInOffHand(offHand == null ? null : offHand.clone());
    player.getInventory().setHeldItemSlot(heldSlot);
    player.setExp(experience);
    player.setLevel(level);
    player.setTotalExperience(totalExperience);
    player.setHealth(
        Math.min(
            health,
            Objects.requireNonNull(
                    player.getAttribute(PaperAttributeResolver.maxHealth()), "max health")
                .getValue()));
    player.setFoodLevel(food);
    player.setSaturation(saturation);
    player.setExhaustion(exhaustion);
    player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
    effects.forEach(player::addPotionEffect);
    player.setGameMode(gameMode);
    player.setWalkSpeed(walkSpeed);
    player.setFlySpeed(flySpeed);
    player.setAllowFlight(allowFlight);
    player.setFlying(allowFlight && flying);
    player.teleport(location);
  }

  private static ItemStack[] cloneItems(ItemStack[] items) {
    ItemStack[] result = new ItemStack[items.length];
    for (int index = 0; index < items.length; index++) {
      result[index] = items[index] == null ? null : items[index].clone();
    }
    return result;
  }
}

