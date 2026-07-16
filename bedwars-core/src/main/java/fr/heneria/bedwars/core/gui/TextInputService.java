package fr.heneria.bedwars.core.gui;

import java.util.Collection;
import java.util.UUID;

/**
 * Thread-safe boundary for one-shot administrative chat input.
 *
 * <p>Platform listeners must cancel publication before forwarding text and must invoke callbacks on
 * the server thread.
 */
public interface TextInputService {
  /** Starts a session unless that player already owns one. */
  boolean begin(UUID playerId, TextInputRequest request);

  /** May be called from an asynchronous chat listener. */
  boolean active(UUID playerId);

  /** Submits text on the platform's safe callback thread. */
  TextInputSubmission submit(UUID playerId, String message);

  /** Ends one session and invokes its cancellation callback. */
  boolean cancel(UUID playerId, TextInputCancelReason reason);

  /** Expires overdue sessions; callers schedule this on a safe thread. */
  Collection<UUID> expire();

  /** Ends every session during plugin shutdown. */
  void cancelAll(TextInputCancelReason reason);
}
