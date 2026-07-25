package fr.heneria.zombie.plugin.gui;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Extensible registry for stable action identifiers referenced by YAML buttons. */
public final class GuiActionRegistry {

  private final Map<String, GuiAction> actions = new ConcurrentHashMap<>();

  /**
   * Registers an action.
   *
   * @param id safe action identifier
   * @param action action
   */
  public void register(String id, GuiAction action) {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(action, "action");
    if (!id.matches("[a-z0-9][a-z0-9_.-]{0,95}")) {
      throw new IllegalArgumentException("Invalid GUI action id: " + id);
    }
    if (actions.putIfAbsent(id, action) != null) {
      throw new IllegalArgumentException("GUI action already registered: " + id);
    }
  }

  /**
   * Resolves an action.
   *
   * @param id identifier
   * @return optional action
   */
  public Optional<GuiAction> find(String id) {
    return Optional.ofNullable(actions.get(id));
  }

  /**
   * @return immutable known action identifiers
   */
  public Set<String> ids() {
    return Set.copyOf(actions.keySet());
  }
}
