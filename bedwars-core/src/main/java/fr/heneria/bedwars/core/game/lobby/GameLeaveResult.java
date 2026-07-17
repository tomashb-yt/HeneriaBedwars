package fr.heneria.bedwars.core.game.lobby;

import fr.heneria.bedwars.core.game.GameInstance;
import java.util.Optional;

/** Explicit result of membership removal and snapshot restoration. */
public record GameLeaveResult(GameLeaveCode code, Optional<GameInstance> instance, String detail) {
  public GameLeaveResult {
    instance = instance == null ? Optional.empty() : instance;
    detail = detail == null ? "" : detail.trim();
  }

  public boolean successful() {
    return code == GameLeaveCode.SUCCESS;
  }

  static GameLeaveResult success(GameInstance instance) {
    return new GameLeaveResult(GameLeaveCode.SUCCESS, Optional.of(instance), "");
  }

  static GameLeaveResult failure(GameLeaveCode code, GameInstance instance, String detail) {
    return new GameLeaveResult(code, Optional.ofNullable(instance), detail);
  }
}
