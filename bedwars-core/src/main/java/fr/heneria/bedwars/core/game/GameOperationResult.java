package fr.heneria.bedwars.core.game;

import java.util.Optional;

public record GameOperationResult(
    GameOperationCode code, Optional<GameInstance> instance, String detail) {
  public GameOperationResult {
    instance = instance == null ? Optional.empty() : instance;
    detail = detail == null ? "" : detail.trim();
  }

  public boolean successful() {
    return code == GameOperationCode.SUCCESS;
  }

  public static GameOperationResult success(GameInstance instance) {
    return new GameOperationResult(GameOperationCode.SUCCESS, Optional.of(instance), "");
  }

  public static GameOperationResult failure(GameOperationCode code, String detail) {
    return new GameOperationResult(code, Optional.empty(), detail);
  }

  public static GameOperationResult failure(
      GameOperationCode code, GameInstance instance, String detail) {
    return new GameOperationResult(code, Optional.ofNullable(instance), detail);
  }
}
