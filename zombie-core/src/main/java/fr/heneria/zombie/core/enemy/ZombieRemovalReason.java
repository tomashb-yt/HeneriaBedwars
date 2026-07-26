package fr.heneria.zombie.core.enemy;

/** Explicit cause controlling rewards and round accounting. */
public enum ZombieRemovalReason {
  KILLED_BY_PLAYER(true, true),
  KILLED_BY_TRAP(true, true),
  KILLED_BY_ENVIRONMENT(true, true),
  DESPAWNED_STUCK(false, true),
  DESPAWNED_ADMIN(false, true),
  GAME_ENDED(false, false),
  ROUND_CANCELLED(false, false),
  WORLD_UNLOADED(false, true),
  INVALID_ENTITY(false, true),
  REPLACED(false, true),
  ERROR(false, true);

  private final boolean rewards;
  private final boolean completesRoundSlot;

  ZombieRemovalReason(boolean rewards, boolean completesRoundSlot) {
    this.rewards = rewards;
    this.completesRoundSlot = completesRoundSlot;
  }

  public boolean rewards() {
    return rewards;
  }

  public boolean completesRoundSlot() {
    return completesRoundSlot;
  }
}
