package fr.heneria.zombie.core.isolation;

import fr.heneria.zombie.core.session.PlayerContext;
import fr.heneria.zombie.core.session.PlayerSessionService;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Selects exact recipients for targeted lobby and instance broadcasts. */
public final class AudienceSelector {

  private final PlayerSessionService sessions;

  /**
   * Creates the selector.
   *
   * @param sessions session source of truth
   */
  public AudienceSelector(PlayerSessionService sessions) {
    this.sessions = Objects.requireNonNull(sessions, "sessions");
  }

  /**
   * Selects online lobby players.
   *
   * @return immutable recipient identifiers
   */
  public Set<UUID> lobby() {
    return sessions.sessions().stream()
        .filter(session -> session.online() && session.context() == PlayerContext.LOBBY)
        .map(session -> session.playerId())
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Selects online players from one instance.
   *
   * @param instanceId instance identifier
   * @return immutable recipient identifiers
   */
  public Set<UUID> instance(UUID instanceId) {
    Objects.requireNonNull(instanceId, "instanceId");
    return sessions.sessions().stream()
        .filter(
            session ->
                session.online()
                    && session.context() == PlayerContext.INSTANCE
                    && session.instanceId().filter(instanceId::equals).isPresent())
        .map(session -> session.playerId())
        .collect(Collectors.toUnmodifiableSet());
  }
}
