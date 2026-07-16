package fr.heneria.bedwars.core.arena.editor;

import fr.heneria.bedwars.core.config.ProblemSeverity;

/** Pure severity-to-item mapping consumed by the visual validation menu. */
public final class ArenaValidationVisual {
  private ArenaValidationVisual() {}

  public static String itemKey(ProblemSeverity severity) {
    return switch (severity) {
      case INFO -> "arena.validation.info";
      case WARNING -> "arena.validation.warning";
      case ERROR -> "arena.validation.error";
      case CRITICAL -> "arena.validation.critical";
    };
  }
}
