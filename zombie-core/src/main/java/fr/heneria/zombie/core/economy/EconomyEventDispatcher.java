package fr.heneria.zombie.core.economy;

/** Failure-isolated synchronous economy event boundary. */
@FunctionalInterface
public interface EconomyEventDispatcher {
  void publish(EconomyEvent event);

  static EconomyEventDispatcher noop() {
    return ignored -> {};
  }
}
