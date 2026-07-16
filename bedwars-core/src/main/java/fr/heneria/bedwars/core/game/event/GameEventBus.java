package fr.heneria.bedwars.core.game.event;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Synchronous internal event bus with explicit subscription cleanup. */
public final class GameEventBus {
  private final CopyOnWriteArrayList<Consumer<GameEvent>> subscribers =
      new CopyOnWriteArrayList<>();

  public AutoCloseable subscribe(Consumer<GameEvent> subscriber) {
    Consumer<GameEvent> checked = Objects.requireNonNull(subscriber, "subscriber");
    subscribers.add(checked);
    return () -> subscribers.remove(checked);
  }

  public void publish(GameEvent event) {
    for (Consumer<GameEvent> subscriber : subscribers) subscriber.accept(event);
  }

  public int subscriberCount() {
    return subscribers.size();
  }

  public void clear() {
    subscribers.clear();
  }
}
