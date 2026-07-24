package fr.heneria.zombie.plugin.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import fr.heneria.zombie.api.PluginState;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class PaperZombieApiTest {

  @Test
  void reportsStateAndSanitizesCounts() {
    AtomicReference<PluginState> state = new AtomicReference<>(PluginState.STARTING);
    PaperZombieApi api = new PaperZombieApi(state, () -> -4, () -> 3);

    state.set(PluginState.RUNNING);

    assertEquals(PluginState.RUNNING, api.state());
    assertEquals(0, api.registeredMapCount());
    assertEquals(3, api.activeInstanceCount());
  }
}
