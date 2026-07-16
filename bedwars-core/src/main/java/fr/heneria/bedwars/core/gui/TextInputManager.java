package fr.heneria.bedwars.core.gui;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** In-memory, synchronized input coordinator; callbacks run on the caller's thread. */
public final class TextInputManager implements TextInputService {
  private final Clock clock;
  private final Map<UUID, TextInputSession> sessions = new HashMap<>();

  public TextInputManager(Clock clock) {
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Override
  public synchronized boolean begin(UUID playerId, TextInputRequest request) {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(request, "request");
    if (sessions.containsKey(playerId)) return false;
    sessions.put(
        playerId, new TextInputSession(playerId, request, clock.instant().plus(request.timeout())));
    return true;
  }

  @Override
  public synchronized boolean active(UUID playerId) {
    return sessions.containsKey(playerId);
  }

  @Override
  public TextInputSubmission submit(UUID playerId, String message) {
    TextInputSession session;
    String value = message == null ? "" : message.trim();
    String invalidProblem = null;
    TextInputSubmission submission;
    synchronized (this) {
      session = sessions.get(playerId);
      if (session == null) return TextInputSubmission.NOT_ACTIVE;
      if (session.request().cancelKeywords().contains(value.toLowerCase(Locale.ROOT))) {
        sessions.remove(playerId);
        submission = TextInputSubmission.CANCELLED;
      } else if (value.isEmpty() || value.length() > session.request().maximumLength()) {
        invalidProblem = "length";
        submission = TextInputSubmission.INVALID;
      } else {
        Optional<String> problem = session.request().validator().apply(value);
        if (problem.isPresent()) {
          invalidProblem = problem.orElseThrow();
          submission = TextInputSubmission.INVALID;
        } else {
          sessions.remove(playerId);
          submission = TextInputSubmission.ACCEPTED;
        }
      }
    }
    if (submission == TextInputSubmission.ACCEPTED) session.request().onSuccess().accept(value);
    else if (submission == TextInputSubmission.INVALID)
      session.request().onInvalid().accept(invalidProblem);
    else session.request().onCancel().accept(TextInputCancelReason.PLAYER);
    return submission;
  }

  @Override
  public boolean cancel(UUID playerId, TextInputCancelReason reason) {
    TextInputSession removed;
    synchronized (this) {
      removed = sessions.remove(playerId);
    }
    if (removed == null) return false;
    removed.request().onCancel().accept(reason);
    return true;
  }

  @Override
  public Collection<UUID> expire() {
    Instant now = clock.instant();
    List<TextInputSession> expired = new ArrayList<>();
    synchronized (this) {
      sessions.values().stream()
          .filter(session -> !session.expiresAt().isAfter(now))
          .toList()
          .forEach(
              session -> {
                sessions.remove(session.playerId());
                expired.add(session);
              });
    }
    expired.forEach(session -> session.request().onCancel().accept(TextInputCancelReason.TIMEOUT));
    return expired.stream().map(TextInputSession::playerId).toList();
  }

  @Override
  public void cancelAll(TextInputCancelReason reason) {
    List<UUID> ids;
    synchronized (this) {
      ids = List.copyOf(sessions.keySet());
    }
    ids.forEach(id -> cancel(id, reason));
  }
}
