package fr.heneria.bedwars.core.config;

import java.util.List;

/** Validated slots used by the arena administration menus. */
public record ArenaEditorSettings(
    int listRows,
    List<Integer> contentSlots,
    int createSlot,
    int filterSlot,
    int sortSlot,
    int previousPageSlot,
    int nextPageSlot,
    int editorRows,
    int informationSlot,
    int displayNameSlot,
    int worldSlot,
    int waitingSlot,
    int spectatorSlot,
    int playersSlot,
    int teamsSlot,
    int boundarySlot,
    int validationSlot,
    int enableSlot,
    int deleteSlot,
    int editorBackSlot,
    int editorRefreshSlot,
    int editorCloseSlot) {
  public ArenaEditorSettings {
    contentSlots = List.copyOf(contentSlots);
  }
}
