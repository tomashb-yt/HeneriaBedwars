package fr.heneria.bedwars.core.arena;

import fr.heneria.bedwars.core.config.ProblemSeverity;
import java.util.Objects;

/** Stable validation diagnostic suitable for commands, logs and tests. */
public record ArenaProblem(ProblemSeverity severity, String code, String field, String message) {
  public ArenaProblem {
    Objects.requireNonNull(severity, "severity");
    code = Objects.requireNonNull(code, "code");
    field = Objects.requireNonNull(field, "field");
    message = Objects.requireNonNull(message, "message");
  }
}
