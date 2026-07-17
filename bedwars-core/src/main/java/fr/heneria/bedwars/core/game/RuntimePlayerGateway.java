package fr.heneria.bedwars.core.game;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

/** Player movement port ready for local Bukkit or a future proxy implementation. */
public interface RuntimePlayerGateway {
  CompletionStage<Boolean> enter(
      UUID playerId,
      RuntimeWorldHandle world,
      RuntimeLocation destination,
      WaitingPlayerContext context);

  CompletionStage<Boolean> leave(UUID playerId);

  /** Drops an in-memory snapshot for an offline player without attempting a teleport. */
  default void disconnect(UUID playerId) {}

  /** Removes waiting-only inventory and displays while preserving the pre-game snapshot. */
  default void beginPlaying(UUID playerId) {}
}
