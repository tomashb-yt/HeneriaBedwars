package fr.heneria.bedwars.core.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable flattened representation of one YAML file. */
public record ConfigurationDocument(
    String id, String fileName, int version, Map<String, Object> values) {
  public ConfigurationDocument {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(fileName, "fileName");
    values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
  }

  public Object value(String path) {
    return values.get(path);
  }
}
