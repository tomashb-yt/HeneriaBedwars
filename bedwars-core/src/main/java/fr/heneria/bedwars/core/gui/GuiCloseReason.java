package fr.heneria.bedwars.core.gui;

/** Reason why a GUI view ended. */
public enum GuiCloseReason {
  PLAYER,
  NAVIGATION,
  PLUGIN,
  DISCONNECT,
  KICK,
  ERROR,
  REPLACED
}
