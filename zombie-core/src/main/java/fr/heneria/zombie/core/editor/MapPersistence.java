package fr.heneria.zombie.core.editor;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/** Asynchronous persistence port for atomically stored map definitions. */
public interface MapPersistence {
  CompletableFuture<Collection<MapDefinition>> loadAll();

  CompletableFuture<Void> save(MapDefinition definition);

  /** Permanently removes a definition and every persistent artifact owned by it. */
  CompletableFuture<Void> delete(MapDefinition definition);
}
