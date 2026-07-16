package fr.heneria.bedwars.core.game;

import java.util.concurrent.CompletionStage;

/** Heavy world lifecycle port. Implementations must keep filesystem work off the server thread. */
public interface RuntimeWorldService {
  CompletionStage<RuntimeWorldHandle> create(RuntimeArena arena);

  CompletionStage<Void> destroy(RuntimeWorldHandle world);
}
