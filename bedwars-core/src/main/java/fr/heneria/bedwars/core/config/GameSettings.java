package fr.heneria.bedwars.core.config;

import java.util.List;
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
    boolean scoreboardHideNumbers,
    String scoreboardTitle,
    List<String> scoreboardWaitingLines,
    List<String> scoreboardStartingLines,
    List<String> scoreboardPlayingLines,
    String serverName,
    String serverAddress,
    int leaveItemSlot,
    int infoItemSlot,
    long itemInteractionCooldownMillis,
    boolean forcedStartEnabled,
    int endingDurationSeconds) {
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
    scoreboardTitle = text(scoreboardTitle, "scoreboardTitle");
    scoreboardWaitingLines = lines(scoreboardWaitingLines, "scoreboardWaitingLines");
    scoreboardStartingLines = lines(scoreboardStartingLines, "scoreboardStartingLines");
    scoreboardPlayingLines = lines(scoreboardPlayingLines, "scoreboardPlayingLines");
    serverName = text(serverName, "serverName");
    serverAddress = text(serverAddress, "serverAddress");
    if (scoreboardRefreshTicks < 1)
      throw new IllegalArgumentException("scoreboardRefreshTicks must be positive");
    if (leaveItemSlot < 0 || leaveItemSlot > 8 || infoItemSlot < 0 || infoItemSlot > 8)
      throw new IllegalArgumentException("Waiting item slots must be inside the hotbar");
    if (leaveItemSlot == infoItemSlot)
      throw new IllegalArgumentException("Waiting item slots must be distinct");
    if (itemInteractionCooldownMillis < 0 || itemInteractionCooldownMillis > 60_000)
      throw new IllegalArgumentException("Waiting item cooldown is out of range");
    if (endingDurationSeconds < 1 || endingDurationSeconds > 300)
      throw new IllegalArgumentException("Ending duration must be between 1 and 300 seconds");
  }

  private static String text(String value, String field) {
    String result = Objects.requireNonNull(value, field).trim();
    if (result.isEmpty()) throw new IllegalArgumentException(field + " is blank");
    return result;
  }

  private static List<String> lines(List<String> value, String field) {
    List<String> result = List.copyOf(Objects.requireNonNull(value, field));
    if (result.isEmpty() || result.size() > 15)
      throw new IllegalArgumentException(field + " must contain between 1 and 15 lines");
    return result;
  }
}
