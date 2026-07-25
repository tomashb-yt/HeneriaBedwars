package fr.heneria.zombie.core.instance;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Port implemented by the Paper world adapter. */
public interface WorldInstanceGateway {

  /**
   * Copies and loads an isolated world.
   *
   * @param instanceId instance identifier
   * @param mapId validated template identifier
   * @return future runtime handle
   */
  CompletableFuture<WorldInstanceHandle> prepare(UUID instanceId, String mapId);

  /**
   * Unloads and conditionally deletes an isolated world.
   *
   * @param handle runtime handle
   * @param preserveOnFailure whether uncertain files must be preserved
   * @return future completed with {@code true} only after safe cleanup
   */
  CompletableFuture<Boolean> destroy(WorldInstanceHandle handle, boolean preserveOnFailure);
}
