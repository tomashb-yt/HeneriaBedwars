package fr.heneria.zombie.plugin.gui;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe registry allowing future modules to contribute screens. */
public final class GuiRegistry {

  private final Map<GuiId, Gui> guis = new ConcurrentHashMap<>();

  /**
   * Registers one unique GUI.
   *
   * @param gui screen
   */
  public void register(Gui gui) {
    Objects.requireNonNull(gui, "gui");
    if (guis.putIfAbsent(gui.id(), gui) != null) {
      throw new IllegalArgumentException("GUI already registered: " + gui.id());
    }
  }

  /**
   * Finds a screen.
   *
   * @param id identifier
   * @return optional screen
   */
  public Optional<Gui> find(GuiId id) {
    return Optional.ofNullable(guis.get(id));
  }

  /**
   * @return immutable registered screens
   */
  public Collection<Gui> all() {
    return java.util.List.copyOf(guis.values());
  }
}
