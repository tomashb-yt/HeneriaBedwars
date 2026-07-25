package fr.heneria.zombie.core.session;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/** Concurrent source of truth for lobby and instance membership. */
public final class PlayerSessionService {

  private final ConcurrentMap<UUID, PlayerSession> sessions = new ConcurrentHashMap<>();
  private final Supplier<ReconnectPolicy> reconnectPolicy;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param reconnectPolicy current reconnect policy
   * @param clock time source
   */
  public PlayerSessionService(Supplier<ReconnectPolicy> reconnectPolicy, Clock clock) {
    this.reconnectPolicy = Objects.requireNonNull(reconnectPolicy, "reconnectPolicy");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /**
   * Connects a player and selects the safe destination.
   *
   * @param playerId player identifier
   * @return reconnect decision
   */
  public ReconnectDecision connect(UUID playerId) {
    PlayerSession session = sessions.computeIfAbsent(playerId, id -> new PlayerSession(id, true));
    synchronized (session) {
      PlayerSessionSnapshot before = session.snapshot();
      boolean canReturn =
          before.context() == PlayerContext.INSTANCE
              && before.reconnectDeadline().map(clock.instant()::isBefore).orElse(false);
      if (!canReturn && !before.online()) {
        session.moveToLobby();
      }
      session.markOnline();
      return canReturn ? ReconnectDecision.RETURN_TO_INSTANCE : ReconnectDecision.RETURN_TO_LOBBY;
    }
  }

  /**
   * Marks a player disconnected and starts grace retention when configured.
   *
   * @param playerId player identifier
   * @return resulting session snapshot
   */
  public PlayerSessionSnapshot disconnect(UUID playerId) {
    PlayerSession session = sessions.computeIfAbsent(playerId, id -> new PlayerSession(id, false));
    ReconnectPolicy policy = reconnectPolicy.get();
    synchronized (session) {
      PlayerSessionSnapshot before = session.snapshot();
      Optional<Instant> deadline =
          policy.enabled()
                  && policy.reservePlayerSlot()
                  && before.context() == PlayerContext.INSTANCE
              ? Optional.of(clock.instant().plus(policy.gracePeriod()))
              : Optional.empty();
      if (deadline.isEmpty() && before.context() == PlayerContext.INSTANCE) {
        session.moveToLobby();
      }
      session.markOffline(deadline);
      return session.snapshot();
    }
  }

  /**
   * Assigns a session to exactly one instance.
   *
   * @param playerId player identifier
   * @param instanceId instance identifier
   * @return assignment result
   */
  public SessionAssignmentResult assignInstance(UUID playerId, UUID instanceId) {
    PlayerSession session = sessions.computeIfAbsent(playerId, id -> new PlayerSession(id, true));
    return session.assignInstance(instanceId);
  }

  /**
   * Sends a session to the lobby.
   *
   * @param playerId player identifier
   * @return previous instance, when present
   */
  public Optional<UUID> sendToLobby(UUID playerId) {
    PlayerSession session = sessions.computeIfAbsent(playerId, id -> new PlayerSession(id, true));
    synchronized (session) {
      Optional<UUID> previous = session.snapshot().instanceId();
      session.moveToLobby();
      return previous;
    }
  }

  /**
   * Finds a session.
   *
   * @param playerId player identifier
   * @return optional snapshot
   */
  public Optional<PlayerSessionSnapshot> findSession(UUID playerId) {
    return Optional.ofNullable(sessions.get(playerId)).map(PlayerSession::snapshot);
  }

  /**
   * Returns every current snapshot.
   *
   * @return immutable snapshot collection
   */
  public Collection<PlayerSessionSnapshot> sessions() {
    return sessions.values().stream().map(PlayerSession::snapshot).toList();
  }

  /**
   * Expires disconnected instance reservations.
   *
   * @return reservations that must be removed from instance membership
   */
  public List<ExpiredReservation> expireReconnectReservations() {
    Instant now = clock.instant();
    ReconnectPolicy policy = reconnectPolicy.get();
    if (!policy.returnToLobbyAfterExpiration()) {
      return List.of();
    }
    return sessions.values().stream()
        .filter(session -> session.reconnectExpired(now))
        .map(
            session -> {
              synchronized (session) {
                PlayerSessionSnapshot snapshot = session.snapshot();
                UUID instanceId = snapshot.instanceId().orElseThrow();
                session.moveToLobby();
                session.markOffline(Optional.empty());
                return new ExpiredReservation(snapshot.playerId(), instanceId);
              }
            })
        .toList();
  }

  /** Clears all retained references during plugin shutdown. */
  public void clear() {
    sessions.clear();
  }
}
