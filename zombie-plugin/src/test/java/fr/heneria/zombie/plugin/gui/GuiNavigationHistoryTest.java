package fr.heneria.zombie.plugin.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GuiNavigationHistoryTest {

  @Test
  void isLifoAndBounded() {
    GuiNavigationHistory history = new GuiNavigationHistory();
    for (int index = 0; index < 40; index++) {
      history.push(new GuiId("menu-" + index), GuiContext.EMPTY);
    }

    assertEquals(32, history.size());
    assertEquals("menu-39", history.pop().orElseThrow().id().value());
  }
}
