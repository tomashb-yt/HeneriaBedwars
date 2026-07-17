package fr.heneria.bedwars.core.config;

import java.util.List;

/** Validated v4 map administration layout. */
public record MapEditorSettings(
    int listRows,
    int listGuideSlot,
    List<Integer> listContentSlots,
    int listPreviousSlot,
    int listFilterSlot,
    int listCreateSlot,
    int listRefreshSlot,
    int listSortSlot,
    int listNextSlot,
    int listDashboardSlot,
    int listCloseSlot,
    int editorRows,
    int summarySlot,
    int enterSlot,
    int saveSlot,
    int spawnSlot,
    int worldStateSlot,
    int displayNameSlot,
    int typeSlot,
    int settingsSlot,
    int workflowSlot,
    int associationsSlot,
    int importSlot,
    int validationSlot,
    int backupSlot,
    int duplicateSlot,
    int deleteSlot,
    int listSlot,
    int refreshSlot,
    int closeSlot,
    int settingsRows,
    int associationsRows,
    int validationRows) {
  public MapEditorSettings {
    listContentSlots = List.copyOf(listContentSlots);
  }
}
