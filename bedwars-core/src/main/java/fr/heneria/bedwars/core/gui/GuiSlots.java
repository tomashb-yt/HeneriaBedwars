package fr.heneria.bedwars.core.gui;

import java.util.ArrayList;
import java.util.List;

/** Helpers for deterministic inventory slot regions. */
public final class GuiSlots {
  private GuiSlots() {}

  public static List<Integer> rectangle(int firstRow, int firstColumn, int rows, int columns) {
    if (firstRow < 0
        || firstColumn < 0
        || rows < 1
        || columns < 1
        || firstColumn + columns > 9
        || firstRow + rows > 6)
      throw new IllegalArgumentException("rectangle must fit inside a 6x9 inventory");
    List<Integer> slots = new ArrayList<>();
    for (int row = firstRow; row < firstRow + rows; row++)
      for (int column = firstColumn; column < firstColumn + columns; column++)
        slots.add(row * 9 + column);
    return List.copyOf(slots);
  }
}
