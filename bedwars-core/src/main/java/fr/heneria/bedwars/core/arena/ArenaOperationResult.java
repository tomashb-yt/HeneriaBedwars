package fr.heneria.bedwars.core.arena;

import java.util.List;
import java.util.Optional;

public record ArenaOperationResult(
    ArenaOperationCode code,
    Optional<ArenaDefinition> arena,
    List<ArenaProblem> problems,
    String detail) {
  public ArenaOperationResult {
    problems = List.copyOf(problems);
  }

  public boolean successful() {
    return code == ArenaOperationCode.SUCCESS;
  }

  public static ArenaOperationResult success(
      ArenaDefinition arena, ArenaValidationResult validation) {
    return new ArenaOperationResult(
        ArenaOperationCode.SUCCESS,
        Optional.ofNullable(arena),
        validation == null ? List.of() : validation.problems(),
        "");
  }

  public static ArenaOperationResult failure(ArenaOperationCode code, String detail) {
    return new ArenaOperationResult(code, Optional.empty(), List.of(), detail);
  }
}
