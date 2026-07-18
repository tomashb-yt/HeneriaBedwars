package fr.heneria.bedwars.core.game;

import fr.heneria.bedwars.api.game.GameState;
import fr.heneria.bedwars.core.game.event.GameEventBus;
import fr.heneria.bedwars.core.game.event.GameVictoryEvent;
import fr.heneria.bedwars.core.game.event.PlayerFinalDeathEvent;
import fr.heneria.bedwars.core.game.event.PlayerGameDeathEvent;
import fr.heneria.bedwars.core.game.event.PlayerRespawnScheduledEvent;
import fr.heneria.bedwars.core.game.event.TeamEliminatedEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.IntSupplier;

/** Decides respawn/final-death/team elimination without any Bukkit dependency. */
public final class GameDeathService {
  private final GameInstanceManager games;
  private final GameEventBus events;
  private final Clock clock;
  private final IntSupplier respawnDelaySeconds;

  public GameDeathService(
      GameInstanceManager games,
      GameEventBus events,
      Clock clock,
      IntSupplier respawnDelaySeconds) {
    this.games = Objects.requireNonNull(games, "games");
    this.events = Objects.requireNonNull(events, "events");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.respawnDelaySeconds = Objects.requireNonNull(respawnDelaySeconds, "respawnDelaySeconds");
  }

  public GameDeathResult handle(UUID victimId, UUID killerId) {
    return handle(victimId, killerId, false);
  }

  private GameDeathResult handle(UUID victimId, UUID killerId, boolean forceFinal) {
    GameInstance game = games.byPlayer(victimId).orElse(null);
    if (game == null || game.state() != GameState.PLAYING) return GameDeathResult.ignored();
    Instant now = clock.instant();
    RuntimePlayer victim;
    RuntimeTeam team;
    DeathDecision decision;
    Optional<String> eliminated = Optional.empty();
    Optional<String> winner = Optional.empty();
    synchronized (game) {
      victim = game.player(victimId).orElse(null);
      if (victim == null || victim.respawning() || victim.finalDeath())
        return GameDeathResult.ignored();
      team = victim.teamId().flatMap(game::team).orElse(null);
      if (team == null) return GameDeathResult.ignored();
      victim.recordDeath();
      victim.degradeTools();
      RuntimePlayer killer =
          killerId == null || killerId.equals(victimId) ? null : game.player(killerId).orElse(null);
      if (team.bedAlive() && !forceFinal) {
        Instant dueAt = now.plusSeconds(Math.max(0, respawnDelaySeconds.getAsInt()));
        victim.scheduleRespawn(now, dueAt);
        if (killer != null) killer.recordKill(false);
        decision = DeathDecision.RESPAWN;
      } else {
        victim.eliminate(now);
        if (killer != null) killer.recordKill(true);
        decision = DeathDecision.FINAL_DEATH;
        if (eliminateIfFinished(game, team)) eliminated = Optional.of(team.id());
        winner = winner(game);
      }
    }
    Optional<UUID> killer = Optional.ofNullable(killerId).filter(id -> !id.equals(victimId));
    events.publish(
        new PlayerGameDeathEvent(
            game.id(), victimId, killer, decision == DeathDecision.FINAL_DEATH, now));
    if (decision == DeathDecision.RESPAWN)
      events.publish(
          new PlayerRespawnScheduledEvent(
              game.id(), victimId, victim.respawnAt().orElseThrow(), now));
    else events.publish(new PlayerFinalDeathEvent(game.id(), victimId, killer, now));
    eliminated.ifPresent(teamId -> events.publish(new TeamEliminatedEvent(game.id(), teamId, now)));
    if (winner.isPresent() && games.transition(game.id(), GameState.ENDING).successful())
      events.publish(new GameVictoryEvent(game.id(), winner.orElseThrow(), now));
    return new GameDeathResult(decision, Optional.of(game), eliminated, winner);
  }

  /** Disconnects are final during PLAYING because reconnect is intentionally not implemented. */
  public GameDeathResult disconnect(UUID playerId) {
    return handle(playerId, null, true);
  }

  private static boolean eliminateIfFinished(GameInstance game, RuntimeTeam team) {
    boolean canReturn =
        team.playerIds().stream()
            .map(game::player)
            .flatMap(Optional::stream)
            .anyMatch(player -> !player.finalDeath());
    if (canReturn || team.eliminated()) return false;
    team.eliminated(true);
    return true;
  }

  private static Optional<String> winner(GameInstance game) {
    List<RuntimeTeam> participants =
        game.teams().stream().filter(team -> !team.playerIds().isEmpty()).toList();
    if (participants.size() < 2) return Optional.empty();
    List<RuntimeTeam> alive = participants.stream().filter(team -> !team.eliminated()).toList();
    return alive.size() == 1 ? Optional.of(alive.getFirst().id()) : Optional.empty();
  }
}
