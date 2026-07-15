package fr.heneria.bedwars.core.gui;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe index enforcing one active GUI session per player. */
public final class GuiSessionManager {
  private final Map<UUID, GuiSession> sessions = new ConcurrentHashMap<>();

  public GuiSession create(UUID player, Gui gui, int maxHistory) {
    GuiSession session = new GuiSession(player, gui, maxHistory);
    sessions.put(player, session);
    return session;
  }

  public Optional<GuiSession> find(UUID player) {
    return Optional.ofNullable(sessions.get(player));
  }

  public boolean remove(UUID player, UUID sessionId, UUID viewId) {
    return sessions.computeIfPresent(
            player,
            (ignored, current) ->
                current.sessionId().equals(sessionId) && current.viewId().equals(viewId)
                    ? null
                    : current)
        == null;
  }

  public Optional<GuiSession> remove(UUID player) {
    return Optional.ofNullable(sessions.remove(player));
  }

  public int size() {
    return sessions.size();
  }

  public Collection<GuiSession> all() {
    return java.util.List.copyOf(sessions.values());
  }

  public void clear() {
    sessions.clear();
  }
}
