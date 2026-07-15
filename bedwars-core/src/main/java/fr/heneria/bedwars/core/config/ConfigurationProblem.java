package fr.heneria.bedwars.core.config;

import java.util.Locale;
import java.util.Objects;

/** A safe, structured configuration diagnostic. */
public record ConfigurationProblem(
    ProblemSeverity severity,
    String file,
    String key,
    String receivedValue,
    String expected,
    String fallback,
    String message) {
  private static final String REDACTED = "<redacted>";

  public ConfigurationProblem {
    Objects.requireNonNull(severity, "severity");
    file = Objects.requireNonNullElse(file, "unknown");
    key = Objects.requireNonNullElse(key, "unknown");
    expected = Objects.requireNonNullElse(expected, "not specified");
    fallback = Objects.requireNonNullElse(fallback, "none");
    message = Objects.requireNonNullElse(message, "Invalid configuration value");
    receivedValue = sensitive(key) ? REDACTED : Objects.requireNonNullElse(receivedValue, "null");
  }

  /** Convenience constructor for diagnostics built from typed configuration values. */
  public ConfigurationProblem(
      ProblemSeverity severity,
      String file,
      String key,
      Object receivedValue,
      Object expected,
      Object fallback,
      String message) {
    this(
        severity,
        file,
        key,
        String.valueOf(receivedValue),
        String.valueOf(expected),
        String.valueOf(fallback),
        message);
  }

  public String toLogMessage() {
    return "[Configuration] "
        + file
        + ": "
        + key
        + " - "
        + message
        + ". Received: "
        + receivedValue
        + ". Expected: "
        + expected
        + ". Fallback: "
        + fallback;
  }

  private static boolean sensitive(String key) {
    String normalized = key.toLowerCase(Locale.ROOT);
    return normalized.contains("password")
        || normalized.contains("token")
        || normalized.contains("secret")
        || normalized.contains("credential");
  }
}
