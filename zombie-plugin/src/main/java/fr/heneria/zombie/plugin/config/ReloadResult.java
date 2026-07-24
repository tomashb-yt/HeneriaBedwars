package fr.heneria.zombie.plugin.config;

import fr.heneria.zombie.core.config.ConfigurationIssue;
import java.util.List;

/**
 * Result of a safe configuration reload.
 *
 * @param successful whether the new snapshot became active
 * @param issues validation diagnostics
 */
public record ReloadResult(boolean successful, List<ConfigurationIssue> issues) {

  /** Defensively copies diagnostics. */
  public ReloadResult {
    issues = List.copyOf(issues);
  }
}
