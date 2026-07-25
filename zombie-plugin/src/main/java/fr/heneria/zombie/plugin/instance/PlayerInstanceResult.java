package fr.heneria.zombie.plugin.instance;

/** Result exposed to command and listener adapters. */
public enum PlayerInstanceResult {
  SUCCESS,
  INSTANCE_NOT_FOUND,
  TEMPLATE_INVALID,
  INSTANCE_UNAVAILABLE,
  ACCESS_DENIED,
  INSTANCE_FULL,
  ALREADY_IN_OTHER_INSTANCE,
  PLAYER_OFFLINE,
  TELEPORT_FAILED
}
