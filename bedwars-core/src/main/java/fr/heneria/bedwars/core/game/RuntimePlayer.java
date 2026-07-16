package fr.heneria.bedwars.core.game;

import fr.heneria.bedwars.api.game.RuntimePlayerSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Mutable state owned exclusively by one {@link GameInstance}. */
public final class RuntimePlayer {
  private final UUID playerId;
  private final Instant joinedAt;
  private String teamId;
  private int kills;
  private int deaths;
  private int finalKills;
  private int bedsDestroyed;
  private boolean spectator;

  public RuntimePlayer(UUID playerId, Instant joinedAt) {
    this.playerId = Objects.requireNonNull(playerId, "playerId");
    this.joinedAt = Objects.requireNonNull(joinedAt, "joinedAt");
  }

  public UUID playerId() {
    return playerId;
  }

  public Optional<String> teamId() {
    return Optional.ofNullable(teamId);
  }

  synchronized void assignTeam(String value) {
    teamId = Objects.requireNonNull(value, "value");
  }

  public synchronized void recordKill(boolean finalKill) {
    kills++;
    if (finalKill) finalKills++;
  }

  public synchronized void recordDeath() {
    deaths++;
  }

  public synchronized void recordBedDestroyed() {
    bedsDestroyed++;
  }

  public synchronized void spectator(boolean value) {
    spectator = value;
  }

  public synchronized RuntimePlayerSnapshot snapshot(Instant now) {
    return new RuntimePlayerSnapshot(
        playerId,
        teamId(),
        kills,
        deaths,
        finalKills,
        bedsDestroyed,
        spectator,
        Duration.between(joinedAt, now).isNegative()
            ? Duration.ZERO
            : Duration.between(joinedAt, now));
  }
}
