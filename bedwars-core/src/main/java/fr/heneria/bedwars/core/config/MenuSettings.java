package fr.heneria.bedwars.core.config;

import java.util.Map;

/** Immutable settings consumed by the internal GUI framework. */
public record MenuSettings(
    int defaultSize,
    boolean fillEmptySlots,
    boolean closeOnCriticalError,
    boolean playClickSounds,
    int previousSlot,
    int nextSlot,
    int backSlot,
    int pageIndicatorSlot,
    boolean historyEnabled,
    int maximumHistorySize,
    long defaultClickCooldownMillis,
    boolean cancelPlayerInventoryClicks,
    boolean cancelDragEvents,
    boolean refreshEnabled,
    int minimumRefreshTicks,
    boolean soundsEnabled,
    Map<String, SoundSettings> sounds,
    ArenaEditorSettings arenaEditor,
    MapEditorSettings mapEditor,
    TextInputSettings textInput) {
  public MenuSettings {
    sounds = Map.copyOf(sounds);
  }
}
