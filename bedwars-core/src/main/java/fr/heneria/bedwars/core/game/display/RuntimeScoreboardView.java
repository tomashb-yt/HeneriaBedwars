package fr.heneria.bedwars.core.game.display;

import java.util.List;

/** Immutable rendered sidebar content, independent from Bukkit scoreboard objects. */
public record RuntimeScoreboardView(String title, List<String> lines) {
  public RuntimeScoreboardView {
    title = title == null ? "" : title;
    lines = List.copyOf(lines);
    if (lines.size() > 15) throw new IllegalArgumentException("A sidebar cannot exceed 15 lines");
  }
}
