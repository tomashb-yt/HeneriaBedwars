package fr.heneria.zombie.core.enemy;

/** Controlled runtime lifecycle of an enemy. */
public enum ZombieState {
  SPAWNING,
  IDLE,
  SEARCHING_TARGET,
  MOVING,
  ATTACKING,
  BREAKING_BARRICADE,
  USING_ABILITY,
  STUNNED,
  KNOCKED_BACK,
  DYING,
  DEAD,
  DESPAWNED,
  INVALID;

  public boolean terminal() {
    return this == DEAD || this == DESPAWNED || this == INVALID;
  }
}
