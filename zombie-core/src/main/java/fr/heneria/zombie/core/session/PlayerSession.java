package fr.heneria.zombie.core.session;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Thread-safe aggregate enforcing exactly one logical context per player. */
public final class PlayerSession {

  private final UUID playerId;
  private PlayerContext context = PlayerContext.LOBBY;
  private UUID instanceId;
  private boolean online;
  private Instant reconnectDeadline;

  /**
   * Creates a lobby session.
   *
   * @param playerId player identifier
   * @param online initial online state
   */
  public PlayerSession(UUID playerId, boolean online) {
    this.playerId = Objects.requireNonNull(playerId, "playerId");
    this.online = online;
  }

  /**
   * Assigns one instance, refusing cross-instance membership.
   *
   * @param targetInstance target instance
   * @return assignment result
   */
  public synchronized SessionAssignmentResult assignInstance(UUID targetInstance) {
    Objects.requireNonNull(targetInstance, "targetInstance");
    if (context == PlayerContext.INSTANCE) {
      return instanceId.equals(targetInstance)
          ? SessionAssignmentResult.ALREADY_ASSIGNED
          : SessionAssignmentResult.OTHER_INSTANCE;
    }
    context = PlayerContext.INSTANCE;
    instanceId = targetInstance;
    reconnectDeadline = null;
    return SessionAssignmentResult.ASSIGNED;
  }

  /** Moves the session to the lobby and removes every instance reference. */
  public synchronized void moveToLobby() {
    context = PlayerContext.LOBBY;
    instanceId = null;
    reconnectDeadline = null;
  }

  /** Marks the player online and clears any grace deadline. */
  public synchronized void markOnline() {
    online = true;
    reconnectDeadline = null;
  }

  /**
   * Marks the player offline.
   *
   * @param deadline reconnect deadline for an instance session, otherwise empty
   */
  public synchronized void markOffline(Optional<Instant> deadline) {
    online = false;
    reconnectDeadline = deadline.orElse(null);
  }

  /**
   * Returns whether the reconnect reservation has expired.
   *
   * @param now current instant
   * @return expiration state
   */
  public synchronized boolean reconnectExpired(Instant now) {
    return !online
        && context == PlayerContext.INSTANCE
        && reconnectDeadline != null
        && !now.isBefore(reconnectDeadline);
  }

  /**
   * Returns a snapshot.
   *
   * @return immutable snapshot
   */
  public synchronized PlayerSessionSnapshot snapshot() {
    return new PlayerSessionSnapshot(
        playerId,
        context,
        Optional.ofNullable(instanceId),
        online,
        Optional.ofNullable(reconnectDeadline));
  }
}
