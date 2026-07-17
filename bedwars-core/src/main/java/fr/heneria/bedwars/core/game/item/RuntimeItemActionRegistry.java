package fr.heneria.bedwars.core.game.item;

import fr.heneria.bedwars.api.game.GameState;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Small validator/anti-double-click registry shared by all current and future runtime items. */
public final class RuntimeItemActionRegistry {
  private final Map<String, Long> cooldowns = new HashMap<>();

  public synchronized RuntimeItemDecision evaluate(
      UUID playerId,
      String runtimeKey,
      String itemGameId,
      String currentGameId,
      GameState state,
      boolean mainHand,
      long nowMillis,
      long cooldownMillis) {
    if (!mainHand) return RuntimeItemDecision.OFF_HAND;
    RuntimeItemAction action = RuntimeItemAction.find(runtimeKey).orElse(null);
    if (action == null) return RuntimeItemDecision.UNKNOWN_KEY;
    if (itemGameId == null || !itemGameId.equals(currentGameId))
      return RuntimeItemDecision.STALE_GAME;
    if (state != GameState.WAITING && state != GameState.STARTING)
      return RuntimeItemDecision.INVALID_STATE;
    String cooldownKey = playerId + ":" + action.key();
    long previous = cooldowns.getOrDefault(cooldownKey, Long.MIN_VALUE / 2);
    if (nowMillis - previous < cooldownMillis) return RuntimeItemDecision.COOLDOWN;
    cooldowns.put(cooldownKey, nowMillis);
    return RuntimeItemDecision.EXECUTE;
  }

  public synchronized void forget(UUID playerId) {
    String prefix = playerId + ":";
    cooldowns.keySet().removeIf(key -> key.startsWith(prefix));
  }
}
