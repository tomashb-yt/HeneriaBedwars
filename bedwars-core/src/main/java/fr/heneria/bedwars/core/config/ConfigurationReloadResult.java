package fr.heneria.bedwars.core.config;

import java.util.List;

/** Outcome of one transactional reload attempt. */
public record ConfigurationReloadResult(
    boolean successful,
    int loadedFiles,
    int warnings,
    int errors,
    List<ConfigurationProblem> problems) {
  public ConfigurationReloadResult {
    problems = List.copyOf(problems);
  }
}
