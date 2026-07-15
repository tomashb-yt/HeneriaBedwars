package fr.heneria.bedwars.core.lifecycle;

import fr.heneria.bedwars.core.logging.ProjectLogger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Starts components in registration order and stops them in reverse order.
 *
 * <p>A start failure immediately rolls back every component that already started and leaves the
 * manager in {@link LifecycleState#FAILED}. Invalid repeated transitions are rejected.
 */
public final class LifecycleManager {
  private final List<LifecycleComponent> components;
  private final List<LifecycleComponent> started = new ArrayList<>();
  private final ProjectLogger logger;
  private LifecycleState state = LifecycleState.NEW;

  /** Creates a manager with an immutable deterministic component order. */
  public LifecycleManager(List<LifecycleComponent> components, ProjectLogger logger) {
    this.components = List.copyOf(components);
    this.logger = Objects.requireNonNull(logger, "logger");
  }

  /**
   * Starts every component once in registration order.
   *
   * @throws LifecycleException when startup fails after rolling back started components
   * @throws IllegalStateException when the manager is not new
   */
  public void startAll() throws LifecycleException {
    requireState(LifecycleState.NEW, "start");
    state = LifecycleState.STARTING;
    try {
      for (LifecycleComponent component : components) {
        logger.debug("Starting component: " + component.name());
        component.start();
        started.add(component);
      }
      state = LifecycleState.RUNNING;
    } catch (Exception exception) {
      logger.error("A component failed to start; rolling back", exception);
      rollbackStarted();
      state = LifecycleState.FAILED;
      throw new LifecycleException("Unable to start the plugin lifecycle", exception);
    }
  }

  /**
   * Stops every started component once in reverse order.
   *
   * @throws LifecycleException when one or more components fail to stop
   * @throws IllegalStateException when the manager is not running
   */
  public void stopAll() throws LifecycleException {
    requireState(LifecycleState.RUNNING, "stop");
    state = LifecycleState.STOPPING;
    Exception firstFailure = stopStartedComponents();
    state = firstFailure == null ? LifecycleState.STOPPED : LifecycleState.FAILED;
    if (firstFailure != null) {
      throw new LifecycleException("One or more components failed to stop", firstFailure);
    }
  }

  /** Returns the current transition state. */
  public LifecycleState state() {
    return state;
  }

  private void rollbackStarted() {
    Exception failure = stopStartedComponents();
    if (failure != null) {
      logger.error("Lifecycle rollback was incomplete", failure);
    }
  }

  private Exception stopStartedComponents() {
    List<LifecycleComponent> reverse = new ArrayList<>(started);
    Collections.reverse(reverse);
    Exception firstFailure = null;
    for (LifecycleComponent component : reverse) {
      try {
        logger.debug("Stopping component: " + component.name());
        component.stop();
      } catch (Exception exception) {
        logger.error("Component failed to stop: " + component.name(), exception);
        if (firstFailure == null) {
          firstFailure = exception;
        } else {
          firstFailure.addSuppressed(exception);
        }
      }
    }
    started.clear();
    return firstFailure;
  }

  private void requireState(LifecycleState expected, String operation) {
    if (state != expected) {
      throw new IllegalStateException(
          "Cannot " + operation + " lifecycle in state " + state + "; expected " + expected);
    }
  }
}
