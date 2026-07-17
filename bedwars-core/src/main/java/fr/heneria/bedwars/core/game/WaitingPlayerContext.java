package fr.heneria.bedwars.core.game;

import java.util.Map;
import java.util.Objects;

/** Immutable instructions used by the platform adapter to prepare one waiting-lobby player. */
public record WaitingPlayerContext(
    String gameMode, int leaveSlot, int infoSlot, Map<String, Object> placeholders) {
  public WaitingPlayerContext {
    gameMode = Objects.requireNonNull(gameMode, "gameMode").trim();
    if (gameMode.isEmpty()) throw new IllegalArgumentException("gameMode is blank");
    if (leaveSlot < 0 || leaveSlot > 8 || infoSlot < 0 || infoSlot > 8 || leaveSlot == infoSlot)
      throw new IllegalArgumentException("Waiting hotbar slots are invalid");
    placeholders = Map.copyOf(placeholders);
  }
}
