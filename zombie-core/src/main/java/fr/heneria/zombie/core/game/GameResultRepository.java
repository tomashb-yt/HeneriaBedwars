package fr.heneria.zombie.core.game;

import java.util.concurrent.CompletableFuture;

/** Asynchronous persistence port for completed results. */
@FunctionalInterface
public interface GameResultRepository {
  CompletableFuture<Void> save(GameResult result);
}
