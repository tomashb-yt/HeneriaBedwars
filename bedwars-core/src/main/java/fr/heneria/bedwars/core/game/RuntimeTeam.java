package fr.heneria.bedwars.core.game;

import fr.heneria.bedwars.api.game.RuntimeTeamSnapshot;
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
  private final Set<UUID> players = new LinkedHashSet<>();
  private Optional<RuntimeLocation> spawn = Optional.empty();
  private boolean bedAlive = true;
  private boolean eliminated;

  public RuntimeTeam(String id, String displayName, String color) {
    this.id = text(id, "id");
    this.displayName = text(displayName, "displayName");
    this.color = text(color, "color");
  }

  public String id() {
    return id;
  }

  public synchronized int size() {
    return players.size();
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
    bedAlive = value;
  }

  public synchronized void eliminated(boolean value) {
    eliminated = value;
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
