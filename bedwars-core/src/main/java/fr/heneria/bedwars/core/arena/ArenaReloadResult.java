package fr.heneria.bedwars.core.arena;

import java.util.List;

public record ArenaReloadResult(int loaded, int preserved, List<String> failures) {
  public ArenaReloadResult {
    failures = List.copyOf(failures);
  }

  public boolean completelySuccessful() {
    return failures.isEmpty();
  }
}
