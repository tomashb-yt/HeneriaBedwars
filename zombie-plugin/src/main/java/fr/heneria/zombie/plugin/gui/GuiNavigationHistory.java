package fr.heneria.zombie.plugin.gui;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/** Bounded per-player back-navigation history. */
public final class GuiNavigationHistory {

  private static final int MAX_ENTRIES = 32;
  private final Deque<Entry> entries = new ArrayDeque<>();

  /**
   * Pushes a previous screen.
   *
   * @param id screen id
   * @param context screen context
   */
  public void push(GuiId id, GuiContext context) {
    if (entries.size() == MAX_ENTRIES) {
      entries.removeFirst();
    }
    entries.addLast(new Entry(id, context));
  }

  /**
   * @return most recent previous screen
   */
  public Optional<Entry> pop() {
    return Optional.ofNullable(entries.pollLast());
  }

  /** Clears all history. */
  public void clear() {
    entries.clear();
  }

  /**
   * @return number of retained entries
   */
  public int size() {
    return entries.size();
  }

  /** Immutable navigation entry. */
  public record Entry(GuiId id, GuiContext context) {}
}
