package fr.heneria.bedwars.core.game;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bounded last-attacker tracker used for void and delayed kill credit. */
public final class CombatTracker {
  private final Map<UUID, Attack> attacks = new ConcurrentHashMap<>();

  public void record(UUID victim, UUID attacker, Instant at) {
    if (victim.equals(attacker)) return;
    attacks.put(victim, new Attack(attacker, at));
  }

  public Optional<UUID> attacker(UUID victim, Instant now, Duration window) {
    Attack attack = attacks.get(victim);
    if (attack == null) return Optional.empty();
    if (attack.at().plus(window).isBefore(now)) {
      attacks.remove(victim, attack);
      return Optional.empty();
    }
    return Optional.of(attack.attacker());
  }

  public void forget(UUID playerId) {
    attacks.remove(playerId);
    attacks.entrySet().removeIf(entry -> entry.getValue().attacker().equals(playerId));
  }

  public void clear() {
    attacks.clear();
  }

  private record Attack(UUID attacker, Instant at) {}
}
