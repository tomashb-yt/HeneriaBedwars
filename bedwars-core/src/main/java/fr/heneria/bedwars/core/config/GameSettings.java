package fr.heneria.bedwars.core.config;

import java.util.Objects;
import java.util.Set;

/** Immutable waiting-lobby, countdown and display settings loaded from {@code game.yml}. */
public record GameSettings(
    String waitingGameMode,
    boolean protectPlayers,
    boolean disableHunger,
    boolean disableItemDrop,
    boolean disableItemPickup,
    double voidRescueY,
    boolean destroyEmptyInstance,
    int emptyDestroyDelaySeconds,
    boolean countdownEnabled,
    int normalCountdownSeconds,
    int fullGameCountdownSeconds,
    boolean cancelBelowMinimum,
    boolean allowJoinDuringCountdown,
    Set<Integer> chatAnnouncementSeconds,
    Set<Integer> titleAnnouncementSeconds,
    boolean bossBarEnabled,
    String bossBarColor,
    String bossBarStyle,
    boolean scoreboardEnabled,
    int scoreboardRefreshTicks,
    String scoreboardFooter,
    int leaveItemSlot,
    int infoItemSlot,
    boolean forcedStartEnabled) {
  public GameSettings {
    waitingGameMode = text(waitingGameMode, "waitingGameMode");
    if (!Double.isFinite(voidRescueY))
      throw new IllegalArgumentException("voidRescueY must be finite");
    if (emptyDestroyDelaySeconds < 0)
      throw new IllegalArgumentException("emptyDestroyDelaySeconds cannot be negative");
    if (normalCountdownSeconds < 1 || fullGameCountdownSeconds < 1)
      throw new IllegalArgumentException("Countdown durations must be positive");
    chatAnnouncementSeconds = Set.copyOf(chatAnnouncementSeconds);
    titleAnnouncementSeconds = Set.copyOf(titleAnnouncementSeconds);
    bossBarColor = text(bossBarColor, "bossBarColor");
    bossBarStyle = text(bossBarStyle, "bossBarStyle");
    scoreboardFooter = Objects.requireNonNull(scoreboardFooter, "scoreboardFooter").trim();
    if (scoreboardRefreshTicks < 1)
      throw new IllegalArgumentException("scoreboardRefreshTicks must be positive");
    if (leaveItemSlot < 0 || leaveItemSlot > 8 || infoItemSlot < 0 || infoItemSlot > 8)
      throw new IllegalArgumentException("Waiting item slots must be inside the hotbar");
    if (leaveItemSlot == infoItemSlot)
      throw new IllegalArgumentException("Waiting item slots must be distinct");
  }

  private static String text(String value, String field) {
    String result = Objects.requireNonNull(value, field).trim();
    if (result.isEmpty()) throw new IllegalArgumentException(field + " is blank");
    return result;
  }
}
