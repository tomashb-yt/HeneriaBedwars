package fr.heneria.zombie.plugin.config;

import fr.heneria.zombie.core.config.ZombieSettings;
import java.util.Map;
import java.util.Objects;

/**
 * Atomically replaceable configuration and message snapshot.
 *
 * @param settings validated settings
 * @param messages immutable MiniMessage templates
 */
public record ConfigurationSnapshot(ZombieSettings settings, Map<String, String> messages) {

  /** Defensively copies the messages. */
  public ConfigurationSnapshot {
    Objects.requireNonNull(settings, "settings");
    messages = Map.copyOf(Objects.requireNonNull(messages, "messages"));
  }
}
