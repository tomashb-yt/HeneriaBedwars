package fr.heneria.zombie.core.editor;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/** Asynchronous persistence boundary for published map histories. */
public interface MapPublicationPersistence {

  CompletableFuture<Collection<MapPublication>> loadAll();

  CompletableFuture<Void> save(MapPublication publication);
}
