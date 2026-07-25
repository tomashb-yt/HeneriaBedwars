package fr.heneria.zombie.core.instance;

/** Result of an aggregate membership request. */
public enum InstanceJoinResult {
  JOINED,
  ALREADY_JOINED,
  ACCESS_DENIED,
  FULL,
  NOT_JOINABLE
}
