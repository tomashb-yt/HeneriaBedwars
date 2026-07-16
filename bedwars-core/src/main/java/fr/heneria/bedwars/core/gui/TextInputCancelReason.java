package fr.heneria.bedwars.core.gui;

/** Reason supplied to the cancellation callback without retaining any platform object. */
public enum TextInputCancelReason {
  PLAYER,
  TIMEOUT,
  DISCONNECT,
  REPLACED,
  PLUGIN_STOP
}
