package fr.heneria.zombie.core.enemy;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Failure-isolating synchronous bus for internal enemy lifecycle extensions. */
public final class ZombieEventDispatcher {
  private final CopyOnWriteArrayList<Consumer<ZombieEvent>> listeners =
      new CopyOnWriteArrayList<>();
  private final Consumer<RuntimeException> failureHandler;

  public ZombieEventDispatcher(Consumer<RuntimeException> failureHandler) {
    this.failureHandler = java.util.Objects.requireNonNull(failureHandler, "failureHandler");
  }

  public AutoCloseable subscribe(Consumer<ZombieEvent> listener) {
    listeners.add(java.util.Objects.requireNonNull(listener, "listener"));
    return () -> listeners.remove(listener);
  }

  public boolean publish(ZombieEvent event) {
    for (Consumer<ZombieEvent> listener : listeners) {
      try {
        listener.accept(event);
      } catch (RuntimeException failure) {
        failureHandler.accept(failure);
      }
    }
    return !event.cancelled();
  }

  public int listenerCount() {
    return listeners.size();
  }
}
