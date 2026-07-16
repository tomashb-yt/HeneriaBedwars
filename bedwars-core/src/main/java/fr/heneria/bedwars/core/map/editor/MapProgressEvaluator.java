package fr.heneria.bedwars.core.map.editor;

import fr.heneria.bedwars.core.map.MapTemplate;
import fr.heneria.bedwars.core.map.MapType;

/** Informational setup progress; it is deliberately separate from technical validation. */
public final class MapProgressEvaluator {
  public MapProgress evaluate(MapTemplate template, boolean primaryLobby) {
    boolean spawn = template.spawn().configured();
    boolean saved = template.lastSavedAt().isPresent();
    boolean linked = !template.linkedArenaIds().isEmpty();
    int total = template.type() == MapType.GENERIC ? 3 : 4;
    int current = 1 + (spawn ? 1 : 0) + (saved ? 1 : 0);
    if (template.type() == MapType.BEDWARS && linked) current++;
    if (template.type() == MapType.LOBBY && primaryLobby) current++;
    String next =
        !spawn
            ? "map.workflow.next.spawn"
            : !saved
                ? "map.workflow.next.save"
                : template.type() == MapType.BEDWARS && !linked
                    ? "map.workflow.next.link-arena"
                    : template.type() == MapType.LOBBY && !primaryLobby
                        ? "map.workflow.next.select-lobby"
                        : "map.workflow.next.complete";
    return new MapProgress(current, total, current * 100 / total, next);
  }
}
