package fr.heneria.bedwars.core.map;

import java.util.Objects;
import java.util.Optional;

/** Explicit result that never exposes a platform exception to commands or menus. */
public record MapOperationResult(
    MapOperationCode code, Optional<MapTemplate> template, String detail) {
  public MapOperationResult {
    code = Objects.requireNonNull(code, "code");
    template = Objects.requireNonNull(template, "template");
    detail = detail == null ? "" : detail;
  }

  public boolean successful() {
    return code == MapOperationCode.SUCCESS;
  }

  public static MapOperationResult success(MapTemplate template) {
    return new MapOperationResult(MapOperationCode.SUCCESS, Optional.of(template), "");
  }

  public static MapOperationResult failure(MapOperationCode code, String detail) {
    return new MapOperationResult(code, Optional.empty(), detail);
  }

  public static MapOperationResult failure(
      MapOperationCode code, MapTemplate template, String detail) {
    return new MapOperationResult(code, Optional.of(template), detail);
  }
}
