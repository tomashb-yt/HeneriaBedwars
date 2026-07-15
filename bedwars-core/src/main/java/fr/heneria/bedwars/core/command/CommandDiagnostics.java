package fr.heneria.bedwars.core.command;

import java.util.Objects;

/** Immutable values displayed by the administrative version command. */
public record CommandDiagnostics(
    String pluginName,
    String pluginVersion,
    String javaVersion,
    String serverVersion,
    String pluginStatus,
    int serviceCount) {

  public CommandDiagnostics {
    Objects.requireNonNull(pluginName, "pluginName");
    Objects.requireNonNull(pluginVersion, "pluginVersion");
    Objects.requireNonNull(javaVersion, "javaVersion");
    Objects.requireNonNull(serverVersion, "serverVersion");
    Objects.requireNonNull(pluginStatus, "pluginStatus");
    if (serviceCount < 0) {
      throw new IllegalArgumentException("serviceCount must not be negative");
    }
  }
}
