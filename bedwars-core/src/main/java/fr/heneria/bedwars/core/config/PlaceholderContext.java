package fr.heneria.bedwars.core.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable internal placeholder values. */
public record PlaceholderContext(Map<String, String> values) {
  public static final PlaceholderContext EMPTY = new PlaceholderContext(Map.of());

  public PlaceholderContext {
    values = Map.copyOf(new LinkedHashMap<>(values));
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private final Map<String, String> values = new LinkedHashMap<>();

    public Builder put(String key, Object value) {
      Objects.requireNonNull(key, "key");
      values.put(key, value == null ? "" : String.valueOf(value));
      return this;
    }

    public PlaceholderContext build() {
      return new PlaceholderContext(values);
    }
  }
}
