package fr.heneria.bedwars.core.game;

import fr.heneria.bedwars.api.game.RuntimePlayerSnapshot;
import fr.heneria.bedwars.core.game.equipment.EquipmentKind;
import fr.heneria.bedwars.core.game.equipment.EquipmentPurchaseCode;
import fr.heneria.bedwars.core.game.equipment.PlayerEquipment;
import fr.heneria.bedwars.core.game.equipment.PlayerEquipmentSnapshot;
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
  private boolean respawning;
  private boolean finalDeath;
  private Instant lastDeathAt;
  private Instant respawnAt;
  private Instant protectedUntil;
  private final PlayerEquipment equipment = new PlayerEquipment();

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

  public EquipmentPurchaseCode canPurchaseEquipment(EquipmentKind kind, int tier) {
    return equipment.canPurchase(kind, tier);
  }

  public boolean purchaseEquipment(EquipmentKind kind, int tier) {
    return equipment.purchase(kind, tier);
  }

  public PlayerEquipmentSnapshot equipment() {
    return equipment.snapshot();
  }

  public void degradeTools() {
    equipment.onDeath();
  }

  public synchronized void spectator(boolean value) {
    spectator = value;
  }

  public synchronized boolean spectator() {
    return spectator;
  }

  public synchronized boolean respawning() {
    return respawning;
  }

  public synchronized boolean finalDeath() {
    return finalDeath;
  }

  public synchronized void scheduleRespawn(Instant deathAt, Instant dueAt) {
    lastDeathAt = Objects.requireNonNull(deathAt, "deathAt");
    respawnAt = Objects.requireNonNull(dueAt, "dueAt");
    respawning = true;
    spectator = true;
  }

  public synchronized void completeRespawn(Instant protectionEnd) {
    respawning = false;
    respawnAt = null;
    spectator = false;
    protectedUntil = protectionEnd;
  }

  public synchronized void eliminate(Instant at) {
    lastDeathAt = Objects.requireNonNull(at, "at");
    respawning = false;
    respawnAt = null;
    finalDeath = true;
    spectator = true;
  }

  public synchronized Optional<Instant> respawnAt() {
    return Optional.ofNullable(respawnAt);
  }

  public synchronized boolean protectedAt(Instant now) {
    return protectedUntil != null && now.isBefore(protectedUntil);
  }

  public synchronized void clearProtection() {
    protectedUntil = null;
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
