package fr.heneria.bedwars.core.map;

import fr.heneria.bedwars.core.config.ProblemSeverity;
import java.util.List;

public record MapValidationResult(List<MapValidationProblem> problems) {
  public MapValidationResult {
    problems = List.copyOf(problems);
  }

  public long errors() {
    return problems.stream()
        .filter(
            problem ->
                problem.severity() == ProblemSeverity.ERROR
                    || problem.severity() == ProblemSeverity.CRITICAL)
        .count();
  }

  public long warnings() {
    return problems.stream()
        .filter(problem -> problem.severity() == ProblemSeverity.WARNING)
        .count();
  }

  public boolean valid() {
    return errors() == 0;
  }
}
