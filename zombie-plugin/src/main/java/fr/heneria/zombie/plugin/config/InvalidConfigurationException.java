package fr.heneria.zombie.plugin.config;

import fr.heneria.zombie.core.config.ConfigurationIssue;
import java.util.List;

/** Signals that a configuration candidate contains critical validation errors. */
public final class InvalidConfigurationException extends Exception {

  private static final long serialVersionUID = 1L;
  private final transient List<ConfigurationIssue> issues;

  /**
   * Creates the exception.
   *
   * @param issues critical diagnostics
   */
  public InvalidConfigurationException(List<ConfigurationIssue> issues) {
    super("Configuration rejected with " + issues.size() + " error(s)");
    this.issues = List.copyOf(issues);
  }

  /**
   * Returns validation diagnostics.
   *
   * @return immutable diagnostics
   */
  public List<ConfigurationIssue> issues() {
    return issues;
  }
}
