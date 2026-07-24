package fr.heneria.zombie.core.bootstrap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Starts components in declaration order and stops them in reverse order.
 *
 * <p>A failed startup rolls back every component that had already started. The manager is intended
 * to be owned by one plugin bootstrap and is synchronized to reject concurrent lifecycle changes.
 */
public final class LifecycleManager {

  private final List<LifecycleComponent> configured = new ArrayList<>();
  private final List<LifecycleComponent> started = new ArrayList<>();
  private boolean startingOrRunning;

  /**
   * Adds a component before startup.
   *
   * @param component component to own
   * @throws IllegalStateException when startup already began
   */
  public synchronized void add(LifecycleComponent component) {
    Objects.requireNonNull(component, "component");
    if (startingOrRunning) {
      throw new IllegalStateException("Cannot add lifecycle components after startup");
    }
    configured.add(component);
  }

  /**
   * Starts every configured component.
   *
   * @throws LifecycleException when startup fails; already-started components are rolled back
   */
  public synchronized void startAll() throws LifecycleException {
    if (startingOrRunning) {
      throw new IllegalStateException("Lifecycle is already started");
    }
    startingOrRunning = true;
    for (LifecycleComponent component : configured) {
      try {
        component.start();
        started.add(component);
      } catch (Exception failure) {
        LifecycleException lifecycleFailure =
            new LifecycleException("Could not start component " + component.name(), failure);
        rollback(lifecycleFailure);
        startingOrRunning = false;
        throw lifecycleFailure;
      }
    }
  }

  /**
   * Stops every started component in reverse order.
   *
   * @throws LifecycleException when at least one component cannot stop; all components are still
   *     attempted
   */
  public synchronized void stopAll() throws LifecycleException {
    LifecycleException failure = null;
    for (int index = started.size() - 1; index >= 0; index--) {
      LifecycleComponent component = started.get(index);
      try {
        component.stop();
      } catch (Exception stopFailure) {
        if (failure == null) {
          failure =
              new LifecycleException("Could not stop component " + component.name(), stopFailure);
        } else {
          failure.addSuppressed(stopFailure);
        }
      }
    }
    started.clear();
    startingOrRunning = false;
    if (failure != null) {
      throw failure;
    }
  }

  /**
   * Returns the number of successfully started components.
   *
   * @return current started count
   */
  public synchronized int startedCount() {
    return started.size();
  }

  private void rollback(LifecycleException lifecycleFailure) {
    for (int index = started.size() - 1; index >= 0; index--) {
      try {
        started.get(index).stop();
      } catch (Exception rollbackFailure) {
        lifecycleFailure.addSuppressed(rollbackFailure);
      }
    }
    started.clear();
  }
}
