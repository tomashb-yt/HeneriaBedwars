package fr.heneria.bedwars.core.game.lobby;

import fr.heneria.bedwars.core.game.GameInstance;
import java.util.Optional;

/** Explicit result of a complete lobby join, including real platform preparation and teleport. */
public record GameJoinResult(GameJoinCode code, Optional<GameInstance> instance, String detail) {
  public GameJoinResult {
    instance = instance == null ? Optional.empty() : instance;
    detail = detail == null ? "" : detail.trim();
  }

  public boolean successful() {
    return code == GameJoinCode.SUCCESS;
  }

  static GameJoinResult success(GameInstance instance) {
    return new GameJoinResult(GameJoinCode.SUCCESS, Optional.of(instance), "");
  }

  static GameJoinResult failure(GameJoinCode code, GameInstance instance, String detail) {
    return new GameJoinResult(code, Optional.ofNullable(instance), detail);
  }
}
