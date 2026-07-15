package fr.heneria.bedwars.core.config;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Central registry for built-in configuration documents. */
public final class ConfigurationRegistry {
  private final Map<ConfigurationId, ConfigurationDocument> active =
      new EnumMap<>(ConfigurationId.class);
  private final EnumSet<ConfigurationId> registered = EnumSet.noneOf(ConfigurationId.class);
  private String lastError = "none";

  /** Registers one stable file identifier and rejects duplicates. */
  public synchronized void register(ConfigurationId id) {
    Objects.requireNonNull(id, "id");
    if (!registered.add(id)) {
      throw new IllegalArgumentException("Configuration is already registered: " + id);
    }
  }

  public synchronized void activate(Map<ConfigurationId, ConfigurationDocument> candidate) {
    Objects.requireNonNull(candidate, "candidate");
    for (ConfigurationId id : registered) {
      if (!candidate.containsKey(id)) {
        throw new IllegalArgumentException("Missing mandatory configuration: " + id);
      }
    }
    if (registered.isEmpty() || candidate.size() != registered.size()) {
      throw new IllegalArgumentException(
          "Candidate does not match the registered configuration set");
    }
    active.clear();
    active.putAll(candidate);
    lastError = "none";
  }

  public synchronized Optional<ConfigurationDocument> find(ConfigurationId id) {
    return Optional.ofNullable(active.get(Objects.requireNonNull(id, "id")));
  }

  public synchronized ConfigurationDocument require(ConfigurationId id) {
    return find(id)
        .orElseThrow(() -> new IllegalStateException("Configuration is not loaded: " + id));
  }

  public synchronized Collection<ConfigurationDocument> loaded() {
    return List.copyOf(active.values());
  }

  public synchronized int version(ConfigurationId id) {
    return require(id).version();
  }

  public synchronized String lastError() {
    return lastError;
  }

  public synchronized void recordError(String error) {
    lastError = Objects.requireNonNullElse(error, "unknown");
  }
}
