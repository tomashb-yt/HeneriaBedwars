package fr.heneria.bedwars.core.gui;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Mutable per-player navigation state; it stores UUIDs rather than a platform player reference. */
public final class GuiSession {
  private final UUID playerId;
  private final UUID sessionId = UUID.randomUUID();
  private UUID viewId = UUID.randomUUID();
  private final Gui root;
  private Gui current;
  private final Deque<Gui> history = new ArrayDeque<>();
  private final int maximumHistory;
  private final Instant openedAt = Instant.now();
  private final Map<String, Object> data = new HashMap<>();
  private final Map<String, Long> cooldowns = new HashMap<>();
  private int page;
  private long refreshCount;
  private long lastRefreshMillis;
  private boolean closing;

  public GuiSession(UUID playerId, Gui initial, int maximumHistory) {
    if (maximumHistory < 0)
      throw new IllegalArgumentException("maximumHistory must not be negative");
    this.playerId = playerId;
    root = initial;
    current = initial;
    this.maximumHistory = maximumHistory;
  }

  public UUID playerId() {
    return playerId;
  }

  public UUID sessionId() {
    return sessionId;
  }

  public UUID viewId() {
    return viewId;
  }

  public Gui current() {
    return current;
  }

  public Gui root() {
    return root;
  }

  public int page() {
    return page;
  }

  public void page(int value) {
    page = Math.max(0, value);
  }

  public Instant openedAt() {
    return openedAt;
  }

  public Map<String, Object> data() {
    return data;
  }

  public int historySize() {
    return history.size();
  }

  public long refreshCount() {
    return refreshCount;
  }

  public long lastRefreshMillis() {
    return lastRefreshMillis;
  }

  public boolean closing() {
    return closing;
  }

  public void closing(boolean value) {
    closing = value;
  }

  public void navigate(Gui target, boolean remember) {
    if (remember && maximumHistory > 0) {
      history.addLast(current);
      while (history.size() > maximumHistory) history.removeFirst();
    }
    current = target;
    viewId = UUID.randomUUID();
    page = 0;
    closing = false;
  }

  public Optional<Gui> back() {
    if (history.isEmpty()) return Optional.empty();
    current = history.removeLast();
    viewId = UUID.randomUUID();
    page = 0;
    return Optional.of(current);
  }

  public Gui goRoot() {
    history.clear();
    current = root;
    viewId = UUID.randomUUID();
    page = 0;
    return current;
  }

  public void refreshed(long nowMillis) {
    refreshCount++;
    lastRefreshMillis = nowMillis;
  }

  public boolean acceptClick(String key, long cooldownMillis, long nowMillis) {
    if (cooldownMillis <= 0) return true;
    Long previous = cooldowns.put(key, nowMillis);
    if (previous != null && nowMillis - previous < cooldownMillis) {
      cooldowns.put(key, previous);
      return false;
    }
    return true;
  }
}
