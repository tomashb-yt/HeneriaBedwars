package fr.heneria.bedwars.core.arena;

import fr.heneria.bedwars.core.config.ProblemSeverity;
import java.util.List;

public record ArenaValidationResult(List<ArenaProblem> problems) {
  public ArenaValidationResult {
    problems = List.copyOf(problems);
  }

  public boolean valid() {
    return problems.stream()
        .noneMatch(
            problem ->
                problem.severity() == ProblemSeverity.ERROR
                    || problem.severity() == ProblemSeverity.CRITICAL);
  }

  public long warnings() {
    return problems.stream().filter(p -> p.severity() == ProblemSeverity.WARNING).count();
  }
}
