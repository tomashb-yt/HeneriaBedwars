package fr.heneria.zombie.core.weapon;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Failure-isolating internal bus for weapon extensions and future perks. */
public final class WeaponEventDispatcher {
  private final CopyOnWriteArrayList<Consumer<WeaponEvent>> listeners =
      new CopyOnWriteArrayList<>();
  private final Consumer<RuntimeException> failureHandler;

  public WeaponEventDispatcher(Consumer<RuntimeException> failureHandler) {
    this.failureHandler = java.util.Objects.requireNonNull(failureHandler, "failureHandler");
  }

  public AutoCloseable subscribe(Consumer<WeaponEvent> listener) {
    listeners.add(java.util.Objects.requireNonNull(listener, "listener"));
    return () -> listeners.remove(listener);
  }

  public boolean publish(WeaponEvent event) {
    for (Consumer<WeaponEvent> listener : listeners) {
      try {
        listener.accept(event);
      } catch (RuntimeException failure) {
        failureHandler.accept(failure);
      }
    }
    return !event.cancelled();
  }
}
