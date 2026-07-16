package fr.heneria.bedwars.core.arena.editor;

/** Stable mapping from validation fields to the editor section capable of fixing them. */
public final class ArenaProblemRouter {
  private ArenaProblemRouter() {}

  public static ArenaEditorSection section(String field) {
    if (field == null) return ArenaEditorSection.VALIDATION;
    if (field.startsWith("world")) return ArenaEditorSection.WORLD;
    if (field.startsWith("map")) return ArenaEditorSection.WORLD;
    if (field.startsWith("locations.waiting")) return ArenaEditorSection.WAITING;
    if (field.startsWith("locations.spectator")) return ArenaEditorSection.SPECTATOR;
    if (field.startsWith("players")) return ArenaEditorSection.PLAYERS;
    if (field.startsWith("teams")) return ArenaEditorSection.TEAMS;
    if (field.startsWith("boundary")) return ArenaEditorSection.BOUNDARY;
    return ArenaEditorSection.INFORMATION;
  }
}
