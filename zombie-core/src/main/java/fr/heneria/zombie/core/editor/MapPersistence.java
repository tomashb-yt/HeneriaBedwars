package fr.heneria.zombie.core.editor;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/** Asynchronous persistence port for atomically stored map definitions. */
public interface MapPersistence {
  CompletableFuture<Collection<MapDefinition>> loadAll();

  CompletableFuture<Void> save(MapDefinition definition);
}
