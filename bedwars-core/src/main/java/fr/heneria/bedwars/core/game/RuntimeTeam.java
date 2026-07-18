package fr.heneria.bedwars.core.game;

import fr.heneria.bedwars.api.game.RuntimeTeamSnapshot;
import fr.heneria.bedwars.core.arena.ArenaTeamDefinition;
import fr.heneria.bedwars.core.game.upgrade.TeamUpgradeSnapshot;
import fr.heneria.bedwars.core.game.upgrade.TeamUpgradeType;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Team scaffold for future BedWars mechanics; no bed gameplay is active in Ticket 009. */
public final class RuntimeTeam {
  private final String id;
  private final String displayName;
  private final String color;
  private final int capacity;
  private final Set<UUID> players = new LinkedHashSet<>();
  private Optional<RuntimeLocation> spawn = Optional.empty();
  private boolean bedAlive = true;
  private boolean eliminated;
  private final EnumMap<TeamUpgradeType, Integer> upgrades = new EnumMap<>(TeamUpgradeType.class);

  public RuntimeTeam(String id, String displayName, String color, int capacity) {
    this.id = text(id, "id");
    this.displayName = text(displayName, "displayName");
    this.color = text(color, "color");
    if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
    this.capacity = capacity;
  }

  public RuntimeTeam(ArenaTeamDefinition definition) {
    this(
        definition.id().value(),
        definition.displayName(),
        definition.color().name(),
        definition.capacity());
    definition
        .spawn()
        .ifPresent(
            location ->
                spawn(
                    new RuntimeLocation(
                        location.position().x(),
                        location.position().y(),
                        location.position().z(),
                        location.yaw(),
                        location.pitch())));
  }

  public String id() {
    return id;
  }

  public synchronized int size() {
    return players.size();
  }

  public int capacity() {
    return capacity;
  }

  public synchronized boolean full() {
    return players.size() >= capacity;
  }

  synchronized void add(UUID playerId) {
    players.add(playerId);
  }

  synchronized void remove(UUID playerId) {
    players.remove(playerId);
  }

  public synchronized Optional<RuntimeLocation> spawn() {
    return spawn;
  }

  public synchronized void spawn(RuntimeLocation value) {
    spawn = Optional.of(Objects.requireNonNull(value, "value"));
  }

  public synchronized void bedAlive(boolean value) {
    if (bedAlive == false && value)
      throw new IllegalStateException("A destroyed bed cannot revive");
    bedAlive = value;
  }

  public synchronized boolean bedAlive() {
    return bedAlive;
  }

  public synchronized void eliminated(boolean value) {
    eliminated = value;
  }

  public synchronized boolean eliminated() {
    return eliminated;
  }

  public synchronized Set<UUID> playerIds() {
    return Set.copyOf(players);
  }

  public synchronized int upgradeLevel(TeamUpgradeType type) {
    return upgrades.getOrDefault(Objects.requireNonNull(type, "type"), 0);
  }

  public synchronized int upgrade(TeamUpgradeType type, int maximumLevel) {
    if (maximumLevel < 1) throw new IllegalArgumentException("maximumLevel must be positive");
    int current = upgradeLevel(type);
    if (current >= maximumLevel) return current;
    int next = current + 1;
    upgrades.put(type, next);
    return next;
  }

  public synchronized TeamUpgradeSnapshot upgrades() {
    return new TeamUpgradeSnapshot(upgrades);
  }

  public synchronized RuntimeTeamSnapshot snapshot() {
    return new RuntimeTeamSnapshot(id, displayName, color, players, bedAlive, eliminated);
  }

  private static String text(String value, String field) {
    String clean = Objects.requireNonNull(value, field).trim();
    if (clean.isEmpty()) throw new IllegalArgumentException(field + " is blank");
    return clean;
  }
}
