package fr.heneria.zombie.plugin.gui;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable data passed to one GUI render. */
public record GuiContext(Map<String, Object> values) {

  /** Empty context. */
  public static final GuiContext EMPTY = new GuiContext(Map.of());

  /** Defensively copies values. */
  public GuiContext {
    values = Map.copyOf(values);
  }

  /**
   * Creates a single-value context.
   *
   * @param key stable key
   * @param value value
   * @return context
   */
  public static GuiContext of(String key, Object value) {
    return new GuiContext(Map.of(Objects.requireNonNull(key, "key"), value));
  }

  /**
   * Reads a typed value.
   *
   * @param key stable key
   * @param type expected type
   * @param <T> value type
   * @return matching value
   */
  public <T> Optional<T> value(String key, Class<T> type) {
    Object value = values.get(key);
    return type.isInstance(value) ? Optional.of(type.cast(value)) : Optional.empty();
  }
}
