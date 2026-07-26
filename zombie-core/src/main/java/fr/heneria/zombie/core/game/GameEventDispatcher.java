package fr.heneria.zombie.core.game;

/** Synchronous, non-cancellable internal event sink. */
@FunctionalInterface
public interface GameEventDispatcher {
  void publish(GameEvent event);
}
