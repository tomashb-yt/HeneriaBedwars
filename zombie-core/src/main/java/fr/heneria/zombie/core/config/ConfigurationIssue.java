package fr.heneria.zombie.core.config;

import java.util.Objects;

/**
 * One structured configuration diagnostic.
 *
 * @param severity diagnostic severity
 * @param path stable configuration path
 * @param message human-readable explanation
 */
public record ConfigurationIssue(ValidationSeverity severity, String path, String message) {

  /** Validates the diagnostic fields. */
  public ConfigurationIssue {
    Objects.requireNonNull(severity, "severity");
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(message, "message");
  }
}
