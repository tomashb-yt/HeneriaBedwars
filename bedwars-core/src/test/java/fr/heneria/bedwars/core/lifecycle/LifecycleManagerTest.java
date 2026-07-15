package fr.heneria.bedwars.core.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fr.heneria.bedwars.core.logging.ProjectLogger;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LifecycleManagerTest {
  private final List<String> events = new ArrayList<>();

  @Test
  void startsAComponent() throws Exception {
    LifecycleManager manager = manager(component("one"));
    manager.startAll();
    assertEquals(List.of("start-one"), events);
    assertEquals(LifecycleState.RUNNING, manager.state());
  }

  @Test
  void stopsAComponent() throws Exception {
    LifecycleManager manager = manager(component("one"));
    manager.startAll();
    manager.stopAll();
    assertEquals(List.of("start-one", "stop-one"), events);
    assertEquals(LifecycleState.STOPPED, manager.state());
  }

  @Test
  void startsInRegistrationOrder() throws Exception {
    LifecycleManager manager = manager(component("one"), component("two"));
    manager.startAll();
    assertEquals(List.of("start-one", "start-two"), events);
  }

  @Test
  void stopsInReverseOrder() throws Exception {
    LifecycleManager manager = manager(component("one"), component("two"));
    manager.startAll();
    manager.stopAll();
    assertEquals(List.of("start-one", "start-two", "stop-two", "stop-one"), events);
  }

  @Test
  void rollsBackAfterStartFailure() {
    LifecycleManager manager = manager(component("one"), failingComponent("two"));
    assertThrows(LifecycleException.class, manager::startAll);
    assertEquals(List.of("start-one", "start-two", "stop-one"), events);
  }

  @Test
  void remainsFailedAfterStartFailure() {
    LifecycleManager manager = manager(failingComponent("one"));
    assertThrows(LifecycleException.class, manager::startAll);
    assertEquals(LifecycleState.FAILED, manager.state());
  }

  @Test
  void rejectsRepeatedStart() throws Exception {
    LifecycleManager manager = manager(component("one"));
    manager.startAll();
    assertThrows(IllegalStateException.class, manager::startAll);
  }

  @Test
  void rejectsStopBeforeStart() {
    LifecycleManager manager = manager(component("one"));
    assertThrows(IllegalStateException.class, manager::stopAll);
  }

  private LifecycleManager manager(LifecycleComponent... components) {
    return new LifecycleManager(List.of(components), new SilentLogger());
  }

  private LifecycleComponent component(String name) {
    return new TestComponent(name, false);
  }

  private LifecycleComponent failingComponent(String name) {
    return new TestComponent(name, true);
  }

  private final class TestComponent implements LifecycleComponent {
    private final String name;
    private final boolean failStart;

    private TestComponent(String name, boolean failStart) {
      this.name = name;
      this.failStart = failStart;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public void start() {
      events.add("start-" + name);
      if (failStart) {
        throw new IllegalStateException("expected test failure");
      }
    }

    @Override
    public void stop() {
      events.add("stop-" + name);
    }
  }

  private static final class SilentLogger implements ProjectLogger {
    @Override
    public void info(String message) {}

    @Override
    public void warning(String message) {}

    @Override
    public void error(String message, Throwable cause) {}

    @Override
    public void debug(String message) {}
  }
}
