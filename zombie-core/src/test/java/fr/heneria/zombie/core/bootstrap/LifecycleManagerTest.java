package fr.heneria.zombie.core.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LifecycleManagerTest {

  @Test
  void startsInDeclarationOrderAndStopsInReverseOrder() throws Exception {
    List<String> events = new ArrayList<>();
    LifecycleManager lifecycle = new LifecycleManager();
    lifecycle.add(component("first", events, false, false));
    lifecycle.add(component("second", events, false, false));

    lifecycle.startAll();
    lifecycle.stopAll();

    assertEquals(List.of("start:first", "start:second", "stop:second", "stop:first"), events);
    assertEquals(0, lifecycle.startedCount());
  }

  @Test
  void startupFailureRollsBackAlreadyStartedComponents() {
    List<String> events = new ArrayList<>();
    LifecycleManager lifecycle = new LifecycleManager();
    lifecycle.add(component("first", events, false, false));
    lifecycle.add(component("broken", events, true, false));

    assertThrows(LifecycleException.class, lifecycle::startAll);

    assertEquals(List.of("start:first", "start:broken", "stop:first"), events);
    assertEquals(0, lifecycle.startedCount());
  }

  @Test
  void shutdownAttemptsEveryComponentAfterFailure() throws Exception {
    List<String> events = new ArrayList<>();
    LifecycleManager lifecycle = new LifecycleManager();
    lifecycle.add(component("first", events, false, false));
    lifecycle.add(component("broken", events, false, true));
    lifecycle.startAll();

    assertThrows(LifecycleException.class, lifecycle::stopAll);

    assertEquals(List.of("start:first", "start:broken", "stop:broken", "stop:first"), events);
  }

  private static LifecycleComponent component(
      String name, List<String> events, boolean failStart, boolean failStop) {
    return new LifecycleComponent() {
      @Override
      public String name() {
        return name;
      }

      @Override
      public void start() throws Exception {
        events.add("start:" + name);
        if (failStart) {
          throw new Exception("start");
        }
      }

      @Override
      public void stop() throws Exception {
        events.add("stop:" + name);
        if (failStop) {
          throw new Exception("stop");
        }
      }
    };
  }
}
