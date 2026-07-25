package fr.heneria.zombie.core.editor;

import java.util.List;

/** Immutable map validation result separated by operational severity. */
public record ValidationReport(List<String> errors, List<String> warnings, List<String> advice) {
  public ValidationReport {
    errors = List.copyOf(errors);
    warnings = List.copyOf(warnings);
    advice = List.copyOf(advice);
  }

  public boolean valid() {
    return errors.isEmpty();
  }
}
