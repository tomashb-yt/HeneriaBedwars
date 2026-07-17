package fr.heneria.bedwars.plugin.game;

import fr.heneria.bedwars.core.game.display.RuntimeScoreboardView;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/** Stable personal sidebar: teams and entries are created once, then only changed lines update. */
final class BukkitScoreboardSession {
  private final Scoreboard scoreboard;
  private final Objective objective;
  private final List<Team> teams = new ArrayList<>();
  private final List<String> entries = new ArrayList<>();
  private List<String> rendered = List.of();

  BukkitScoreboardSession(Scoreboard scoreboard, String title, boolean hideNumbers) {
    this.scoreboard = scoreboard;
    this.objective = scoreboard.registerNewObjective("hbw", "dummy", title);
    objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    if (hideNumbers) ScoreboardNumberHider.tryHide(objective);
    for (int index = 0; index < 15; index++) {
      String entry = ChatColor.values()[index].toString() + ChatColor.RESET;
      Team team = scoreboard.registerNewTeam("hbw_line_" + index);
      team.addEntry(entry);
      teams.add(team);
      entries.add(entry);
    }
  }

  Scoreboard scoreboard() {
    return scoreboard;
  }

  void update(RuntimeScoreboardView view) {
    if (!objective.getDisplayName().equals(view.title())) objective.setDisplayName(view.title());
    List<String> lines = view.lines();
    for (int index = 0; index < 15; index++) {
      boolean visible = index < lines.size();
      String next = visible ? lines.get(index) : "";
      String previous = index < rendered.size() ? rendered.get(index) : null;
      if (visible && !next.equals(previous)) teams.get(index).setPrefix(next);
      if (visible && !objective.getScore(entries.get(index)).isScoreSet())
        objective.getScore(entries.get(index)).setScore(lines.size() - index);
      if (!visible && objective.getScore(entries.get(index)).isScoreSet())
        scoreboard.resetScores(entries.get(index));
    }
    rendered = List.copyOf(lines);
  }
}
