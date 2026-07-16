package fr.heneria.bedwars.core.game;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

/** Player movement port ready for local Bukkit or a future proxy implementation. */
public interface RuntimePlayerGateway {
  CompletionStage<Boolean> enter(
      UUID playerId, RuntimeWorldHandle world, RuntimeLocation destination);

  CompletionStage<Boolean> leave(UUID playerId);
}
