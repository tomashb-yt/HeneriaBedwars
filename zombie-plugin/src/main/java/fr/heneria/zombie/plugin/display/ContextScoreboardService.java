package fr.heneria.zombie.plugin.display;

import fr.heneria.zombie.core.game.ZombieGame;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/** Owns one lobby scoreboard and one independent scoreboard per instance. */
public final class ContextScoreboardService {

  private final Scoreboard lobby = create("hz_lobby", Component.text("HeneriaZombie"));
  private final Map<UUID, Scoreboard> instances = new ConcurrentHashMap<>();

  /**
   * Applies the lobby board.
   *
   * @param player player
   */
  public void applyLobby(Player player) {
    player.setScoreboard(lobby);
  }

  /**
   * Applies an isolated instance board.
   *
   * @param player player
   * @param instanceId instance identifier
   */
  public void applyInstance(Player player, UUID instanceId) {
    player.setScoreboard(
        instances.computeIfAbsent(
            instanceId, ignored -> create("hz_game", Component.text("Zombies • En attente"))));
  }

  /** Updates the existing instance board without allocating a new scoreboard. */
  public void updateGame(Player player, ZombieGame.Snapshot game) {
    Scoreboard scoreboard = instances.get(game.gameId());
    if (scoreboard == null) {
      return;
    }
    Objective objective = scoreboard.getObjective("hz_game");
    if (objective == null) {
      return;
    }
    objective.displayName(Component.text("Zombies • " + game.mapId()));
    scoreboard.getEntries().forEach(scoreboard::resetScores);
    int round = game.round().map(value -> value.number()).orElse(0);
    int remaining =
        game.round()
            .map(value -> value.waitingEnemies() + value.aliveEnemies() + value.pendingSpawns())
            .orElse(0);
    int points =
        java.util.Optional.ofNullable(game.players().get(player.getUniqueId()))
            .map(value -> value.points())
            .orElse(0);
    objective.getScore("§6Manche : §f" + round).setScore(3);
    objective.getScore("§cZombies : §f" + remaining).setScore(2);
    objective.getScore("§aPoints : §f" + points).setScore(1);
  }

  /**
   * Drops a closed instance board.
   *
   * @param instanceId instance identifier
   */
  public void removeInstance(UUID instanceId) {
    instances.remove(Objects.requireNonNull(instanceId, "instanceId"));
  }

  /** Clears all runtime boards. */
  public void clear() {
    instances.clear();
  }

  private static Scoreboard create(String objectiveName, Component title) {
    Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
    Objective objective = scoreboard.registerNewObjective(objectiveName, Criteria.DUMMY, title);
    objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    return scoreboard;
  }
}
